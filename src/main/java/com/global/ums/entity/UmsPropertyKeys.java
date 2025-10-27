package com.global.ums.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 属性键配置实体类
 */
@Data
@TableName("ums_property_keys")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UmsPropertyKeys {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 属性键名
     */
    @TableField("`key`")
    private String key;

    /**
     * 数据类型
     */
    private Integer dataType;

    /**
     * 中文描述
     */
    private String description;

    /**
     * 最大尺寸（字节）
     */
    private Long size;

    /**
     * 范围
     */
    private Integer scope;
}
