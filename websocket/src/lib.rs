pub mod config;
pub mod db;
pub mod http;

use serde::{Deserialize, Serialize};
use validator::{Validate, ValidationError};
use lazy_static::lazy_static;

// main.rs에서 사용하는 구조체들을 pub으로 export
pub use config::Config;
pub use db::{DatabaseManager, ChatMessage, RoomInfo};
pub use http::{AppState, create_router};

lazy_static! {
    static ref COLOR_REGEX: regex::Regex = regex::Regex::new(r"^#[0-9A-Fa-f]{6}$").unwrap();
}

#[derive(Serialize, Deserialize, Debug, Clone, Validate)]
pub struct PlayerState {
    #[validate(length(min = 1, max = 50, message = "이름은 1-50자 사이여야 합니다"))]
    pub name: String,
    #[validate(regex(path = "COLOR_REGEX", message = "올바른 색상 형식이 아닙니다"))]
    pub color: String,
    #[validate(custom(function = "validate_position"))]
    pub position: (f32, f32, f32),
    #[validate(custom(function = "validate_quaternion"))]
    pub rotation: (f32, f32, f32, f32),
    #[serde(skip_serializing_if = "Option::is_none")]
    pub animation: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub velocity: Option<(f32, f32, f32)>,
    #[serde(skip_serializing_if = "Option::is_none")]
    #[serde(rename = "modelUrl")]
    pub model_url: Option<String>,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(tag = "type")]
pub enum ClientMessage {
    Join { 
        room_id: String, 
        name: String, 
        color: String 
    },
    Update { 
        state: PlayerState 
    },
    Chat { 
        message: String 
    },
}

fn validate_position(position: &(f32, f32, f32)) -> Result<(), ValidationError> {
    let (x, y, z) = *position;
    if x.is_finite() && y.is_finite() && z.is_finite() && 
       x.abs() <= 1000.0 && y.abs() <= 1000.0 && z.abs() <= 1000.0 {
        Ok(())
    } else {
        Err(ValidationError::new("invalid_position"))
    }
}

fn validate_quaternion(rotation: &(f32, f32, f32, f32)) -> Result<(), ValidationError> {
    let (w, x, y, z) = *rotation;
    if w.is_finite() && x.is_finite() && y.is_finite() && z.is_finite() {
        let magnitude = (w * w + x * x + y * y + z * z).sqrt();
        if (magnitude - 1.0).abs() < 0.1 {
            Ok(())
        } else {
            Err(ValidationError::new("invalid_quaternion"))
        }
    } else {
        Err(ValidationError::new("invalid_quaternion"))
    }
}

impl ClientMessage {
    pub fn validate(&self) -> Result<(), validator::ValidationErrors> {
        match self {
            ClientMessage::Join { room_id, name, color } => {
                let mut errors = validator::ValidationErrors::new();
                
                if room_id.is_empty() {
                    errors.add("room_id", ValidationError::new("empty_room_id"));
                }
                if name.is_empty() || name.len() > 50 {
                    errors.add("name", ValidationError::new("invalid_name"));
                }
                if !COLOR_REGEX.is_match(color) {
                    errors.add("color", ValidationError::new("invalid_color"));
                }
                
                if errors.is_empty() {
                    Ok(())
                } else {
                    Err(errors)
                }
            },
            ClientMessage::Update { state } => {
                state.validate()
            },
            ClientMessage::Chat { message } => {
                let mut errors = validator::ValidationErrors::new();
                
                if message.is_empty() || message.len() > 1000 {
                    errors.add("message", ValidationError::new("invalid_message_length"));
                }
                
                if errors.is_empty() {
                    Ok(())
                } else {
                    Err(errors)
                }
            },
        }
    }
}