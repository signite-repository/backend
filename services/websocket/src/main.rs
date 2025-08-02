use anyhow::{Context, Result};
use chrono::Utc;
use dashmap::DashMap;
use futures_util::{stream::StreamExt, sink::SinkExt};
use serde_json::from_str;
use std::{
    net::SocketAddr,
    sync::Arc,
};
use tokio::net::TcpListener;
use tokio::sync::mpsc::{self, UnboundedSender};
use tokio_tungstenite::tungstenite::protocol::Message;
use tracing::{error, info, warn, debug};
use uuid::Uuid;

use rust_websocket_server::{
    Config, DatabaseManager, ChatMessage, RoomInfo, 
    AppState, create_router, ClientMessage, PlayerState
};
use validator::{Validate, ValidationError, ValidationErrors};
use serde::{Serialize, Deserialize};

type ClientId = String;
type RoomId = String;
type Tx = UnboundedSender<Message>;
type PeerMap = Arc<DashMap<ClientId, (RoomId, Tx)>>;
type RoomMap = Arc<DashMap<RoomId, DashMap<ClientId, PlayerState>>>;

// PlayerState와 ClientMessage는 lib.rs에서 import하므로 중복 정의 제거

#[derive(Serialize, Deserialize, Debug)]
#[serde(tag = "type")]
enum ServerMessage {
    Welcome {
        client_id: ClientId,
        room_state: Vec<(ClientId, PlayerState)>,
        chat_history: Vec<ChatMessage>,
    },
    PlayerJoined {
        client_id: ClientId,
        state: PlayerState,
    },
    PlayerLeft {
        client_id: ClientId,
    },
    PlayerUpdate {
        client_id: ClientId,
        state: PlayerState,
    },
    ChatMessage {
        client_id: ClientId,
        name: String,
        message: String,
        timestamp: String,
    },
    Error {
        message: String,
    },
}

#[derive(Clone)]
struct WebSocketState {
    peer_map: PeerMap,
    room_map: RoomMap,
    db: DatabaseManager,
}

async fn handle_client_message(
    msg: ClientMessage,
    client_id: &ClientId,
    peer_map: &PeerMap,
    room_map: &RoomMap,
    db: &DatabaseManager,
    tx: &Tx,
) -> Result<()> {
    match msg {
        ClientMessage::Join { room_id, name, color } => {
            let initial_state = PlayerState {
                name: name.clone(),
                color,
                position: (0.0, 0.0, 0.0),
                rotation: (1.0, 0.0, 0.0, 0.0),
                animation: None,
                velocity: None,
                model_url: None,
            };

            // 룸 상태 업데이트
            let room_state: Vec<(ClientId, PlayerState)> = {
                room_map.entry(room_id.clone()).or_insert_with(DashMap::new)
                    .insert(client_id.clone(), initial_state.clone());
                
                room_map.get(&room_id)
                    .map(|room| room.iter().map(|entry| (entry.key().clone(), entry.value().clone())).collect())
                    .unwrap_or_default()
            };
            
            peer_map.insert(client_id.clone(), (room_id.clone(), tx.clone()));

            // 채팅 기록 조회
            let chat_history = db.get_chat_history(&room_id, 20).await
                .unwrap_or_else(|e| {
                    warn!("채팅 기록 조회 실패: {}", e);
                    vec![]
                });

            // 룸 정보 업데이트
            let room_info = RoomInfo {
                id: room_id.clone(),
                name: room_id.clone(), // 기본적으로 ID를 이름으로 사용
                created_at: Utc::now(),
                last_activity: Utc::now(),
                player_count: room_state.len() as u32,
            };
            if let Err(e) = db.upsert_room_info(&room_info).await {
                warn!("룸 정보 저장 실패: {}", e);
            }

            // Redis에 플레이어 수 업데이트
            if let Err(e) = db.update_room_player_count(&room_id, room_state.len() as u32).await {
                warn!("플레이어 수 업데이트 실패: {}", e);
            }

            let welcome_msg = ServerMessage::Welcome {
                client_id: client_id.clone(),
                room_state,
                chat_history,
            };
            send_message_to_client(tx, &welcome_msg).await;

            let joined_msg = ServerMessage::PlayerJoined {
                client_id: client_id.clone(),
                state: initial_state,
            };
            broadcast_message(peer_map, room_map, client_id, &room_id, &joined_msg).await;
            
            info!("[{}] 룸 [{}]에 접속", client_id, room_id);
        }
        ClientMessage::Update { state } => {
            let room_id = peer_map.get(client_id)
                .map(|entry| entry.0.clone())
                .context("클라이언트가 룸에 속해있지 않음")?;

            if let Some(room) = room_map.get(&room_id) {
                room.insert(client_id.clone(), state.clone());
            }

            // Redis에 플레이어 상태 캐시
            if let Ok(state_json) = serde_json::to_string(&state) {
                if let Err(e) = db.cache_player_state(&room_id, client_id, &state_json).await {
                    warn!("플레이어 상태 캐시 실패: {}", e);
                }
            }

            let update_msg = ServerMessage::PlayerUpdate {
                client_id: client_id.clone(),
                state,
            };
            broadcast_message(peer_map, room_map, client_id, &room_id, &update_msg).await;
            
            debug!("[{}] 상태 업데이트", client_id);
        }
        ClientMessage::Chat { message } => {
            let (room_id, name) = {
                let peer_entry = peer_map.get(client_id)
                    .context("클라이언트가 룸에 속해있지 않음")?;
                let room_id = peer_entry.0.clone();
                
                let name = room_map.get(&room_id)
                    .and_then(|room| room.get(client_id).map(|state| state.name.clone()))
                    .unwrap_or_default();
                    
                (room_id, name)
            };

            let timestamp = Utc::now();
            
            // MongoDB에 채팅 메시지 저장
            let chat_message = ChatMessage {
                id: Uuid::new_v4().to_string(),
                room_id: room_id.clone(),
                client_id: client_id.clone(),
                username: name.clone(),
                message: message.clone(),
                timestamp,
            };
            
            if let Err(e) = db.save_chat_message(&chat_message).await {
                warn!("채팅 메시지 저장 실패: {}", e);
            }
                
            let chat_msg = ServerMessage::ChatMessage {
                client_id: client_id.clone(),
                name,
                message,
                timestamp: timestamp.to_rfc3339(),
            };
            broadcast_message(peer_map, room_map, client_id, &room_id, &chat_msg).await;
            
            debug!("[{}] 채팅 메시지 전송", client_id);
        }
    }
    Ok(())
}

