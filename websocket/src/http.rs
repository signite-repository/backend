use axum::{
    extract::State,
    http::StatusCode,
    response::Json,
    routing::{get, post},
    Router,
};
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tower_http::cors::{CorsLayer, Any};
use crate::{db::DatabaseManager, config::Config};

#[derive(Clone)]
pub struct AppState {
    pub db: DatabaseManager,
    pub config: Config,
}

#[derive(Serialize)]
struct HealthResponse {
    status: String,
    timestamp: String,
    services: HealthServices,
}

#[derive(Serialize)]
struct HealthServices {
    websocket: String,
    mongodb: String,
    redis: String,
}

#[derive(Serialize)]
struct RoomsResponse {
    rooms: Vec<crate::db::RoomInfo>,
}

#[derive(Deserialize)]
struct ChatHistoryQuery {
    room_id: String,
    limit: Option<u32>,
}

pub fn create_router(state: AppState) -> Router {
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    Router::new()
        .route("/health", get(health_check))
        .route("/rooms", get(get_active_rooms))
        .route("/chat/history", post(get_chat_history))
        .layer(cors)
        .with_state(Arc::new(state))
}

async fn health_check(
    State(state): State<Arc<AppState>>,
) -> Result<Json<HealthResponse>, StatusCode> {
    let timestamp = chrono::Utc::now().to_rfc3339();
    
    // MongoDB 상태 확인
    let mongodb_status = match state.db.get_active_rooms().await {
        Ok(_) => "healthy",
        Err(_) => "unhealthy",
    };

    // Redis 상태 확인
    let redis_status = match state.db.get_redis_connection().await {
        Ok(_) => "healthy",
        Err(_) => "unhealthy",
    };

    let overall_status = if mongodb_status == "healthy" && redis_status == "healthy" {
        "healthy"
    } else {
        "unhealthy"
    };

    let response = HealthResponse {
        status: overall_status.to_string(),
        timestamp,
        services: HealthServices {
            websocket: "healthy".to_string(), // WebSocket은 별도 체크가 어려우므로 기본값
            mongodb: mongodb_status.to_string(),
            redis: redis_status.to_string(),
        },
    };

    if overall_status == "healthy" {
        Ok(Json(response))
    } else {
        Err(StatusCode::SERVICE_UNAVAILABLE)
    }
}

async fn get_active_rooms(
    State(state): State<Arc<AppState>>,
) -> Result<Json<RoomsResponse>, StatusCode> {
    match state.db.get_active_rooms().await {
        Ok(rooms) => Ok(Json(RoomsResponse { rooms })),
        Err(e) => {
            tracing::error!("활성 룸 조회 실패: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn get_chat_history(
    State(state): State<Arc<AppState>>,
    Json(query): Json<ChatHistoryQuery>,
) -> Result<Json<Vec<crate::db::ChatMessage>>, StatusCode> {
    let limit = query.limit.unwrap_or(50).min(100); // 최대 100개로 제한
    
    match state.db.get_chat_history(&query.room_id, limit).await {
        Ok(messages) => Ok(Json(messages)),
        Err(e) => {
            tracing::error!("채팅 기록 조회 실패: {}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
} 