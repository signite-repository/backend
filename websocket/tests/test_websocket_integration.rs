use futures_util::{SinkExt, StreamExt};
use serde_json::{json, from_str, to_string};
use tokio_tungstenite::{connect_async, tungstenite::Message};
use rust_websocket_server::ClientMessage;
use url::Url;

// WebSocket 클라이언트 연결 테스트 (실제 서버 실행 필요)
#[tokio::test]
async fn 웹소켓_클라이언트_연결_테스트() {
    // 환경변수로 실제 서버 테스트 실행 여부 결정
    if std::env::var("TEST_WITH_WEBSOCKET_SERVER").is_ok() {
        let url = "ws://127.0.0.1:8080";
        
        // WebSocket 연결 시도
        let _url = Url::parse(url).expect("잘못된 URL");
        let (ws_stream, _) = connect_async(url).await.expect("WebSocket 연결 실패");
        
        let (mut write, mut read) = ws_stream.split();
        
        // Join 메시지 전송
        let join_message = ClientMessage::Join {
            room_id: "test-room".to_string(),
            name: "테스트플레이어".to_string(),
            color: "#FF0000".to_string(),
        };
        
        let join_json = to_string(&join_message).expect("Join 메시지 직렬화 실패");
        write.send(Message::Text(join_json.into())).await.expect("Join 메시지 전송 실패");
        
        // 서버로부터 응답 받기
        if let Some(msg) = read.next().await {
            let msg = msg.expect("메시지 수신 실패");
            if let Message::Text(text) = msg {
                println!("서버 응답: {}", text);
                // 응답 메시지 형식 검증
                assert!(!text.is_empty());
            }
        }
        
        // Update 메시지 전송
        let update_message = ClientMessage::Update {
            state: rust_websocket_server::PlayerState {
                name: "테스트플레이어".to_string(),
                color: "#FF0000".to_string(),
                position: (10.0, 0.0, 5.0),
                rotation: (1.0, 0.0, 0.0, 0.0),
                animation: Some("walking".to_string()),
                velocity: Some((1.0, 0.0, 0.0)),
                model_url: None,
            },
        };
        
        let update_json = to_string(&update_message).expect("Update 메시지 직렬화 실패");
        write.send(Message::Text(update_json.into())).await.expect("Update 메시지 전송 실패");
        
        // Chat 메시지 전송
        let chat_message = ClientMessage::Chat {
            message: "안녕하세요! 테스트 메시지입니다.".to_string(),
        };
        
        let chat_json = to_string(&chat_message).expect("Chat 메시지 직렬화 실패");
        write.send(Message::Text(chat_json.into())).await.expect("Chat 메시지 전송 실패");
        
        // 연결 종료
        write.close().await.expect("연결 종료 실패");
    }
}

#[test]
fn 메시지_JSON_형식_검증_테스트() {
    // Join 메시지 JSON 형식
    let join_json = json!({
        "type": "Join",
        "room_id": "room-123",
        "name": "플레이어1",
        "color": "#00FF00"
    });
    
    let join_str = to_string(&join_json).expect("JSON 직렬화 실패");
    let parsed_join: Result<ClientMessage, _> = from_str(&join_str);
    assert!(parsed_join.is_ok());
    
    // Update 메시지 JSON 형식
    let update_json = json!({
        "type": "Update",
        "state": {
            "name": "플레이어1",
            "color": "#0000FF",
            "position": [0.0, 1.0, 0.0],
            "rotation": [1.0, 0.0, 0.0, 0.0],
            "animation": "running",
            "velocity": [2.0, 0.0, 1.0],
            "modelUrl": "https://example.com/model.glb"
        }
    });
    
    let update_str = to_string(&update_json).expect("JSON 직렬화 실패");
    let parsed_update: Result<ClientMessage, _> = from_str(&update_str);
    assert!(parsed_update.is_ok());
    
    // Chat 메시지 JSON 형식
    let chat_json = json!({
        "type": "Chat",
        "message": "Hello, WebSocket!"
    });
    
    let chat_str = to_string(&chat_json).expect("JSON 직렬화 실패");
    let parsed_chat: Result<ClientMessage, _> = from_str(&chat_str);
    assert!(parsed_chat.is_ok());
}

#[test]
fn 잘못된_JSON_메시지_처리_테스트() {
    let invalid_jsons = vec![
        r#"{"type": "Join"}"#, // 필수 필드 누락
        r#"{"type": "InvalidType", "data": "test"}"#, // 잘못된 타입
        r#"{"invalid": "json"}"#, // type 필드 누락
        r#"not a json"#, // JSON이 아님
        r#"{"type": "Update", "state": {"name": ""}}"#, // 잘못된 state
    ];
    
    for invalid_json in invalid_jsons {
        let parse_result: Result<ClientMessage, _> = from_str(invalid_json);
        assert!(parse_result.is_err(), "잘못된 JSON이 파싱됨: {}", invalid_json);
    }
}

