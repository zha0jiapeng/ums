package com.global.ums.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.global.ums.entity.UserProperties;
import lombok.Data;

import java.util.List;

/**
 * 用户信息树DTO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoTreeDTO {

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
     * 当前节点的属性列表
     */
    private List<UserProperties> properties;

    /**
     * 父节点信息
     */
    private List<UserInfoTreeDTO> parents;
}
