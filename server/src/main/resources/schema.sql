CREATE TABLE IF NOT EXISTS `users` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255),
    email VARCHAR(255),
    hashedPassword VARCHAR(255),
    imageUrl VARCHAR(255),
    githubUrl VARCHAR(255),
    summary TEXT
);

CREATE TABLE IF NOT EXISTS category (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    thumbnail VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS post (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    summary TEXT,
    content TEXT,
    images TEXT,
    viewcount INT DEFAULT 0,
    site VARCHAR(255),
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    userId INT,
    categoryId INT,
    FOREIGN KEY (userId) REFERENCES `users`(id),
    FOREIGN KEY (categoryId) REFERENCES category(id)
);

-- 사용자 역할 관리 테이블 추가
CREATE TABLE IF NOT EXISTS user_organization_role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    userId INT NOT NULL,
    organizationId INT,
    role VARCHAR(50) DEFAULT 'ACTIVE_MEMBER',
    permissions TEXT,
    isActive BOOLEAN DEFAULT TRUE,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userId) REFERENCES `users`(id),
    INDEX idx_user_active (userId, isActive),
    INDEX idx_user_org (userId, organizationId)
);

INSERT INTO category (title, thumbnail) VALUES 
('기본 카테고리', 'default.jpg'),
('개발', 'dev.jpg'),
('일상', 'daily.jpg')
ON DUPLICATE KEY UPDATE title=VALUES(title); 