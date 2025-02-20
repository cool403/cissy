(ns xhs.create-table-sql)

;; xhs_pages definition
(defonce xhs-pages-table-sql 
  "CREATE TABLE xhs_pages (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	page_url TEXT(200) NOT NULL,
	craw_status TEXT(4), 
    last_visit_time INTEGER, 
    create_time INTEGER, 
    lastmodifiy_time INTEGER
);")

;; xhs_posts definition
(defonce xhs-posts-table-sql 
  "CREATE TABLE xhs_posts (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	page_id INTEGER,
	uid TEXT(50),
	post TEXT,
	post_url TEXT DEFAULT ('200'),
	is_visited TEXT(4),
	last_visit_time INTEGER,
	tag TEXT(10),
	comments_count INTEGER,
	creat_time INTEGER,
	lastmodify_time INTEGER
);")

;; xhs_comments definition
(defonce xhs-comments-table-sql
  "CREATE TABLE xhs_comments (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	page_url INTEGER,
	post_id INTEGER,
	post_uid TEXT(40),
	mention_uid TEXT DEFAULT ('40'),
	comment TEXT,
	comment_time INTEGER,
	create_time INTEGER,
	lastmodify_time INTEGER
);")
