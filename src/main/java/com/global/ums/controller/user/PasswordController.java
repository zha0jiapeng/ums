package com.global.ums.controller.user;

import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.User;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 密码控制器
 */
@RestController
@RequestMapping("/password")
@RequireAuth
public class PasswordController {
    
    @Autowired
    private PasswordService passwordService;
    
    /**
     * 修改密码
     */
    @PostMapping("/change")
    public AjaxResult changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            HttpServletRequest request) {
        
        // 从请求中获取当前用户
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return AjaxResult.error(401, "未登录或登录已过期");
        }
        
        // 修改密码
        boolean result = passwordService.changePassword(currentUser.getId(), oldPassword, newPassword);
        if (result) {
            return AjaxResult.successI18n("password.change.success");
        } else {
            return AjaxResult.errorI18n("password.change.error");
        }
    }
    
    /**
     * 重置密码（管理员操作）
     */
    @PostMapping("/reset/{userId}")
    public AjaxResult resetPassword(
            @PathVariable Long userId,
            @RequestParam String newPassword) {
        
        // 重置密码
        boolean result = passwordService.setPassword(userId, newPassword);
        if (result) {
            return AjaxResult.successI18n("password.reset.success");
        } else {
            return AjaxResult.errorI18n("password.reset.error");
        }
    }
} 