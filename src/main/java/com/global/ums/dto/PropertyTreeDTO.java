package com.global.ums.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户属性树状结构DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyTreeDTO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 属性键
     */
    private String key;
    
    /**
     * 属性值（字符串表示）
     */
    private String value;
    
    /**
     * 数据类型
     */
    private Integer dataType;
    
    /**
     * 属性描述
     */
    private String description;
    
    /**
     * 子属性列表
     */
    private List<PropertyTreeDTO> children;
}
