-- ======================================
-- 测试属性键配置实时刷新功能
-- ======================================

-- 1. 插入一个新的测试属性键
-- 这个操作模拟直接在数据库中添加配置
INSERT INTO ums_property_keys (
    `key`,
    `scope`,
    `description`,
    `size`,
    `create_time`,
    `update_time`
) VALUES (
    'test-real-time-key',           -- 属性键名称
    0,                               -- 作用域：0=用户级
    '测试实时刷新的属性键',          -- 描述
    10240,                           -- 最大大小：10KB
    NOW(),                           -- 创建时间
    NOW()                            -- 更新时间
);

-- 2. 查询验证是否插入成功
SELECT * FROM ums_property_keys WHERE `key` = 'test-real-time-key';

-- ======================================
-- 测试说明：
-- ======================================
--
-- 执行上述 INSERT 语句后，无需重启应用或调用刷新接口，
-- 新的属性键配置会在以下情况下生效：
--
-- 方式1（立即生效）：
--   当您首次使用该 key 进行验证时，系统会自动从数据库查询并缓存
--
-- 方式2（延迟生效）：
--   最多等待 30 秒（可配置），定时任务会自动刷新所有缓存
--
-- ======================================

-- 3. 测试更新功能
UPDATE ums_property_keys
SET `description` = '更新后的描述-测试实时刷新',
    `size` = 20480,               -- 改为20KB
    `update_time` = NOW()
WHERE `key` = 'test-real-time-key';

-- 4. 测试删除功能
-- DELETE FROM ums_property_keys WHERE `key` = 'test-real-time-key';

-- ======================================
-- 性能优化说明：
-- ======================================
--
-- 系统采用三级缓存策略：
--
-- 1. UmsPropertyKeysService 缓存（ConcurrentHashMap）
--    - 缓存未命中时自动查询数据库并缓存
--
-- 2. KeyValidationUtils 缓存（静态 Map）
--    - 缓存未命中时通过 Service 查询数据库并缓存
--
-- 3. 定时刷新任务（默认30秒）
--    - 定期从数据库全量刷新缓存
--    - 确保即使不使用的 key 也能及时更新
--
-- 配置项调整（application.yml）：
-- property:
--   keys:
--     refresh:
--       interval: 30000  # 刷新间隔（毫秒）
--
-- ======================================
