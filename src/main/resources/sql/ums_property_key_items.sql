-- 创建属性键枚举项表
-- 用于存储某些属性键的可选枚举值，例如 pd-expert-privileges 的 admin、user 等值

CREATE TABLE `ums_property_key_items` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联的属性键（如：pd-expert-privileges）',
  `item_value` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '枚举值（如：admin、user）',
  `item_label` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '枚举值显示标签（如：管理员、普通用户）',
  `priority` int DEFAULT 0 COMMENT '优先级（数字越小优先级越高，用于排序）',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用（0:禁用 1:启用）',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注说明',
  PRIMARY KEY (`id`),
  KEY `idx_property_key` (`key`),
  KEY `idx_item_value` (`item_value`),
  KEY `idx_priority` (`priority`),
  UNIQUE KEY `uk_property_key_item_value` (`key`, `item_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='属性键枚举项表';

-- 示例数据：为 pd-expert-privileges 属性键添加枚举值
-- 注意：执行前需要确保 ums_property_keys 表中已存在 pd-expert-privileges 这个键

INSERT INTO `ums_property_key_items` (`key`, `item_value`, `item_label`, `priority`, `remark`)
VALUES
  ('pd-expert-privileges', 'admin', '管理员权限', 1, 'PD专家管理员权限，拥有所有操作权限'),
  ('pd-expert-privileges', 'user', '普通用户权限', 2, 'PD专家普通用户权限，仅有基本查看和使用权限');

-- 查询某个属性键的所有枚举项（按优先级排序）
-- SELECT * FROM ums_property_key_items WHERE `key` = 'pd-expert-privileges' ORDER BY priority ASC;

-- 查询指定属性键的所有启用枚举项
-- SELECT * FROM ums_property_key_items
-- WHERE `key` = 'pd-expert-privileges' AND enabled = 1
-- ORDER BY priority ASC;
