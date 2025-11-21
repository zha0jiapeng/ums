-- 为 ums_property_keys 表添加 override_parent 字段
-- 该字段用于标识当用户自己有该 key 时，是否忽略父集的该属性

ALTER TABLE `ums_property_keys`
ADD COLUMN `override_parent` tinyint(1) DEFAULT 0 COMMENT '是否覆盖父集属性（0:继承父集 1:只用当前用户的，忽略父集）';

-- 示例：将 storage 设置为覆盖父集
-- UPDATE `ums_property_keys` SET `override_parent` = 1 WHERE `key` = 'storage';
