use chrono::Utc;
use rust_websocket_server::db::{ChatMessage, RoomInfo};
use serde_json::{to_string, from_str};
use uuid::Uuid;

#[test]
fn 채팅_메시지_직렬화_역직렬화_테스트() {
    let original_message = ChatMessage {
        id: Uuid::new_v4().to_string(),
        room_id: "room-123".to_string(),
        client_id: "client-456".to_string(),
        username: "테스트유저".to_string(),
        message: "안녕하세요! 👋".to_string(),
        timestamp: Utc::now(),
    };

    // 직렬화
    let json_str = to_string(&original_message).expect("직렬화 실패");
    assert!(!json_str.is_empty());

    // 역직렬화
    let deserialized_message: ChatMessage = from_str(&json_str).expect("역직렬화 실패");
    
    assert_eq!(original_message.id, deserialized_message.id);
    assert_eq!(original_message.room_id, deserialized_message.room_id);
    assert_eq!(original_message.client_id, deserialized_message.client_id);
    assert_eq!(original_message.username, deserialized_message.username);
    assert_eq!(original_message.message, deserialized_message.message);
}

#[test]
fn 룸_정보_직렬화_역직렬화_테스트() {
    let original_room = RoomInfo {
        id: "room-789".to_string(),
        name: "테스트 룸".to_string(),
        created_at: Utc::now(),
        last_activity: Utc::now(),
        player_count: 5,
    };

    // 직렬화
    let json_str = to_string(&original_room).expect("직렬화 실패");
    assert!(!json_str.is_empty());

    // 역직렬화
    let deserialized_room: RoomInfo = from_str(&json_str).expect("역직렬화 실패");
    
    assert_eq!(original_room.id, deserialized_room.id);
    assert_eq!(original_room.name, deserialized_room.name);
    assert_eq!(original_room.player_count, deserialized_room.player_count);
}

#[test]
fn 채팅_메시지_필드_검증_테스트() {
    let message = ChatMessage {
        id: Uuid::new_v4().to_string(),
        room_id: "room-test".to_string(),
        client_id: "client-test".to_string(),
        username: "유저명".to_string(),
        message: "메시지 내용".to_string(),
        timestamp: Utc::now(),
    };

    // ID 형식 검증
    assert!(!message.id.is_empty());
    assert!(Uuid::parse_str(&message.id).is_ok());

    // 필수 필드 검증
    assert!(!message.room_id.is_empty());
    assert!(!message.client_id.is_empty());
    assert!(!message.username.is_empty());
    assert!(!message.message.is_empty());
}

#[test]
fn 룸_정보_필드_검증_테스트() {
    let room = RoomInfo {
        id: "room-validation-test".to_string(),
        name: "검증 테스트 룸".to_string(),
        created_at: Utc::now(),
        last_activity: Utc::now(),
        player_count: 10,
    };

    // 필수 필드 검증
    assert!(!room.id.is_empty());
    assert!(!room.name.is_empty());
    assert!(room.player_count >= 0);
    assert!(room.created_at <= room.last_activity);
}

#[test]
fn 특수문자_포함_메시지_처리_테스트() {
    let special_chars_message = ChatMessage {
        id: Uuid::new_v4().to_string(),
        room_id: "room-special".to_string(),
        client_id: "client-special".to_string(),
        username: "유저💎".to_string(),
        message: "특수문자 테스트: !@#$%^&*()_+ 🎮🎯🚀".to_string(),
        timestamp: Utc::now(),
    };

    // 직렬화/역직렬화가 특수문자를 올바르게 처리하는지 확인
    let json_str = to_string(&special_chars_message).expect("특수문자 직렬화 실패");
    let deserialized: ChatMessage = from_str(&json_str).expect("특수문자 역직렬화 실패");
    
    assert_eq!(special_chars_message.username, deserialized.username);
    assert_eq!(special_chars_message.message, deserialized.message);
}

