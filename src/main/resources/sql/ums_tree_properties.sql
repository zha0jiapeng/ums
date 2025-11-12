CREATE TABLE IF NOT EXISTS `ums_tree_properties` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `tree_id` BIGINT NOT NULL COMMENT '树id',
    `key` VARCHAR(100) DEFAULT NULL COMMENT 'key名称' COLLATE 'utf8mb4_general_ci',
    `required` TINYINT DEFAULT NULL COMMENT '是否必填',
    `values` VARCHAR(100) DEFAULT NULL COMMENT '枚举' COLLATE 'utf8mb4_general_ci',
    `description` VARCHAR(100) DEFAULT NULL COMMENT '中文描述' COLLATE 'utf8mb4_general_ci',
    `default_value` VARCHAR(100) DEFAULT NULL COMMENT '默认值' COLLATE 'utf8mb4_general_ci',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