#[test]
fn 플레이어_상태_업데이트_시나리오_테스트() {
    // 초기 위치
    let initial_state = rust_websocket_server::PlayerState {
        name: "플레이어1".to_string(),
        color: "#FF5733".to_string(),
        position: (0.0, 0.0, 0.0),
        rotation: (1.0, 0.0, 0.0, 0.0),
        animation: Some("idle".to_string()),
        velocity: None,
        model_url: None,
    };
    
    // 이동 상태
    let moving_state = rust_websocket_server::PlayerState {
        name: "플레이어1".to_string(),
        color: "#FF5733".to_string(),
        position: (10.0, 0.0, 5.0),
        rotation: (0.707, 0.0, 0.707, 0.0), // 45도 회전
        animation: Some("walking".to_string()),
        velocity: Some((1.0, 0.0, 0.5)),
        model_url: None,
    };
    
    // 점프 상태
    let jumping_state = rust_websocket_server::PlayerState {
        name: "플레이어1".to_string(),
        color: "#FF5733".to_string(),
        position: (10.0, 5.0, 5.0), // Y축 상승
        rotation: (0.707, 0.0, 0.707, 0.0),
        animation: Some("jumping".to_string()),
        velocity: Some((0.0, 3.0, 0.0)), // 위쪽 속도
        model_url: None,
    };
    
    // 각 상태가 유효한지 검증
    use validator::Validate;
    assert!(initial_state.validate().is_ok());
    assert!(moving_state.validate().is_ok());
    assert!(jumping_state.validate().is_ok());
    
    // 메시지로 변환하여 직렬화 테스트
    let messages = vec![
        ClientMessage::Update { state: initial_state },
        ClientMessage::Update { state: moving_state },
        ClientMessage::Update { state: jumping_state },
    ];
    
    for message in messages {
        let json_str = to_string(&message).expect("메시지 직렬화 실패");
        let parsed_message: ClientMessage = from_str(&json_str).expect("메시지 역직렬화 실패");
        
        // 메시지 타입 확인
        match parsed_message {
            ClientMessage::Update { state } => {
                assert!(!state.name.is_empty());
                assert!(state.validate().is_ok());
            },
            _ => panic!("잘못된 메시지 타입"),
        }
    }
}

#[test]
fn 멀티플레이어_시나리오_테스트() {
    // 여러 플레이어가 같은 룸에 접속하는 시나리오
    let players = vec![
        ("플레이어1", "#FF0000", (0.0, 0.0, 0.0)),
        ("플레이어2", "#00FF00", (10.0, 0.0, 0.0)),
        ("플레이어3", "#0000FF", (-10.0, 0.0, 0.0)),
        ("플레이어4", "#FFFF00", (0.0, 0.0, 10.0)),
    ];
    
    let room_id = "multiplayer-test-room";
    
    for (name, color, position) in players {
        // Join 메시지
        let join_message = ClientMessage::Join {
            room_id: room_id.to_string(),
            name: name.to_string(),
            color: color.to_string(),
        };
        
        let join_json = to_string(&join_message).expect("Join 메시지 직렬화 실패");
        let parsed_join: ClientMessage = from_str(&join_json).expect("Join 메시지 파싱 실패");
        
        match parsed_join {
            ClientMessage::Join { room_id: parsed_room, name: parsed_name, color: parsed_color } => {
                assert_eq!(parsed_room, room_id);
                assert_eq!(parsed_name, name);
                assert_eq!(parsed_color, color);
            },
            _ => panic!("잘못된 Join 메시지"),
        }
        
        // Update 메시지
        let player_state = rust_websocket_server::PlayerState {
            name: name.to_string(),
            color: color.to_string(),
            position,
            rotation: (1.0, 0.0, 0.0, 0.0),
            animation: Some("idle".to_string()),
            velocity: None,
            model_url: None,
        };
        
        let update_message = ClientMessage::Update { state: player_state };
        let update_json = to_string(&update_message).expect("Update 메시지 직렬화 실패");
        let _parsed_update: ClientMessage = from_str(&update_json).expect("Update 메시지 파싱 실패");
    }
}

#[test]
fn 채팅_메시지_다양한_언어_테스트() {
    let messages = vec![
        ("English", "Hello, how are you?"),
        ("한국어", "안녕하세요! 반갑습니다."),
        ("日本語", "こんにちは、元気ですか？"),
        ("中文", "你好，你好吗？"),
        ("Emoji", "🎮 게임 재밌어요! 🚀✨"),
        ("Special", "!@#$%^&*()_+-=[]{}|;':\",./<>?"),
    ];
    
    for (lang, text) in messages {
        let chat_message = ClientMessage::Chat {
            message: text.to_string(),
        };
        
        let json_str = to_string(&chat_message).expect(&format!("{} 메시지 직렬화 실패", lang));
        let parsed_message: ClientMessage = from_str(&json_str).expect(&format!("{} 메시지 파싱 실패", lang));
        
        match parsed_message {
            ClientMessage::Chat { message } => {
                assert_eq!(message, text);
            },
            _ => panic!("잘못된 Chat 메시지: {}", lang),
        }
    }
}

// 실제 서버와의 부하 테스트 (선택적 실행)
#[tokio::test]
async fn 웹소켓_연결_부하_테스트() {
    if std::env::var("TEST_LOAD").is_ok() {
        let concurrent_connections = 10;
        let mut handles = Vec::new();
        
        for i in 0..concurrent_connections {
            let handle = tokio::spawn(async move {
                if let Ok(_url) = Url::parse("ws://127.0.0.1:8080") {
                    if let Ok((ws_stream, _)) = connect_async("ws://127.0.0.1:8080").await {
                        let (mut write, _read) = ws_stream.split();
                        
                        // 각 연결에서 메시지 전송
                        let join_message = ClientMessage::Join {
                            room_id: format!("load-test-room-{}", i),
                            name: format!("LoadTestPlayer{}", i),
                            color: "#FF0000".to_string(),
                        };
                        
                        if let Ok(join_json) = to_string(&join_message) {
                            let _ = write.send(Message::Text(join_json.into())).await;
                        }
                        
                        let _ = write.close().await;
                    }
                }
            });
            handles.push(handle);
        }
        
        // 모든 연결이 완료될 때까지 대기
        for handle in handles {
            let _ = handle.await;
        }
    }
}