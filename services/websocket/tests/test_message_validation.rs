use rust_websocket_server::{PlayerState, ClientMessage};
use serde_json::{json, from_str, to_string};
use validator::Validate;

#[test]
fn 유효한_플레이어_상태_검증_성공() {
    let player_state = PlayerState {
        name: "테스트플레이어".to_string(),
        color: "#FF5733".to_string(),
        position: (0.0, 0.0, 0.0),
        rotation: (1.0, 0.0, 0.0, 0.0),
        animation: Some("idle".to_string()),
        velocity: Some((0.0, 0.0, 0.0)),
        model_url: Some("https://example.com/model.glb".to_string()),
    };

    assert!(player_state.validate().is_ok());
}

#[test]
fn 잘못된_이름_길이_검증_실패() {
    let player_state = PlayerState {
        name: "".to_string(), // 빈 이름
        color: "#FF5733".to_string(),
        position: (0.0, 0.0, 0.0),
        rotation: (1.0, 0.0, 0.0, 0.0),
        animation: None,
        velocity: None,
        model_url: None,
    };

    let validation_result = player_state.validate();
    assert!(validation_result.is_err());
    
    let errors = validation_result.unwrap_err();
    assert!(errors.field_errors().contains_key("name"));
}

#[test]
fn 잘못된_색상_형식_검증_실패() {
    let player_state = PlayerState {
        name: "테스트플레이어".to_string(),
        color: "invalid-color".to_string(), // 잘못된 색상 형식
        position: (0.0, 0.0, 0.0),
        rotation: (1.0, 0.0, 0.0, 0.0),
        animation: None,
        velocity: None,
        model_url: None,
    };

    let validation_result = player_state.validate();
    assert!(validation_result.is_err());
    
    let errors = validation_result.unwrap_err();
    assert!(errors.field_errors().contains_key("color"));
}

#[test]
fn 범위를_벗어난_위치값_검증_실패() {
    let player_state = PlayerState {
        name: "테스트플레이어".to_string(),
        color: "#FF5733".to_string(),
        position: (2000.0, 0.0, 0.0), // 범위 초과
        rotation: (1.0, 0.0, 0.0, 0.0),
        animation: None,
        velocity: None,
        model_url: None,
    };

    let validation_result = player_state.validate();
    assert!(validation_result.is_err());
    
    let errors = validation_result.unwrap_err();
    assert!(errors.field_errors().contains_key("position"));
}

#[test]
fn 잘못된_쿼터니언_검증_실패() {
    let player_state = PlayerState {
        name: "테스트플레이어".to_string(),
        color: "#FF5733".to_string(),
        position: (0.0, 0.0, 0.0),
        rotation: (2.0, 0.0, 0.0, 0.0), // 쿼터니언 크기가 1이 아님
        animation: None,
        velocity: None,
        model_url: None,
    };

    let validation_result = player_state.validate();
    assert!(validation_result.is_err());
    
    let errors = validation_result.unwrap_err();
    assert!(errors.field_errors().contains_key("rotation"));
}

#[test]
fn 유효한_join_메시지_파싱_성공() {
    // JSON을 문자열로 직접 생성하여 # 문자 문제 해결
    let json_value = json!({
        "type": "Join",
        "room_id": "room-123",
        "name": "플레이어1",
        "color": "#FF0000"
    });
    
    let json_str = to_string(&json_value).expect("JSON 직렬화 실패");

    let message: Result<ClientMessage, _> = from_str(&json_str);
    assert!(message.is_ok());

    if let Ok(ClientMessage::Join { room_id, name, color }) = message {
        assert_eq!(room_id, "room-123");
        assert_eq!(name, "플레이어1");
        assert_eq!(color, "#FF0000");
    } else {
        panic!("Join 메시지 파싱 실패");
    }
}

