CREATE TABLE `ums_user_properties` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `key` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '属性键',
  `value` blob COMMENT '属性值',
  `data_type` int DEFAULT 9 COMMENT '数据类型(0:string,1:integer,2:float,3:double,4:long,5:boolean,6:json,7:binary,8:datetime,9:array,10:unknown)',
  `scope` tinyint(1) DEFAULT NULL COMMENT '属性范围',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_key` (`key`),
  KEY `idx_data_type` (`data_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci; 