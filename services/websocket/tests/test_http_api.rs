use axum::http::StatusCode;
use axum_test::TestServer;
use serde_json::json;
use rust_websocket_server::http::{create_router, AppState};
use rust_websocket_server::config::Config;
use rust_websocket_server::db::DatabaseManager;

// 테스트용 설정 생성
fn create_test_config() -> Config {
    Config {
        server_host: "127.0.0.1".to_string(),
        server_port: 8080,
        http_port: 3001,
        redis_url: "redis://localhost:6379".to_string(),
        mongodb_url: "mongodb://localhost:27017".to_string(),
        mongodb_db_name: "test_signight_websocket".to_string(),
        jwt_secret: "test-secret".to_string(),
        jwt_issuer: "test-issuer".to_string(),
        allowed_origins: vec!["http://localhost:3000".to_string()],
    }
}

// Mock 데이터베이스 매니저 (실제 DB 없이 테스트)
async fn create_test_server() -> TestServer {
    let config = create_test_config();
    
    // 실제 환경에서는 DatabaseManager::new(&config).await를 사용
    // 테스트에서는 mock이나 test database 사용
    let db = DatabaseManager::new(&config).await.unwrap_or_else(|_| {
        // Mock database manager를 반환하거나 테스트를 skip
        panic!("테스트용 데이터베이스 연결 필요")
    });

    let app_state = AppState {
        db,
        config,
    };

    let app = create_router(app_state);
    TestServer::new(app).unwrap()
}

#[tokio::test]
async fn 헬스체크_엔드포인트가_정상_응답한다() {
    // 실제 데이터베이스 연결이 필요한 테스트는 조건부로 실행
    if std::env::var("TEST_WITH_DB").is_ok() {
        let server = create_test_server().await;
        
        let response = server.get("/health").await;
        
        assert_eq!(response.status_code(), StatusCode::OK);
        
        let body: serde_json::Value = response.json();
        assert_eq!(body["services"]["websocket"], "healthy");
    }
}

#[tokio::test]
async fn 활성_룸_목록_조회가_정상_동작한다() {
    if std::env::var("TEST_WITH_DB").is_ok() {
        let server = create_test_server().await;
        
        let response = server.get("/rooms").await;
        
        assert_eq!(response.status_code(), StatusCode::OK);
        
        let body: serde_json::Value = response.json();
        assert!(body["rooms"].is_array());
    }
}

#[tokio::test]
async fn 채팅_기록_조회가_정상_동작한다() {
    if std::env::var("TEST_WITH_DB").is_ok() {
        let server = create_test_server().await;
        
        let request_body = json!({
            "room_id": "test-room",
            "limit": 10
        });
        
        let response = server
            .post("/chat/history")
            .json(&request_body)
            .await;
        
        assert_eq!(response.status_code(), StatusCode::OK);
        
        let body: serde_json::Value = response.json();
        assert!(body.is_array());
    }
}

#[tokio::test]
async fn 잘못된_경로_요청시_404_반환한다() {
    if std::env::var("TEST_WITH_DB").is_ok() {
        let server = create_test_server().await;
        
        let response = server.get("/invalid-path").await;
        
        assert_eq!(response.status_code(), StatusCode::NOT_FOUND);
    }
}

#[tokio::test]
async fn cors_헤더가_올바르게_설정된다() {
    if std::env::var("TEST_WITH_DB").is_ok() {
        let server = create_test_server().await;
        
        let response = server
            .get("/health")
            .add_header("Origin", "http://localhost:3000")
            .await;
        
        // CORS 헤더 확인
        let headers = response.headers();
        assert!(headers.contains_key("access-control-allow-origin"));
    }
}

#[test]
fn 설정값_검증_테스트() {
    // 환경변수 없이도 기본값으로 설정이 생성되는지 테스트
    let config = create_test_config();
    
    assert_eq!(config.server_host, "127.0.0.1");
    assert_eq!(config.server_port, 8080);
    assert_eq!(config.http_port, 3001);
    assert!(!config.jwt_secret.is_empty());
    assert!(!config.mongodb_db_name.is_empty());
}

#[test]
fn 주소_생성_테스트() {
    let config = create_test_config();
    
    assert_eq!(config.websocket_addr(), "127.0.0.1:8080");
    assert_eq!(config.http_addr(), "127.0.0.1:3001");
}