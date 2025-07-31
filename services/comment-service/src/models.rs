use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Comment {
    #[serde(rename = "_id", skip_serializing_if = "Option::is_none")]
    pub id: Option<mongodb::bson::oid::ObjectId>,
    pub post_id: String,
    pub user_id: String,
    pub username: String,
    pub content: String,
    pub parent_id: Option<String>, // 대댓글용
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub is_deleted: bool,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CreateCommentRequest {
    pub post_id: String,
    pub user_id: String,
    pub username: String,
    pub content: String,
    pub parent_id: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct UpdateCommentRequest {
    pub content: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CommentResponse {
    pub id: String,
    pub post_id: String,
    pub user_id: String,
    pub username: String,
    pub content: String,
    pub parent_id: Option<String>,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub replies: Vec<CommentResponse>, // 대댓글들
}

impl From<Comment> for CommentResponse {
    fn from(comment: Comment) -> Self {
        CommentResponse {
            id: comment.id.map(|oid| oid.to_hex()).unwrap_or_default(),
            post_id: comment.post_id,
            user_id: comment.user_id,
            username: comment.username,
            content: comment.content,
            parent_id: comment.parent_id,
            created_at: comment.created_at,
            updated_at: comment.updated_at,
            replies: Vec::new(), // 별도로 채워야 함
        }
    }
}