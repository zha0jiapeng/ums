package com.global.ums.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {

    /**
     * 第一层
     */
    FIRST(1, "first", "第一层",0,"category,type,name"),

    /**
     * 第二层
     */
    SECOND(2, "second", "第二层",1,"category,type,name"),

    /**
     * 第三层
     */
    THIRD(3, "third", "第三层",2,"category,type,name");

    private final int value;
    private final String code;
    private final String description;
    private final int parent;
    private final String initKeys;

    RoleEnum(int value, String code, String description, int parent, String initKeys) {
        this.value = value;
        this.code = code;
        this.description = description;
        this.parent = parent;
        this.initKeys = initKeys;
    }
}