async fn cleanup_connection(
    client_id: &ClientId, 
    peer_map: &PeerMap, 
    room_map: &RoomMap,
    db: &DatabaseManager,
) {
    let room_id = match peer_map.remove(client_id) {
        Some((_, (room_id, _))) => room_id,
        None => return,
    };

    if let Some(room) = room_map.get(&room_id) {
        room.remove(client_id);
        let player_count = room.len();
        
        if player_count == 0 {
            room_map.remove(&room_id);
            info!("빈 룸 [{}] 제거", room_id);
        } else {
            // Redis에 플레이어 수 업데이트
            if let Err(e) = db.update_room_player_count(&room_id, player_count as u32).await {
                warn!("플레이어 수 업데이트 실패: {}", e);
            }
        }
    }

    // Redis에서 플레이어 캐시 정리
    if let Err(e) = db.cleanup_player_cache(&room_id, client_id).await {
        warn!("플레이어 캐시 정리 실패: {}", e);
    }

    let left_msg = ServerMessage::PlayerLeft { 
        client_id: client_id.clone() 
    };
    broadcast_message(peer_map, room_map, client_id, &room_id, &left_msg).await;
    
    info!("[{}] 연결 정리 완료", client_id);
}

async fn broadcast_message(
    peer_map: &PeerMap,
    room_map: &RoomMap,
    sender_id: &ClientId,
    room_id: &RoomId,
    message: &ServerMessage,
) {
    let json_message = match serde_json::to_string(message) {
        Ok(json) => json,
        Err(e) => {
            error!("메시지 직렬화 실패: {}", e);
            return;
        }
    };
    
    if let Some(room) = room_map.get(room_id) {
        for client_entry in room.iter() {
            let client_id = client_entry.key();
            if client_id != sender_id {
                if let Some(peer_entry) = peer_map.get(client_id) {
                    let tx = &peer_entry.1;
                    if let Err(e) = tx.send(Message::Text(json_message.clone().into())) {
                        warn!("[{}] 브로드캐스트 전송 실패: {}", client_id, e);
                    }
                }
            }
        }
    }
}

async fn send_message_to_client(tx: &Tx, message: &ServerMessage) {
    if let Ok(json) = serde_json::to_string(message) {
        if let Err(e) = tx.send(Message::Text(json.into())) {
            warn!("클라이언트 메시지 전송 실패: {}", e);
        }
    }
}

mod error_handler;

