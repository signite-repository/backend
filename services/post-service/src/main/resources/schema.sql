-- Post 테이블 생성 (MariaDB)
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content LONGTEXT NOT NULL,
    author_id VARCHAR(100) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    tags JSON DEFAULT ('[]'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_posts_category_id (category_id),
    INDEX idx_posts_author_id (author_id),
    INDEX idx_posts_created_at (created_at)
);