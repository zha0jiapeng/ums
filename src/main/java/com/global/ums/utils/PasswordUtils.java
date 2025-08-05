package com.global.ums.utils;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码工具类
 */
public class PasswordUtils {

    /**
     * 盐的长度
     */
    private static final int SALT_LENGTH = 16;

    /**
     * 迭代次数
     */
    private static final int ITERATIONS = 1000;

    /**
     * 生成随机盐
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 对密码进行加密
     * 
     * @param password 原始密码
     * @param salt 盐值
     * @return 加密后的密码
     */
    public static String encryptPassword(String password, String salt) {
        String passwordWithSalt = password + salt;
        String hash = passwordWithSalt;
        
        // 多次迭代加密
        for (int i = 0; i < ITERATIONS; i++) {
            hash = DigestUtils.md5DigestAsHex(hash.getBytes(StandardCharsets.UTF_8));
        }
        
        return hash;
    }

    /**
     * 验证密码是否正确
     * 
     * @param password 输入的密码
     * @param salt 盐值
     * @param encryptedPassword 已加密的密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String password, String salt, String encryptedPassword) {
        String newEncryptedPassword = encryptPassword(password, salt);
        return newEncryptedPassword.equals(encryptedPassword);
    }
    
    /**
     * 将密码和盐组合成存储格式
     * 
     * @param encryptedPassword 加密后的密码
     * @param salt 盐值
     * @return 存储格式的密码
     */
    public static String formatPassword(String encryptedPassword, String salt) {
        return encryptedPassword + ":" + salt;
    }
    
    /**
     * 将存储格式的密码解析为加密密码和盐
     * 
     * @param storedPassword 存储格式的密码
     * @return 数组，第一个元素是加密密码，第二个元素是盐
     */
    public static String[] parsePassword(String storedPassword) {
        return storedPassword.split(":");
    }
    
    /**
     * 将存储格式的密码转换为字节数组
     * 
     * @param encryptedPassword 加密后的密码
     * @param salt 盐值
     * @return 字节数组
     */
    public static byte[] toBytes(String encryptedPassword, String salt) {
        String formatted = formatPassword(encryptedPassword, salt);
        return formatted.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * 从字节数组解析存储格式的密码
     * 
     * @param bytes 字节数组
     * @return 数组，第一个元素是加密密码，第二个元素是盐
     */
    public static String[] fromBytes(byte[] bytes) {
        String storedPassword = new String(bytes, StandardCharsets.UTF_8);
        return parsePassword(storedPassword);
    }
} 