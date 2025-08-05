package com.global.ums.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.global.ums.enums.DataType;
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
     * 数据类型
     */
    private Integer dataType;

    private Integer hidden;
    
    /**
     * 属性范围
     */
    private Integer scope;
    
    /**
     * 所有父级用户的同key属性列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<UserProperties> parentProperties;
} 