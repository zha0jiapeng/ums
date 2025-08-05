package com.global.ums.utils;

import com.global.ums.constant.UserTypeConstant;

/**
 * 用户类型工具类
 */
public class UserTypeUtils {
    
    /**
     * 根据类型编码获取类型描述
     *
     * @param typeCode 类型编码
     * @return 类型描述
     */
    public static String getTypeDesc(Integer typeCode) {
        return UserTypeConstant.getTypeDesc(typeCode);
    }
    
    /**
     * 判断是否为微信注册类型
     *
     * @param typeCode 类型编码
     * @return 是否为微信注册类型
     */
    public static boolean isWechatType(Integer typeCode) {
        return typeCode != null && typeCode == UserTypeConstant.WECHAT;
    }
    
    /**
     * 判断是否为手机注册类型
     *
     * @param typeCode 类型编码
     * @return 是否为手机注册类型
     */
    public static boolean isPhoneType(Integer typeCode) {
        return typeCode != null && typeCode == UserTypeConstant.PHONE;
    }
    
    /**
     * 判断是否为邮箱注册类型
     *
     * @param typeCode 类型编码
     * @return 是否为邮箱注册类型
     */
    public static boolean isEmailType(Integer typeCode) {
        return typeCode != null && typeCode == UserTypeConstant.EMAIL;
    }
    
    /**
     * 判断是否为用户组类型
     *
     * @param typeCode 类型编码
     * @return 是否为用户组类型
     */
    public static boolean isUserGroupType(Integer typeCode) {
        return typeCode != null && typeCode == UserTypeConstant.USER_GROUP;
    }
    
    /**
     * 验证类型编码是否有效
     *
     * @param typeCode 类型编码
     * @return 是否有效
     */
    public static boolean isValidType(Integer typeCode) {
        if (typeCode == null) {
            return false;
        }
        
        return typeCode == UserTypeConstant.WECHAT
                || typeCode == UserTypeConstant.PHONE
                || typeCode == UserTypeConstant.EMAIL
                || typeCode == UserTypeConstant.USER_GROUP;
    }
} 