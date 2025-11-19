package com.global.ums.enums;

/**
 * ums_template 节点类型枚举
 */
public enum TemplateType {

    APPLICATION(1, "application", "应用"),
    DEPARTMENT(2, "department", "部门"),
    UNKNOWN(0, "unknown", "未知");

    private final int value;
    private final String code;
    private final String description;

    TemplateType(int value, String code, String description) {
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

    public static TemplateType fromValue(Integer value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (TemplateType templateType : values()) {
            if (templateType.value == value) {
                return templateType;
            }
        }
        return UNKNOWN;
    }

    public boolean isValid() {
        return this != UNKNOWN;
    }
}