#[test]
fn 유효한_update_메시지_파싱_성공() {
    let json_value = json!({
        "type": "Update",
        "state": {
            "name": "플레이어1",
            "color": "#00FF00",
            "position": [10.0, 5.0, -3.2],
            "rotation": [1.0, 0.0, 0.0, 0.0],
            "animation": "walking"
        }
    });
    
    let json_str = to_string(&json_value).expect("JSON 직렬화 실패");

    let message: Result<ClientMessage, _> = from_str(&json_str);
    assert!(message.is_ok());

    if let Ok(ClientMessage::Update { state }) = message {
        assert_eq!(state.name, "플레이어1");
        assert_eq!(state.color, "#00FF00");
        assert_eq!(state.position, (10.0, 5.0, -3.2));
        assert_eq!(state.animation, Some("walking".to_string()));
    } else {
        panic!("Update 메시지 파싱 실패");
    }
}

#[test]
fn 유효한_chat_메시지_파싱_성공() {
    let json_value = json!({
        "type": "Chat",
        "message": "안녕하세요!"
    });
    
    let json_str = to_string(&json_value).expect("JSON 직렬화 실패");

    let message: Result<ClientMessage, _> = from_str(&json_str);
    assert!(message.is_ok());

    if let Ok(ClientMessage::Chat { message }) = message {
        assert_eq!(message, "안녕하세요!");
    } else {
        panic!("Chat 메시지 파싱 실패");
    }
}

#[test]
fn 잘못된_메시지_타입_파싱_실패() {
    let json_str = r#"
    {
        "type": "InvalidType",
        "data": "some data"
    }"#;

    let message: Result<ClientMessage, _> = from_str(&json_str);
    assert!(message.is_err());
}

#[test]
fn 필수_필드_누락시_파싱_실패() {
    let json_str = r#"
    {
        "type": "Join",
        "room_id": "room-123"
    }"#;

    let message: Result<ClientMessage, _> = from_str(&json_str);
    assert!(message.is_err());
}

#[test]
fn 메시지_검증_로직_테스트() {
    // 유효한 Join 메시지
    let valid_join = ClientMessage::Join {
        room_id: "room-123".to_string(),
        name: "플레이어".to_string(),
        color: "#FF0000".to_string(),
    };
    assert!(valid_join.validate().is_ok());

    // 잘못된 Join 메시지 (빈 룸 ID)
    let invalid_join = ClientMessage::Join {
        room_id: "".to_string(),
        name: "플레이어".to_string(),
        color: "#FF0000".to_string(),
    };
    assert!(invalid_join.validate().is_err());

    // 유효한 Chat 메시지
    let valid_chat = ClientMessage::Chat {
        message: "안녕하세요!".to_string(),
    };
    assert!(valid_chat.validate().is_ok());

    // 잘못된 Chat 메시지 (너무 긴 메시지)
    let long_message = "a".repeat(1001);
    let invalid_chat = ClientMessage::Chat {
        message: long_message,
    };
    assert!(invalid_chat.validate().is_err());
}

#[test]
fn 색상_정규식_검증_테스트() {
    let valid_colors = vec!["#FF0000", "#00FF00", "#0000FF", "#123456", "#ABCDEF"];
    let invalid_colors = vec!["FF0000", "#GG0000", "#12345", "#1234567", "red", ""];

    for color in valid_colors {
        let player_state = PlayerState {
            name: "테스트".to_string(),
            color: color.to_string(),
            position: (0.0, 0.0, 0.0),
            rotation: (1.0, 0.0, 0.0, 0.0),
            animation: None,
            velocity: None,
            model_url: None,
        };
        assert!(player_state.validate().is_ok(), "색상 '{}' 검증 실패", color);
    }

    for color in invalid_colors {
        let player_state = PlayerState {
            name: "테스트".to_string(),
            color: color.to_string(),
            position: (0.0, 0.0, 0.0),
            rotation: (1.0, 0.0, 0.0, 0.0),
            animation: None,
            velocity: None,
            model_url: None,
        };
        assert!(player_state.validate().is_err(), "잘못된 색상 '{}' 검증 통과", color);
    }
}