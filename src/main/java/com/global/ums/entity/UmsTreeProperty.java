package com.global.ums.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 树节点属性配置实体
 */
@Data
@TableName("ums_tree_properties")
public class UmsTreeProperty {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 树节点 ID
     */
    private Long treeId;

    /**
     * 属性键
     */
    private String key;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 可选值枚举（逗号分隔）
     */
    @TableField("values")
    private String allowedValues;

    /**
     * 描述
     */
    private String description;

    /**
     * 默认值
     */
    private String defaultValue;
}
