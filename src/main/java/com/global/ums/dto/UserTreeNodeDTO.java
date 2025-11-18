package com.global.ums.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.global.ums.entity.UserProperties;
import lombok.Data;

import java.util.List;

/**
 * 树状用户节点（包含属性和子节点信息）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserTreeNodeDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户唯一标识
     */
    private String uniqueId;

    /**
     * 用户类型
     */
    private Integer type;

    /**
     * 用户类型描述
     */
    private String typeDesc;

    /**
     * 用户属性列表
     */
    private List<UserProperties> properties;

    /**
     * 子用户节点
     */
    private List<UserTreeNodeDTO> children;
}