#[tokio::main]
async fn main() -> Result<()> {
    // 패닉 핸들러 설정
    error_handler::setup_panic_handler();
    
    // 로깅 초기화
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::from_default_env()
                .add_directive("rust_websocket_server=debug".parse()?)
        )
        .init();

    // 설정 로드
    let config = Config::from_env();
    info!("서버 설정 로드 완료");

    // 데이터베이스 연결
    let db = DatabaseManager::new(&config).await
        .context("데이터베이스 연결 실패")?;
    info!("데이터베이스 연결 완료");

    // WebSocket 상태 초기화
    let peer_map = PeerMap::new(DashMap::new());
    let room_map = RoomMap::new(DashMap::new());
    
    let ws_state = Arc::new(WebSocketState {
        peer_map,
        room_map,
        db: db.clone(),
    });

    // HTTP 서버 설정
    let http_state = AppState {
        db: db.clone(),
        config: config.clone(),
    };

    let app = create_router(http_state);

    // HTTP 서버 시작
    let http_listener = TcpListener::bind(config.http_addr()).await
        .context("HTTP 서버 바인딩 실패")?;
    
    info!("HTTP 서버가 {}에서 실행 중 (헬스체크: /health)", config.http_addr());

    tokio::spawn(async move {
        if let Err(e) = axum::serve(http_listener, app).await {
            error!("HTTP 서버 오류: {}", e);
        }
    });

    // WebSocket 서버 시작 (기존 방식 유지)
    let ws_listener = TcpListener::bind(config.websocket_addr()).await
        .context("WebSocket 서버 바인딩 실패")?;
    
    info!("WebSocket 서버가 {}에서 실행 중...", config.websocket_addr());
    info!("클라이언트 접속을 기다리는 중...");

    while let Ok((stream, addr)) = ws_listener.accept().await {
        let ws_state_clone = ws_state.clone();
        
        tokio::spawn(async move {
            if let Err(e) = handle_connection(ws_state_clone, stream, addr).await {
                error!("WebSocket 연결 처리 오류 [{}]: {}", addr, e);
            }
        });
    }
    
    Ok(())
}

async fn handle_connection(
    state: Arc<WebSocketState>,
    stream: tokio::net::TcpStream,
    addr: SocketAddr,
) -> Result<()> {
    info!("새 WebSocket 클라이언트 접속: {}", addr);
    
    let ws_stream = tokio_tungstenite::accept_async(stream)
        .await
        .context("웹소켓 핸드셰이크 실패")?;

    let client_id = Uuid::new_v4().to_string();
    let (tx, mut rx) = mpsc::unbounded_channel();
    let (mut ws_sender, mut ws_receiver) = ws_stream.split();

    let client_id_sender = client_id.clone();
    
    tokio::spawn(async move {
        while let Some(message) = rx.recv().await {
            if let Err(e) = ws_sender.send(message).await {
                warn!("[{}] 클라이언트로 메시지 전송 실패: {}", client_id_sender, e);
                break;
            }
        }
        debug!("[{}] 송신 루프 종료", client_id_sender);
    });

    while let Some(msg) = ws_receiver.next().await {
        let msg = match msg {
            Ok(msg) => msg,
            Err(e) => {
                warn!("[{}] 메시지 수신 오류: {}", client_id, e);
                break;
            }
        };

        if let Message::Text(text) = msg {
            match from_str::<ClientMessage>(&text) {
                Ok(client_msg) => {
                    if let Err(e) = client_msg.validate() {
                        warn!("[{}] 메시지 검증 실패: {:?}", client_id, e);
                        let error_msg = ServerMessage::Error {
                            message: format!("잘못된 메시지 형식: {}", e),
                        };
                        send_message_to_client(&tx, &error_msg).await;
                        continue;
                    }

                    if let Err(e) = handle_client_message(
                        client_msg,
                        &client_id,
                        &state.peer_map,
                        &state.room_map,
                        &state.db,
                        &tx,
                    ).await {
                        error!("[{}] 메시지 처리 오류: {}", client_id, e);
                    }
                }
                Err(e) => {
                    warn!("[{}] 메시지 파싱 오류: {}", client_id, e);
                    let error_msg = ServerMessage::Error {
                        message: "JSON 파싱 오류".to_string(),
                    };
                    send_message_to_client(&tx, &error_msg).await;
                }
            }
        }
    }
    
    cleanup_connection(&client_id, &state.peer_map, &state.room_map, &state.db).await;
    info!("WebSocket 클라이언트 연결 종료: {}", client_id);
    Ok(())
}
