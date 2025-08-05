package com.global.ums.enums;

/**
 * 用户属性数据类型枚举
 * 用于标识user_properties表中value字段的数据类型
 */
public enum DataType {
    
    /**
     * 字符串类型
     */
    STRING(0, "string", "字符串"),
    
    /**
     * 数字类型（整数）
     */
    INTEGER(1, "integer", "整数"),
    
    /**
     * 数字类型（单精度浮点数）
     */
    FLOAT(2, "float", "单精度浮点数"),
    
    /**
     * 数字类型（双精度浮点数）
     */
    DOUBLE(3, "double", "双精度浮点数"),
    
    /**
     * 数字类型（长整数）
     */
    LONG(4, "long", "长整数"),
    
    /**
     * 布尔类型
     */
    BOOLEAN(5, "boolean", "布尔值"),
    
    /**
     * JSON对象类型
     */
    JSON(5, "json", "JSON对象"),
    
    /**
     * 二进制数据（图片、文件等）
     */
    BINARY(6, "binary", "二进制数据"),
    
    /**
     * 日期时间类型
     */
    DATETIME(7, "datetime", "日期时间"),
    
    /**
     * 数组类型
     */
    ARRAY(8, "array", "数组"),
    
    /**
     * 未知类型
     */
    UNKNOWN(9, "unknown", "未知类型");

    private final int value;
    private final String code;
    private final String description;

    DataType(int value, String code, String description) {
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
     * 根据数值获取数据类型
     */
    public static DataType fromValue(int value) {
        for (DataType dataType : values()) {
            if (dataType.value == value) {
                return dataType;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据代码获取数据类型
     */
    public static DataType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        
        for (DataType dataType : values()) {
            if (dataType.code.equalsIgnoreCase(code)) {
                return dataType;
            }
        }
        return UNKNOWN;
    }

    /**
     * 根据描述获取数据类型
     */
    public static DataType fromDescription(String description) {
        if (description == null) {
            return UNKNOWN;
        }
        
        for (DataType dataType : values()) {
            if (dataType.description.equals(description)) {
                return dataType;
            }
        }
        return UNKNOWN;
    }

    /**
     * 判断是否为文本类型（可转换为字符串）
     */
    public boolean isTextType() {
        return this == STRING || this == JSON || this == DATETIME || this == ARRAY;
    }

    /**
     * 判断是否为数字类型
     */
    public boolean isNumericType() {
        return this == INTEGER || this == FLOAT || this == DOUBLE || this == LONG;
    }

    /**
     * 判断是否为二进制类型
     */
    public boolean isBinaryType() {
        return this == BINARY;
    }

    @Override
    public String toString() {
        return code;
    }
} 