package com.global.ums.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 用户属性实体类
 */
@Data
@TableName("ums_user_properties")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProperties {

    /**
     * 属性id
     */
    @TableId(type = IdType.AUTO)
    @JsonIgnore
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 属性键
     */
    @TableField("`key`")
    private String key;

    /**
     * 属性值
     */
    private byte[] value;

    /**
     * 数据类型（从 ums_property_keys 表查询获得，非数据库字段）
     */
    @TableField(exist = false)
    private Integer dataType;

    /**
     * 是否隐藏（从 ums_property_keys 表查询获得，非数据库字段）
     */
    @TableField(exist = false)
    private Integer hidden;

    /**
     * 属性范围（从 ums_property_keys 表查询获得，非数据库字段）
     */
    @TableField(exist = false)
    private Integer scope;

    /**
     * 属性描述（从 ums_property_keys 表查询获得，非数据库字段）
     */
    @TableField(exist = false)
    private String description;

    /**
     * 所有父级用户的同key属性列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<UserProperties> parentProperties;

}
