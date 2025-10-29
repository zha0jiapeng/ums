package com.global.ums.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.global.ums.annotation.BrotliCompress;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.dto.PropertyTreeDTO;
import com.global.ums.entity.User;
import com.global.ums.enums.UserType;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UserService;
import com.global.ums.utils.LoginUserContextHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@Api(tags = "用户管理")
@RestController
@RequestMapping("/user")
@RequireAuth
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 添加用户
     */
    @ApiOperation(value = "添加用户", notes = "创建新用户")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody User user) {
        return userService.addUser(user);
    }

    /**
     * 删除用户
     */
    @ApiOperation(value = "删除用户", notes = "根据ID删除用户，会检查用户组引用并级联删除用户属性")
    @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataTypeClass = Long.class, paramType = "path")
    @DeleteMapping("/delete/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        return userService.deleteUser(id);
    }

    /**
     * 更新用户
     */
    @ApiOperation(value = "更新用户", notes = "更新用户信息")
    @PutMapping("/update")
    public AjaxResult update(@RequestBody User user) {
        boolean result = userService.updateById(user);
        if (result) {
            return AjaxResult.successI18n("user.update.success");
        } else {
            return AjaxResult.errorI18n("user.update.error");
        }
    }

    /**
     * 获取用户详情
     */
    @ApiOperation(value = "获取当前用户信息", notes = "获取当前登录用户的详细信息，包含所有用户属性及其数据类型信息")
    @GetMapping("/info")
    public AjaxResult info() {
        Long userId = LoginUserContextHolder.getUserId();
        User user = userService.getUserWithProperties(userId);
        if (user != null) {
            return AjaxResult.success(user);
        } else {
            return AjaxResult.errorI18n("user.not.found");
        }
    }

    /**
     * 分页查询用户 - 使用高压缩等级
     */
    @ApiOperation(value = "分页查询用户", notes = "支持按用户类型、唯一标识、类别、属性类型、上级用户查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码", defaultValue = "1", dataTypeClass = Integer.class, paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量", defaultValue = "10", dataTypeClass = Integer.class, paramType = "query"),
            @ApiImplicitParam(name = "type", value = "用户类型", dataTypeClass = Integer.class, paramType = "query"),
            @ApiImplicitParam(name = "uniqueId", value = "唯一标识（支持模糊查询）", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "category", value = "类别（用户属性category）", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "propertyType", value = "属性类型（用户属性type）", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "parentId", value = "上级用户ID", dataTypeClass = Long.class, paramType = "query")
    })
    @GetMapping("/page")
    public AjaxResult page(@RequestParam(defaultValue = "1") Integer pageNum,
                        @RequestParam(defaultValue = "10") Integer pageSize,
                        @RequestParam(required = false) Integer type,
                        @RequestParam(required = false) String uniqueId,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String propertyType,
                        @RequestParam(required = false) Long parentId) {
        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> userPage = userService.getUserPage(page, type, uniqueId, category, propertyType, parentId);
        return AjaxResult.success(userPage);
    }

    /**
     * 获取所有用户类型枚举
     */
    @ApiOperation(value = "获取用户类型枚举", notes = "获取所有可用的用户类型枚举值，用于前端选择器等场景")
    @GetMapping("/user-types")
    public AjaxResult getUserTypes() {
        try {
            List<Map<String, Object>> userTypes = new ArrayList<>();

            for (UserType userType : UserType.values()) {
                // 跳过 UNKNOWN 类型
                if (userType == UserType.UNKNOWN) {
                    continue;
                }

                Map<String, Object> item = new HashMap<>();
                item.put("value", userType.getValue());
                item.put("code", userType.getCode());
                item.put("description", userType.getDescription());
                userTypes.add(item);
            }

            return AjaxResult.success(userTypes);
        } catch (Exception e) {
            return AjaxResult.error("获取用户类型失败: " + e.getMessage());
        }
    }

    /**
     * 用户属性树状结构
     */
    @GetMapping("/getTree")
    @BrotliCompress(quality = 4, threshold = 512)
    public AjaxResult getTree(String category) {
        Long userId = LoginUserContextHolder.getUserId();
        Integer userType = LoginUserContextHolder.getUserType();

        // 超级管理员(type=2)：获取全部的树状结构
        if (userType != null && userType == 2) {
            List<PropertyTreeDTO> tree = userService.getTree(userId,category);
            if (tree != null && !tree.isEmpty()) {
                return AjaxResult.success(tree);
            } else {
                return AjaxResult.errorI18n("user.properties.not.found");
            }
        }

        // 普通用户(type=1)：待定
        if (userType != null && userType == 1) {
            // TODO: 普通用户的逻辑待定
            return AjaxResult.error(403, "功能开发中");
        }

        // 其他用户类型：无权限
        return AjaxResult.error(403, "无权限访问");
    }
} 