package com.global.ums.constant;

/**
 * 用户类型常量
 */
public class UserTypeConstant {
    
    /**
     * 微信注册
     */
    public static final int WECHAT = 1;
    
    /**
     * 手机注册
     */
    public static final int PHONE = 2;
    
    /**
     * 邮箱注册
     */
    public static final int EMAIL = 3;
    
    /**
     * 用户组
     */
    public static final int USER_GROUP = 4;

    /**
     * 超级管理员
     */
    public static final int ADMIN = 5;
    
    /**
     * 获取用户类型描述
     *
     * @param type 用户类型编码
     * @return 用户类型描述
     */
    public static String getTypeDesc(Integer type) {
        if (type == null) {
            return "未知";
        }
        
        switch (type) {
            case WECHAT:
                return "微信注册";
            case PHONE:
                return "手机注册";
            case EMAIL:
                return "邮箱注册";
            case USER_GROUP:
                return "用户组";
            case ADMIN:
                return "超级管理员";
            default:
                return "未知";
        }
    }
} 