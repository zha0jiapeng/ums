-- 为ums_user_properties表添加data_type字段
-- 执行前请先备份数据库

-- 添加data_type字段
ALTER TABLE `ums_user_properties` 
ADD COLUMN `data_type` int DEFAULT 10 COMMENT '数据类型(0:string,1:integer,2:float,3:double,4:long,5:boolean,6:json,7:binary,8:datetime,9:array,10:unknown)' AFTER `value`;

-- 添加索引
ALTER TABLE `ums_user_properties` 
ADD INDEX `idx_data_type` (`data_type`);

-- 更新现有数据的data_type字段
-- 根据value的内容推断数据类型（这里只是示例，实际可能需要更复杂的逻辑）
UPDATE `ums_user_properties` 
SET `data_type` = 7 
WHERE `value` IS NOT NULL AND LENGTH(`value`) > 0;

-- 如果value为空，设置为unknown
UPDATE `ums_user_properties` 
SET `data_type` = 10 
WHERE `value` IS NULL OR LENGTH(`value`) = 0; 