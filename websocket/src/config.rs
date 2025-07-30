use std::env;

#[derive(Clone)]
pub struct Config {
    pub server_host: String,
    pub server_port: u16,
    pub http_port: u16,
    pub redis_url: String,
    pub mongodb_url: String,
    pub mongodb_db_name: String,
    pub jwt_secret: String,
    pub jwt_issuer: String,
    pub allowed_origins: Vec<String>,
}

impl Config {
    pub fn from_env() -> Self {
        dotenv::dotenv().ok();

        let allowed_origins_str = env::var("ALLOWED_ORIGINS")
            .unwrap_or_else(|_| "http://localhost:5174,https://signight.com".to_string());
        
        let allowed_origins = allowed_origins_str
            .split(',')
            .map(|s| s.trim().to_string())
            .collect();

        Config {
            server_host: env::var("SERVER_HOST").unwrap_or_else(|_| "0.0.0.0".to_string()),
            server_port: env::var("SERVER_PORT")
                .unwrap_or_else(|_| "8080".to_string())
                .parse()
                .expect("SERVER_PORT must be a valid port number"),
            http_port: env::var("HTTP_PORT")
                .unwrap_or_else(|_| "3001".to_string())
                .parse()
                .expect("HTTP_PORT must be a valid port number"),
            redis_url: env::var("REDIS_URL").unwrap_or_else(|_| "redis://redis:6379".to_string()),
            mongodb_url: env::var("MONGODB_URL").unwrap_or_else(|_| "mongodb://mongo:27017".to_string()),
            mongodb_db_name: env::var("MONGODB_DB_NAME").unwrap_or_else(|_| "signight_websocket".to_string()),
            jwt_secret: env::var("JWT_SECRET").unwrap_or_else(|_| "your-jwt-secret-key".to_string()),
            jwt_issuer: env::var("JWT_ISSUER").unwrap_or_else(|_| "https://auth.signite.com".to_string()),
            allowed_origins,
        }
    }

    pub fn websocket_addr(&self) -> String {
        format!("{}:{}", self.server_host, self.server_port)
    }

    pub fn http_addr(&self) -> String {
        format!("{}:{}", self.server_host, self.http_port)
    }
} 