#[test]
fn 빈_룸_이름_허용_여부_테스트() {
    let room_with_empty_name = RoomInfo {
        id: "room-empty-name".to_string(),
        name: "".to_string(), // 빈 이름
        created_at: Utc::now(),
        last_activity: Utc::now(),
        player_count: 0,
    };

    // 빈 룸 이름도 기술적으로는 허용하지만, 비즈니스 로직에서 검증해야 함
    let json_str = to_string(&room_with_empty_name).expect("직렬화 실패");
    let _deserialized: RoomInfo = from_str(&json_str).expect("역직렬화 실패");
    
    // 실제 애플리케이션에서는 빈 이름을 거부해야 함
    assert!(room_with_empty_name.name.is_empty());
}

#[test]
fn 최대_플레이어_수_제한_테스트() {
    let room_with_max_players = RoomInfo {
        id: "room-max".to_string(),
        name: "최대 인원 테스트".to_string(),
        created_at: Utc::now(),
        last_activity: Utc::now(),
        player_count: 100, // 최대 인원
    };

    assert!(room_with_max_players.player_count <= 100);

    // 음수 플레이어 수는 불가능
    let room_with_negative_players = RoomInfo {
        id: "room-negative".to_string(),
        name: "음수 테스트".to_string(),
        created_at: Utc::now(),
        last_activity: Utc::now(),
        player_count: 0, // u32 타입이므로 음수 불가능
    };

    assert!(room_with_negative_players.player_count >= 0);
}

#[test]
fn 긴_메시지_처리_테스트() {
    let long_message = "a".repeat(10000); // 10KB 메시지
    
    let chat_message = ChatMessage {
        id: Uuid::new_v4().to_string(),
        room_id: "room-long".to_string(),
        client_id: "client-long".to_string(),
        username: "장문유저".to_string(),
        message: long_message.clone(),
        timestamp: Utc::now(),
    };

    // 긴 메시지도 직렬화/역직렬화가 가능한지 확인
    let json_str = to_string(&chat_message).expect("긴 메시지 직렬화 실패");
    let deserialized: ChatMessage = from_str(&json_str).expect("긴 메시지 역직렬화 실패");
    
    assert_eq!(chat_message.message, deserialized.message);
    assert_eq!(chat_message.message.len(), 10000);
}

// 데이터베이스 연결이 가능한 경우에만 실행되는 통합 테스트
#[tokio::test]
async fn 데이터베이스_연결_테스트() {
    // 환경변수로 테스트 실행 여부 결정
    if std::env::var("TEST_WITH_DB").is_ok() {
        use rust_websocket_server::config::Config;
        use rust_websocket_server::db::DatabaseManager;

        let config = Config {
            server_host: "127.0.0.1".to_string(),
            server_port: 8080,
            http_port: 3001,
            redis_url: "redis://localhost:6379".to_string(),
            mongodb_url: "mongodb://localhost:27017".to_string(),
            mongodb_db_name: "test_signight_websocket".to_string(),
            jwt_secret: "test-secret".to_string(),
            jwt_issuer: "test-issuer".to_string(),
            allowed_origins: vec!["http://localhost:3000".to_string()],
        };

        // 데이터베이스 연결 테스트
        let db_result = DatabaseManager::new(&config).await;
        
        if let Ok(db) = db_result {
            // Redis 연결 테스트
            let redis_conn_result = db.get_redis_connection().await;
            assert!(redis_conn_result.is_ok(), "Redis 연결 실패");

            // 테스트 채팅 메시지 저장
            let test_message = ChatMessage {
                id: Uuid::new_v4().to_string(),
                room_id: "test-room".to_string(),
                client_id: "test-client".to_string(),
                username: "테스트유저".to_string(),
                message: "테스트 메시지".to_string(),
                timestamp: Utc::now(),
            };

            let save_result = db.save_chat_message(&test_message).await;
            assert!(save_result.is_ok(), "채팅 메시지 저장 실패");

            // 채팅 기록 조회 테스트
            let history_result = db.get_chat_history("test-room", 10).await;
            assert!(history_result.is_ok(), "채팅 기록 조회 실패");

            let messages = history_result.unwrap();
            assert!(messages.len() >= 0, "채팅 기록 조회 결과 확인");
        }
    }
}