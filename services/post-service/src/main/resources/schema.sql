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

-- Comment 테이블 생성 (MariaDB)
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    depth INT DEFAULT 0,
    path VARCHAR(255) DEFAULT '',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_comments_post_id (post_id),
    INDEX idx_comments_parent_id (parent_id),
    INDEX idx_comments_path (path),
    INDEX idx_comments_author_id (author_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);