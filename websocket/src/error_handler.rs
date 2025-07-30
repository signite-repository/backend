use std::panic;
use std::process;
use tracing::{error, info};

/// 패닉 핸들러 설정
pub fn setup_panic_handler() {
    let default_panic = panic::take_hook();
    
    panic::set_hook(Box::new(move |panic_info| {
        error!("서버 패닉 발생: {:?}", panic_info);
        
        // 기본 패닉 핸들러 호출
        default_panic(panic_info);
        
        // 프로세스 종료 (systemd나 k8s가 재시작하도록)
        process::exit(1);
    }));
}

/// 안전한 종료 처리
pub async fn graceful_shutdown() {
    info!("서버 종료 신호 수신, 안전하게 종료 중...");
    
    // 새로운 연결 거부
    // 기존 연결 정리
    // 리소스 정리
    
    tokio::time::sleep(tokio::time::Duration::from_secs(5)).await;
    info!("서버 종료 완료");
}