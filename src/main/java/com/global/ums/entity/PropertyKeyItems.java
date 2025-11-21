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
 * 属性键枚举项实体类
 */
@Data
@TableName("ums_property_key_items")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "属性键枚举项")
public class PropertyKeyItems {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 关联的属性键
     */
    @TableField("`key`")
    @ApiModelProperty(value = "关联的属性键", example = "pd-expert-privileges")
    private String key;

    /**
     * 枚举值
     */
    @ApiModelProperty(value = "枚举值", example = "admin")
    private String itemValue;

    /**
     * 枚举值显示标签
     */
    @ApiModelProperty(value = "枚举值显示标签", example = "管理员权限")
    private String itemLabel;

    /**
     * 优先级（数字越小优先级越高）
     */
    @ApiModelProperty(value = "优先级（数字越小优先级越高）", example = "1")
    private Integer priority;
}
