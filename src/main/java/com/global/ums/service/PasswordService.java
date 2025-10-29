package com.global.ums.service;

/**
 * 密码服务接口
 */
public interface PasswordService {
    
    /**
     * 设置用户密码
     *
     * @param userId 用户ID
     * @param password 原始密码
     * @return 是否成功
     */
    boolean setPassword(Long userId, String password);

    byte[] getPassword(String password);

    /**
     * 验证用户密码
     *
     * @param userId 用户ID
     * @param password 原始密码
     * @return 是否匹配
     */
    boolean verifyPassword(Long userId, String password);
    
    /**
     * 修改用户密码
     *
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
} 