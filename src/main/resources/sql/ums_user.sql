CREATE TABLE `ums_user` (
  `id` bigint NOT NULL COMMENT '用户id',
  `type` tinyint(1) DEFAULT NULL COMMENT '类型',
  `unique_id` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '值',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci; 