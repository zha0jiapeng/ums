package com.global.ums.enums;

/**
 * 用户类型枚举
 * 用于标识用户的注册来源和类型
 */
public enum UserType {

    /**
     * 普通用户
     */
    USER(1, "user", "普通用户"),

    /**
     * 超级管理员
     */
    ADMIN(2, "admin", "超级管理员"),

    /**
     * 用户组
     */
    APPLICATION(3, "application", "应用"),

    DEPT(4, "dept", "部门"),



    /**
     * 未知类型
     */
    UNKNOWN(0, "unknown", "未知类型");

    private final int value;
    private final String code;
    private final String description;

    UserType(int value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据数值获取用户类型
     */
    public static UserType fromValue(int value) {
        for (UserType userType : values()) {
            if (userType.value == value) {
                return userType;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据代码获取用户类型
     */
    public static UserType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }

        for (UserType userType : values()) {
            if (userType.code.equalsIgnoreCase(code)) {
                return userType;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据描述获取用户类型
     */
    public static UserType fromDescription(String description) {
        if (description == null) {
            return UNKNOWN;
        }

        for (UserType userType : values()) {
            if (userType.description.equals(description)) {
                return userType;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return code;
    }
}
