-- V004: Mark sensitive fields in dataset_fields.
-- Values of these fields must never appear verbatim in context_summary / reports / logs.
ALTER TABLE dataset_fields
    ADD COLUMN is_sensitive TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为敏感字段(手机号/邮箱/身份证/账号等)';
