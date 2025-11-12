CREATE TABLE IF NOT EXISTS `ums_tree` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `parent_id` BIGINT NOT NULL COMMENT '父级id',
    `name` VARCHAR(100) DEFAULT NULL COMMENT '名称' COLLATE 'utf8mb4_general_ci',
    `description` VARCHAR(100) DEFAULT NULL COMMENT '中文描述' COLLATE 'utf8mb4_general_ci',
    `type` INT DEFAULT NULL COMMENT '1 应用 2部门',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
