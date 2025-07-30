use anyhow::{Context, Result};
use mongodb::{Client as MongoClient, Database, Collection};
use redis::{Client as RedisClient, aio::Connection as RedisConnection};
use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};
use crate::config::Config;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatMessage {
    pub id: String,
    pub room_id: String,
    pub client_id: String,
    pub username: String,
    pub message: String,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RoomInfo {
    pub id: String,
    pub name: String,
    pub created_at: DateTime<Utc>,
    pub last_activity: DateTime<Utc>,
    pub player_count: u32,
}

#[derive(Clone)]
pub struct DatabaseManager {
    mongo_db: Database,
    redis_client: RedisClient,
}

impl DatabaseManager {
    pub async fn new(config: &Config) -> Result<Self> {
        // MongoDB 연결
        let mongo_client = MongoClient::with_uri_str(&config.mongodb_url)
            .await
            .context("MongoDB 연결 실패")?;
        
        let mongo_db = mongo_client.database(&config.mongodb_db_name);

        // Redis 연결
        let redis_client = RedisClient::open(config.redis_url.as_str())
            .context("Redis 클라이언트 생성 실패")?;

        // 연결 테스트
        let mut redis_conn = redis_client.get_async_connection().await
            .context("Redis 연결 실패")?;
        
        redis::cmd("PING").query_async::<_, String>(&mut redis_conn).await
            .context("Redis 핑 실패")?;

        Ok(DatabaseManager {
            mongo_db,
            redis_client,
        })
    }

    // Redis 연결 가져오기
    pub async fn get_redis_connection(&self) -> Result<RedisConnection> {
        self.redis_client.get_async_connection().await
            .context("Redis 연결 가져오기 실패")
    }

    // 채팅 메시지 저장 (MongoDB)
    pub async fn save_chat_message(&self, message: &ChatMessage) -> Result<()> {
        let collection: Collection<ChatMessage> = self.mongo_db.collection("chat_messages");
        collection.insert_one(message, None).await
            .context("채팅 메시지 저장 실패")?;
        Ok(())
    }

    // 채팅 기록 조회 (MongoDB)
    pub async fn get_chat_history(&self, room_id: &str, limit: u32) -> Result<Vec<ChatMessage>> {
        use mongodb::options::FindOptions;
        use futures::stream::TryStreamExt;
        
        let collection: Collection<ChatMessage> = self.mongo_db.collection("chat_messages");
        let filter = mongodb::bson::doc! { "room_id": room_id };
        let options = FindOptions::builder()
            .sort(mongodb::bson::doc! { "timestamp": -1 })
            .limit(limit as i64)
            .build();

        let cursor = collection.find(filter, options).await
            .context("채팅 기록 조회 실패")?;
        
        let messages: Vec<ChatMessage> = cursor.try_collect().await
            .context("채팅 기록 수집 실패")?;
        
        Ok(messages.into_iter().rev().collect())
    }

    // 룸 정보 저장/업데이트 (MongoDB)
    pub async fn upsert_room_info(&self, room_info: &RoomInfo) -> Result<()> {
        use mongodb::options::{UpdateOptions, ReplaceOptions};
        
        let collection: Collection<RoomInfo> = self.mongo_db.collection("rooms");
        let filter = mongodb::bson::doc! { "id": &room_info.id };
        
        let options = ReplaceOptions::builder().upsert(true).build();
        collection.replace_one(filter, room_info, options).await
            .context("룸 정보 저장 실패")?;
        Ok(())
    }

    // 활성 룸 목록 조회 (MongoDB)
    pub async fn get_active_rooms(&self) -> Result<Vec<RoomInfo>> {
        use futures::stream::TryStreamExt;
        use chrono::Duration;
        
        let collection: Collection<RoomInfo> = self.mongo_db.collection("rooms");
        let cutoff_time = Utc::now() - Duration::hours(1); // 1시간 이내 활동
        
        let filter = mongodb::bson::doc! { 
            "last_activity": { "$gte": mongodb::bson::to_bson(&cutoff_time).unwrap() } 
        };
        
        let cursor = collection.find(filter, None).await
            .context("활성 룸 조회 실패")?;
        
        cursor.try_collect().await
            .context("활성 룸 수집 실패")
    }

    // 플레이어 상태 캐시 (Redis)
    pub async fn cache_player_state(&self, room_id: &str, client_id: &str, state: &str) -> Result<()> {
        let mut conn = self.get_redis_connection().await?;
        let key = format!("room:{}:player:{}", room_id, client_id);
        
        redis::cmd("SETEX")
            .arg(&key)
            .arg(3600) // 1시간 TTL
            .arg(state)
            .query_async(&mut conn)
            .await
            .context("플레이어 상태 캐시 실패")?;
        Ok(())
    }

    // 플레이어 상태 조회 (Redis)
    pub async fn get_cached_player_state(&self, room_id: &str, client_id: &str) -> Result<Option<String>> {
        let mut conn = self.get_redis_connection().await?;
        let key = format!("room:{}:player:{}", room_id, client_id);
        
        let state: Option<String> = redis::cmd("GET")
            .arg(&key)
            .query_async(&mut conn)
            .await
            .context("플레이어 상태 조회 실패")?;
        Ok(state)
    }

    // 룸의 온라인 플레이어 수 업데이트 (Redis)
    pub async fn update_room_player_count(&self, room_id: &str, count: u32) -> Result<()> {
        let mut conn = self.get_redis_connection().await?;
        let key = format!("room:{}:count", room_id);
        
        redis::cmd("SETEX")
            .arg(&key)
            .arg(300) // 5분 TTL
            .arg(count)
            .query_async(&mut conn)
            .await
            .context("룸 플레이어 수 업데이트 실패")?;
        Ok(())
    }

    // 플레이어 연결 해제 시 정리 (Redis)
    pub async fn cleanup_player_cache(&self, room_id: &str, client_id: &str) -> Result<()> {
        let mut conn = self.get_redis_connection().await?;
        let key = format!("room:{}:player:{}", room_id, client_id);
        
        redis::cmd("DEL")
            .arg(&key)
            .query_async(&mut conn)
            .await
            .context("플레이어 캐시 정리 실패")?;
        Ok(())
    }
} 