-- 数据迁移脚本：将 key-validation-config.json 中的数据导入到 ums_property_keys 表

-- 插入用户密码配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('password', NULL, '用户密码', 1024, 1);

-- 插入用户名配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('username', NULL, '用户名', 256, 2);

-- 插入用户昵称配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('nickname', NULL, '用户昵称', 512, 2);

-- 插入用户头像配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('avatar', NULL, '用户头像', 5242880, 2);

-- 插入用户邮箱配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('email', NULL, '用户邮箱', 512, 2);

-- 插入手机号配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('phone', NULL, '手机号', 32, 1);

-- 插入手机号（带国家码）配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('phone_number', NULL, '手机号（带国家码）', 64, 1);

-- 插入小程序openid配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('ma_openid', NULL, '小程序openid', 128, 1);

-- 插入微信公众号openid配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('mp_openid', NULL, '微信公众号openid', 128, 1);

-- 插入微信UnionID配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('unionid', NULL, '微信UnionID', 128, 1);

-- 插入创建时间配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('create_time', NULL, '创建时间', 64, 2);

-- 插入PD专家权限配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('pd-expert-privileges', NULL, 'PD专家权限配置', 1048576, 1);

-- 插入用户个人设置配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('user-settings', NULL, '用户个人设置', 524288, 2);

-- 插入设备配置信息配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('device-config', NULL, '设备配置信息', 2097152, 1);

-- 插入报告模板配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('report-template', NULL, '报告模板', 5242880, 2);

-- 插入个人资料图片配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('profile-image', NULL, '个人资料图片', 5242880, 2);

-- 插入数据存储路径配置
INSERT INTO `ums_property_keys` (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('storage', NULL, '数据存储路径', 10240, 1);
