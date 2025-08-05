package com.global.ums.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录成功后返回的Token信息
 */
@Data
@NoArgsConstructor
public class TokenDTO implements Serializable {
    
    /**
     * Token
     */
    private String accessToken;


    private String refreshToken;
    
    /**
     * Token前缀
     */
    private String tokenPrefix;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户类型
     */
    private Integer userType;
    
    /**
     * 用户名
     */
    private String username;

    public TokenDTO(String accessToken, String refreshToken, String tokenPrefix, Long userId, Integer userType, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenPrefix = tokenPrefix;
        this.userId = userId;
        this.userType = userType;
        this.username = username;
    }
}