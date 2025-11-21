-- 用户类型调整 SQL
-- 执行前请先备份数据库

-- 新的用户类型定义：
-- 1: 普通用户
-- 2: 用户组（原来的 ADMIN、APPLICATION、DEPT 统一为用户组，通过 group_type 属性区分）
-- 0: 未知类型

-- group_type 属性值定义：
-- 1: 部门
-- 2: 应用

-- 1. 查看当前的用户类型分布
SELECT type, COUNT(*) as count
FROM ums_user
GROUP BY type
ORDER BY type;

-- 2. 将原来的 ADMIN(2)、APPLICATION(3)、DEPT(4) 统一改为 USER_GROUP(2)
UPDATE ums_user
SET type = 2
WHERE type IN (2, 3, 4);

-- 3. 为原来的 APPLICATION 和 DEPT 类型用户添加 group_type 属性
-- 注意：你需要根据原来的 type 值来设置 group_type
-- 这里提供示例 SQL，需要根据实际业务逻辑调整

-- 示例：如果你有办法识别哪些是部门（原 DEPT 类型或其他标识）
-- 为部门用户添加 group_type=1 属性
INSERT INTO ums_user_properties (`user_id`, `key`, `value`)
SELECT u.id, 'group_type', '1'
FROM ums_user u
WHERE u.type = 2
AND NOT EXISTS (
    SELECT 1 FROM ums_user_properties up
    WHERE up.user_id = u.id AND up.`key` = 'group_type'
)
-- 这里添加识别部门的条件，例如：
-- AND u.unique_id LIKE 'dept-%'
-- 或者手动指定部门用户的 ID
-- AND u.id IN (1, 2, 3, ...)
;

-- 示例：如果你有办法识别哪些是应用（原 APPLICATION 类型或其他标识）
-- 为应用用户添加 group_type=2 属性
INSERT INTO ums_user_properties (`user_id`, `key`, `value`)
SELECT u.id, 'group_type', '2'
FROM ums_user u
WHERE u.type = 2
AND NOT EXISTS (
    SELECT 1 FROM ums_user_properties up
    WHERE up.user_id = u.id AND up.`key` = 'group_type'
)
-- 这里添加识别应用的条件，例如：
-- AND u.unique_id LIKE 'app-%'
-- 或者手动指定应用用户的 ID
-- AND u.id IN (10, 11, 12, ...)
;

-- 4. 确保 group_type 属性键在 ums_property_keys 表中存在
INSERT INTO ums_property_keys (`key`, `data_type`, `description`, `size`, `scope`, `hidden`)
VALUES ('group_type', 1, '用户组类型（1:部门 2:应用）', 10, 0, 0)
ON DUPLICATE KEY UPDATE description = '用户组类型（1:部门 2:应用）';

-- 5. 验证调整后的数据
SELECT
    u.id,
    u.unique_id,
    u.type,
    CASE
        WHEN u.type = 1 THEN '普通用户'
        WHEN u.type = 2 THEN '用户组'
        ELSE '未知类型'
    END as type_desc,
    (SELECT CAST(up.value AS CHAR) FROM ums_user_properties up WHERE up.user_id = u.id AND up.`key` = 'group_type' LIMIT 1) as group_type,
    CASE
        WHEN (SELECT up.value FROM ums_user_properties up WHERE up.user_id = u.id AND up.`key` = 'group_type' LIMIT 1) = '1' THEN '部门'
        WHEN (SELECT up.value FROM ums_user_properties up WHERE up.user_id = u.id AND up.`key` = 'group_type' LIMIT 1) = '2' THEN '应用'
        ELSE NULL
    END as group_type_desc
FROM ums_user u
ORDER BY u.type, u.id;

-- 注意事项：
-- 1. 用户类型现在只有两种：1(普通用户) 和 2(用户组)
-- 2. 用户组通过 group_type 属性来区分：1(部门) 2(应用)
-- 3. 新建部门时，需要自动添加 group_type=1 的属性
-- 4. 新建应用时，需要自动添加 group_type=2 的属性
-- 5. 前端可以通过 /user/page?type=2&groupType=1 查询所有部门
-- 6. 前端可以通过 /user/page?type=2&groupType=2 查询所有应用
