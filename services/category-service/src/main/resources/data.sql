INSERT INTO categories (name, slug, path, level, display_order) VALUES
('공지사항', 'notice', 'notice', 0, 1) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, slug, path, level, display_order) VALUES
('SIG 활동', 'sig', 'sig', 0, 2) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, slug, path, level, display_order) VALUES  
('자유게시판', 'free', 'free', 0, 3) ON CONFLICT DO NOTHING;