package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.global.ums.constant.UserPropertiesConstant;
import com.global.ums.dto.LoginDTO;
import com.global.ums.dto.TokenDTO;
import com.global.ums.entity.User;
import com.global.ums.entity.UserProperties;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.AuthService;
import com.global.ums.service.PasswordService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.service.UserService;
import com.global.ums.utils.JwtUtils;
import com.global.ums.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;


/**
 * 认证服务实现
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserPropertiesService userPropertiesService;
    
    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.token-prefix:Bearer}")
    private String tokenPrefix;

    @Autowired
    private RedisCache redisCache;

    public final String CAPTCHA_CODE_KEY = "captcha_codes:";
    /**
     * 用户登录
     */
    @Override
    public AjaxResult login(LoginDTO loginDTO) {
        String uuid = loginDTO.getUuid();
        String verifyKey = CAPTCHA_CODE_KEY + com.global.ums.utils.StringUtils.nvl(uuid, "");
        String result = redisCache.getCacheObject(verifyKey);
        if(!loginDTO.getCode().equals(result)){
            //验证码错误
            return AjaxResult.errorI18n("auth.captcha.error");
        }

        String username = loginDTO.getUsername();
        Long userId = null;
        List<UserProperties> list = userPropertiesService.list(
                new LambdaQueryWrapper<UserProperties>().eq(UserProperties::getKey, UserPropertiesConstant.KEY_USERNAME));
        for (UserProperties userProperties : list) {
            String usernameItem = new String(userProperties.getValue());
            if(usernameItem.equals(username)){
                userId =  userProperties.getUserId();
            }
        }
        if (userId == null) {
            //用户不存在
            return AjaxResult.errorI18n("auth.user.not.exists");
        }
        // 查询所有用户
        User user = userService.getById(userId);

        // 用户不存在
        if (user == null) {
            //用户不存在
            return AjaxResult.errorI18n("auth.user.not.exists");
        }
        
        // 验证密码
        if (!passwordService.verifyPassword(user.getId(), loginDTO.getPassword())) {
            //密码错误
            return AjaxResult.errorI18n("auth.password.error");
        }
        TokenDTO tokenDTO = jwtUtils.generateToken(user.getId(), user.getType(), username);
        // 生成token
        return AjaxResult.success(tokenDTO);
    }

    /**
     * 通过token获取用户信息
     */
    @Override
    public User getUserByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        
        // 去除token前缀
        if (token.startsWith(tokenPrefix + " ")) {
            token = token.substring((tokenPrefix + " ").length());
        }
        
        // 验证token是否有效
        if (!jwtUtils.validateToken(token)) {
            return null;
        }
        
        // 从token中获取用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            return null;
        }
        
        // 获取用户信息
        return userService.getUserWithProperties(userId);
    }

    /**
     * 用户登出
     */
    @Override
    public boolean logout(String token) {
        // 由于JWT是无状态的，客户端只需要删除token即可
        // 如果需要服务端控制，需要将token加入黑名单
        return true;
    }

    /**
     * 刷新令牌
     */
    @Override
    public TokenDTO refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return null;
        }
        
        // 去除token前缀
        if (refreshToken.startsWith(tokenPrefix + " ")) {
            refreshToken = refreshToken.substring((tokenPrefix + " ").length());
        }
        
        // 判断是否为有效的Token（允许refreshToken或accessToken）
        if (!jwtUtils.validateToken(refreshToken) || 
            (!jwtUtils.isRefreshToken(refreshToken) && !jwtUtils.isAccessToken(refreshToken))) {
            return null;
        }
        // 从Token中获取用户信息
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        User user = userService.getUserWithProperties(userId);
        if (user == null) {
            return null;
        }

        // 生成新的Token
        return jwtUtils.generateToken(user.getId(), user.getType(), user.getUsernameFromProperties());
    }
} 