package com.global.ums.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 属性键配置实体类
 */
@Data
@TableName("ums_property_keys")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "属性键配置")
public class PropertyKeys {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 属性键名
     */
    @TableField("`key`")
    @ApiModelProperty(value = "属性键名", example = "username")
    private String key;

    /**
     * 数据类型
     */
    @ApiModelProperty(value = "数据类型值", example = "0")
    private Integer dataType;

    /**
     * 中文描述
     */
    @ApiModelProperty(value = "属性描述", example = "用户名")
    private String description;

    /**
     * 最大尺寸（字节）
     */
    @ApiModelProperty(value = "最大尺寸（字节）", example = "255")
    private Long size;

    /**
     * 范围
     */
    @ApiModelProperty(value = "作用域（0:用户级 1:系统级）", example = "0")
    private Integer scope;

    /**
     * 是否隐藏
     */
    @ApiModelProperty(value = "是否隐藏（0:否 1:是）", example = "0")
    private Integer hidden;

    /**
     * 是否覆盖父集属性
     */
    @ApiModelProperty(value = "是否覆盖父集属性（0:继承父集 1:只用当前用户的，忽略父集）", example = "0")
    private Integer overrideParent;
}
