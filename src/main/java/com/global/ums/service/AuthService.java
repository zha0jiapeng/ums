package com.global.ums.service;

import com.global.ums.dto.LoginDTO;
import com.global.ums.dto.TokenDTO;
import com.global.ums.dto.UserInfoTreeDTO;
import com.global.ums.entity.User;
import com.global.ums.result.AjaxResult;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 令牌信息
     */
    AjaxResult login(LoginDTO loginDTO);
    
    /**
     * 通过token获取用户信息
     *
     * @param token 令牌
     * @return 用户信息
     */
    User getUserByToken(String token);

    /**
     * 通过token获取用户树状结构信息
     *
     * @param token 令牌
     * @return 用户树
     */
    UserInfoTreeDTO getUserInfoTreeByToken(String token);
    
    /**
     * 用户登出
     *
     * @param token 令牌
     * @return 是否成功
     */
    boolean logout(String token);
    
    /**
     * 刷新令牌
     *
     * @param oldToken 旧令牌
     * @return 新令牌信息
     */
    TokenDTO refreshToken(String oldToken);
} 
