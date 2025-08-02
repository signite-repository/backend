use actix_web::{web, App, HttpServer, middleware::Logger};
use std::io;

mod config;
mod db;
mod models;
mod handlers;

use config::Config;
use db::DatabaseManager;

#[actix_web::main]
async fn main() -> io::Result<()> {
    // 로거 초기화
    env_logger::init_from_env(env_logger::Env::new().default_filter_or("info"));

    // 설정 로드
    let config = Config::from_env();
    log::info!("Comment Service 시작 중...");
    log::info!("서버 주소: {}", config.server_addr());
    log::info!("데이터베이스 URI: {}", config.database_uri);

    // MongoDB 연결
    let db_manager = match DatabaseManager::new(&config).await {
        Ok(db) => db,
        Err(e) => {
            log::error!("데이터베이스 연결 실패: {}", e);
            return Err(io::Error::new(io::ErrorKind::ConnectionRefused, e));
        }
    };

    log::info!("Comment Service 시작됨: {}", config.server_addr());

    HttpServer::new(move || {
        App::new()
            .app_data(web::Data::new(db_manager.clone()))
            .wrap(Logger::default())
            .service(
                web::scope("/api/v1")
                    .route("/health", web::get().to(handlers::health_check))
                    .route("/comments", web::post().to(handlers::create_comment))
                    .route("/comments/post/{post_id}", web::get().to(handlers::get_comments))
                    .route("/comments/{comment_id}", web::put().to(handlers::update_comment))
                    .route("/comments/{comment_id}", web::delete().to(handlers::delete_comment))
            )
            // 기존 헬스체크 경로 유지 (K8s 프로브용)
            .route("/health", web::get().to(handlers::health_check))
    })
    .bind(&config.server_addr())?
    .run()
    .await
}
