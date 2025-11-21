package com.global.ums.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.global.ums.entity.PropertyKeyItems;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 属性键配置VO（包含枚举项）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "属性键配置（包含枚举项）")
public class PropertyKeysVO {

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 属性键名
     */
    @ApiModelProperty(value = "属性键名", example = "pd-expert-privileges")
    private String key;

    /**
     * 数据类型
     */
    @ApiModelProperty(value = "数据类型值", example = "0")
    private Integer dataType;

    /**
     * 中文描述
     */
    @ApiModelProperty(value = "属性描述", example = "PD专家权限")
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
     * 枚举项列表
     */
    @ApiModelProperty(value = "枚举项列表")
    private List<PropertyKeyItems> items;
}
