use actix_web::{web, HttpResponse, Result};
use crate::db::DatabaseManager;
use crate::models::{CreateCommentRequest, UpdateCommentRequest};

// 헬스 체크
pub async fn health_check() -> Result<HttpResponse> {
    Ok(HttpResponse::Ok().json(serde_json::json!({
        "status": "healthy",
        "service": "comment-service"
    })))
}

// 댓글 생성
pub async fn create_comment(
    db: web::Data<DatabaseManager>,
    request: web::Json<CreateCommentRequest>,
) -> Result<HttpResponse> {
    match db.create_comment(request.into_inner()).await {
        Ok(comment_id) => Ok(HttpResponse::Created().json(serde_json::json!({
            "success": true,
            "comment_id": comment_id
        }))),
        Err(e) => {
            log::error!("댓글 생성 실패: {}", e);
            Ok(HttpResponse::InternalServerError().json(serde_json::json!({
                "success": false,
                "error": "댓글 생성에 실패했습니다"
            })))
        }
    }
}

// 포스트의 댓글 목록 조회
pub async fn get_comments(
    db: web::Data<DatabaseManager>,
    path: web::Path<String>,
) -> Result<HttpResponse> {
    let post_id = path.into_inner();
    
    match db.get_comments_by_post(&post_id).await {
        Ok(comments) => Ok(HttpResponse::Ok().json(serde_json::json!({
            "success": true,
            "comments": comments
        }))),
        Err(e) => {
            log::error!("댓글 조회 실패: {}", e);
            Ok(HttpResponse::InternalServerError().json(serde_json::json!({
                "success": false,
                "error": "댓글 조회에 실패했습니다"
            })))
        }
    }
}

// 댓글 수정
pub async fn update_comment(
    db: web::Data<DatabaseManager>,
    path: web::Path<String>,
    request: web::Json<UpdateCommentRequest>,
) -> Result<HttpResponse> {
    let comment_id = path.into_inner();
    
    match db.update_comment(&comment_id, request.into_inner()).await {
        Ok(true) => Ok(HttpResponse::Ok().json(serde_json::json!({
            "success": true,
            "message": "댓글이 수정되었습니다"
        }))),
        Ok(false) => Ok(HttpResponse::NotFound().json(serde_json::json!({
            "success": false,
            "error": "댓글을 찾을 수 없습니다"
        }))),
        Err(e) => {
            log::error!("댓글 수정 실패: {}", e);
            Ok(HttpResponse::InternalServerError().json(serde_json::json!({
                "success": false,
                "error": "댓글 수정에 실패했습니다"
            })))
        }
    }
}

// 댓글 삭제
pub async fn delete_comment(
    db: web::Data<DatabaseManager>,
    path: web::Path<String>,
) -> Result<HttpResponse> {
    let comment_id = path.into_inner();
    
    match db.delete_comment(&comment_id).await {
        Ok(true) => Ok(HttpResponse::Ok().json(serde_json::json!({
            "success": true,
            "message": "댓글이 삭제되었습니다"
        }))),
        Ok(false) => Ok(HttpResponse::NotFound().json(serde_json::json!({
            "success": false,
            "error": "댓글을 찾을 수 없습니다"
        }))),
        Err(e) => {
            log::error!("댓글 삭제 실패: {}", e);
            Ok(HttpResponse::InternalServerError().json(serde_json::json!({
                "success": false,
                "error": "댓글 삭제에 실패했습니다"
            })))
        }
    }
}