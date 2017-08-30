alter table email add column content_type varchar(32) NOT NULL DEFAULT 'PLAIN';
alter table email add column raw_data TEXT NOT NULL;