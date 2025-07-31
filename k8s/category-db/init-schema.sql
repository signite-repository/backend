-- categories 테이블 생성 스크립트
CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE IF NOT EXISTS categories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL,
    parent_id     UUID REFERENCES categories(id) ON DELETE SET NULL,
    path          LTREE,
    level         INTEGER NOT NULL CHECK (level >= 0),
    display_order INTEGER NOT NULL DEFAULT 0,
    metadata      JSONB DEFAULT '{}',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_categories_path_gist ON categories USING GIST(path);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);

-- 샘플 데이터 추가
INSERT INTO categories (name, slug, parent_id, path, level, display_order, metadata) VALUES
('공지사항', 'notice', NULL, 'notice', 0, 1, '{"icon": "📢"}'),
('SIG 활동', 'sig', NULL, 'sig', 0, 2, '{"icon": "👥"}'),
('자유게시판', 'free', NULL, 'free', 0, 3, '{"icon": "💬"}');

-- 하위 카테고리 추가
INSERT INTO categories (name, slug, parent_id, path, level, display_order, metadata) 
SELECT 
    '중요 공지', 'important', c.id, c.path || '.important'::ltree, 1, 1, '{"icon": "⚠️"}'
FROM categories c WHERE c.slug = 'notice';

INSERT INTO categories (name, slug, parent_id, path, level, display_order, metadata) 
SELECT 
    '백엔드 SIG', 'backend', c.id, c.path || '.backend'::ltree, 1, 1, '{"icon": "🔧"}'
FROM categories c WHERE c.slug = 'sig';

INSERT INTO categories (name, slug, parent_id, path, level, display_order, metadata) 
SELECT 
    '프론트엔드 SIG', 'frontend', c.id, c.path || '.frontend'::ltree, 1, 2, '{"icon": "🎨"}'
FROM categories c WHERE c.slug = 'sig';