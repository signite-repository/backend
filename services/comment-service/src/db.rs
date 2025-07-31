use anyhow::{Context, Result};
use mongodb::{Client as MongoClient, Database, Collection, bson::{doc, oid::ObjectId}};
use futures::stream::TryStreamExt;
use chrono::Utc;
use crate::config::Config;
use crate::models::{Comment, CreateCommentRequest, UpdateCommentRequest, CommentResponse};

#[derive(Clone)]
pub struct DatabaseManager {
    db: Database,
}

impl DatabaseManager {
    pub async fn new(config: &Config) -> Result<Self> {
        // MongoDB 연결
        let client = MongoClient::with_uri_str(&config.database_uri)
            .await
            .context("MongoDB 연결 실패")?;
        
        let db = client.database(&config.database_name);

        // 연결 테스트
        db.run_command(doc! {"ping": 1}, None)
            .await
            .context("MongoDB 핑 실패")?;

        log::info!("MongoDB 연결 성공: {}", config.database_name);

        Ok(DatabaseManager { db })
    }

    // 댓글 생성
    pub async fn create_comment(&self, request: CreateCommentRequest) -> Result<String> {
        let collection: Collection<Comment> = self.db.collection("comments");
        
        let comment = Comment {
            id: None,
            post_id: request.post_id,
            user_id: request.user_id,
            username: request.username,
            content: request.content,
            parent_id: request.parent_id,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            is_deleted: false,
        };

        let result = collection.insert_one(&comment, None)
            .await
            .context("댓글 생성 실패")?;

        Ok(result.inserted_id.as_object_id().unwrap().to_hex())
    }

    // 포스트의 댓글 목록 조회 (계층 구조)
    pub async fn get_comments_by_post(&self, post_id: &str) -> Result<Vec<CommentResponse>> {
        let collection: Collection<Comment> = self.db.collection("comments");
        
        let filter = doc! {
            "post_id": post_id,
            "is_deleted": false
        };

        let sort = doc! { "created_at": 1 };
        let options = mongodb::options::FindOptions::builder().sort(sort).build();

        let cursor = collection.find(filter, options)
            .await
            .context("댓글 조회 실패")?;

        let comments: Vec<Comment> = cursor.try_collect()
            .await
            .context("댓글 수집 실패")?;

        // 부모 댓글과 대댓글을 분리하여 계층 구조 만들기
        let mut parent_comments: Vec<CommentResponse> = Vec::new();
        let mut replies_map: std::collections::HashMap<String, Vec<CommentResponse>> = std::collections::HashMap::new();

        for comment in comments {
            let comment_response = CommentResponse::from(comment.clone());
            
            if let Some(parent_id) = &comment.parent_id {
                replies_map.entry(parent_id.clone())
                    .or_insert_with(Vec::new)
                    .push(comment_response);
            } else {
                parent_comments.push(comment_response);
            }
        }

        // 부모 댓글에 대댓글 붙이기
        for parent_comment in &mut parent_comments {
            if let Some(replies) = replies_map.remove(&parent_comment.id) {
                parent_comment.replies = replies;
            }
        }

        Ok(parent_comments)
    }

    // 댓글 수정
    pub async fn update_comment(&self, comment_id: &str, request: UpdateCommentRequest) -> Result<bool> {
        let collection: Collection<Comment> = self.db.collection("comments");
        
        let object_id = ObjectId::parse_str(comment_id)
            .context("잘못된 댓글 ID")?;

        let filter = doc! { "_id": object_id };
        let update = doc! {
            "$set": {
                "content": &request.content,
                "updated_at": mongodb::bson::to_bson(&Utc::now()).unwrap()
            }
        };

        let result = collection.update_one(filter, update, None)
            .await
            .context("댓글 수정 실패")?;

        Ok(result.modified_count > 0)
    }

    // 댓글 삭제 (소프트 삭제)
    pub async fn delete_comment(&self, comment_id: &str) -> Result<bool> {
        let collection: Collection<Comment> = self.db.collection("comments");
        
        let object_id = ObjectId::parse_str(comment_id)
            .context("잘못된 댓글 ID")?;

        let filter = doc! { "_id": object_id };
        let update = doc! {
            "$set": {
                "is_deleted": true,
                "updated_at": mongodb::bson::to_bson(&Utc::now()).unwrap()
            }
        };

        let result = collection.update_one(filter, update, None)
            .await
            .context("댓글 삭제 실패")?;

        Ok(result.modified_count > 0)
    }

    // 특정 댓글 조회
    pub async fn get_comment_by_id(&self, comment_id: &str) -> Result<Option<Comment>> {
        let collection: Collection<Comment> = self.db.collection("comments");
        
        let object_id = ObjectId::parse_str(comment_id)
            .context("잘못된 댓글 ID")?;

        let filter = doc! { 
            "_id": object_id,
            "is_deleted": false
        };

        let comment = collection.find_one(filter, None)
            .await
            .context("댓글 조회 실패")?;

        Ok(comment)
    }
}