package com.global.ums.controller.auth;

import com.global.ums.dto.LoginDTO;
import com.global.ums.dto.TokenDTO;
import com.global.ums.dto.UserInfoTreeDTO;
import com.global.ums.entity.User;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginDTO loginDTO) {
        AjaxResult result = authService.login(loginDTO);
        return result;
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public AjaxResult getUserInfo(@RequestHeader(value = "${jwt.token-header:Authorization}", required = false) String token) {
        User user = authService.getUserByToken(token);
        if (user != null) {
            return AjaxResult.success(user);
        } else {
            return AjaxResult.errorI18n(401, "auth.token.invalid");
        }
    }

    /**
     * 获取当前登录用户的树状信息
     */
    @GetMapping("/info/tree")
    public AjaxResult getUserInfoTree(@RequestHeader(value = "${jwt.token-header:Authorization}", required = false) String token) {
        UserInfoTreeDTO tree = authService.getUserInfoTreeByToken(token);
        if (tree == null) {
            return AjaxResult.errorI18n(401, "auth.token.invalid");
        }
        return AjaxResult.success(tree);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public AjaxResult logout(@RequestHeader(value = "${jwt.token-header:Authorization}", required = false) String token) {
        boolean result = authService.logout(token);
        if (result) {
            return AjaxResult.successI18n("auth.logout.success");
        } else {
            return AjaxResult.errorI18n("auth.logout.error");
        }
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public AjaxResult refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        TokenDTO tokenDTO = authService.refreshToken(refreshToken);
        if (tokenDTO == null) {
            return AjaxResult.errorI18n("auth.refresh.token.invalid");
        }
        return AjaxResult.success(tokenDTO);
    }

    private UserInfoTreeDTO filterTreeByType(UserInfoTreeDTO node, Integer filterType) {
        if (node == null || filterType == null) {
            return node;
        }
        node.setParents(collectMatchingNodes(node.getParents(), filterType));
        return node;
    }

    private List<UserInfoTreeDTO> collectMatchingNodes(List<UserInfoTreeDTO> parents, Integer filterType) {
        List<UserInfoTreeDTO> result = new ArrayList<>();
        if (parents == null || parents.isEmpty()) {
            return result;
        }
        for (UserInfoTreeDTO parent : parents) {
            if (parent == null) {
                continue;
            }
            List<UserInfoTreeDTO> childMatches = collectMatchingNodes(parent.getParents(), filterType);
            boolean matches = parent.getType() != null && parent.getType().equals(filterType);
            if (matches) {
                parent.setParents(childMatches);
                result.add(parent);
            } else {
                result.addAll(childMatches);
            }
        }
        return result;
    }
} 
