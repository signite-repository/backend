CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE IF NOT EXISTS categories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL,
    parent_id     UUID,
    path          LTREE,
    level         INTEGER NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL DEFAULT 0,
    metadata      JSONB DEFAULT '{}',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_categories_path_gist ON categories USING GIST(path);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);