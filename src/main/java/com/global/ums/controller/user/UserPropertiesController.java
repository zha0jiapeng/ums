package com.global.ums.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.global.ums.annotation.BrotliCompress;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.UserProperties;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.utils.DataTypeUtils;
import com.global.ums.utils.KeyValidationUtils;
import com.global.ums.utils.LoginUserContextHolder;
import com.global.ums.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * 用户属性控制器
 */
@RestController
@RequestMapping("/user/properties")
@RequireAuth
public class UserPropertiesController {
    
    @Autowired
    private UserPropertiesService userPropertiesService;
    
    /**
     * 添加/更新用户属性（文件上传）
     */
    @PostMapping("/update")
    public AjaxResult update(@RequestParam("key") String key, @RequestParam(value = "data",required = false) MultipartFile data) throws IOException {
        Long userId = LoginUserContextHolder.getUserId();
        if(StringUtils.isEmpty(key)){
            return AjaxResult.error(400,"入参有误");
        }
        byte[] bytes = new byte[0];
        Long dataSize = 0l ;
        if(data != null){
            dataSize = data.getSize();
            bytes = data.getBytes();
        }
        // 验证key是否被允许
        KeyValidationUtils.ValidationResult validationResult = KeyValidationUtils.validateKey(key, dataSize);
        if (!validationResult.isValid()) {
            return AjaxResult.error(400, validationResult.getErrorMessage());
        }
        if(data == null){
            userPropertiesService.remove(
                    new LambdaQueryWrapper<UserProperties>()
                            .eq(UserProperties::getUserId, userId)
                            .eq(UserProperties::getKey, key)
            );
            return AjaxResult.successI18n("user.properties.delete.success");
        }
        
        // 获取key配置
        KeyValidationUtils.KeyConfig keyConfig = KeyValidationUtils.getKeyConfig(key);
        UserProperties userProperties = new UserProperties();
        userProperties.setUserId(userId);
        userProperties.setKey(key);
        userProperties.setValue(bytes);
        // 自动推断数据类型
        userProperties.setDataType(DataTypeUtils.inferDataType(bytes).getValue());
        userProperties.setScope(keyConfig.getScope()); // 使用配置文件中的scope
        
        return userPropertiesService.saveUserProperties(userProperties);
    }



    @DeleteMapping("/delete/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        boolean result = userPropertiesService.removeById(id);
        if (result) {
            return AjaxResult.successI18n("user.properties.delete.success");
        } else {
            return AjaxResult.errorI18n("user.properties.delete.error");
        }
    }
    
    /**
     * 根据用户ID和键获取属性
     */
    @GetMapping("/get")
    @BrotliCompress(quality = 4, threshold = 512)
    public AjaxResult getByUserIdAndKey(@RequestParam String key) {
        Long userId = LoginUserContextHolder.getUserId();
        UserProperties userProperties = userPropertiesService.getByUserIdAndKey(userId, key);

        if (userProperties != null) {
            return AjaxResult.success(userProperties);
        } else {
            return AjaxResult.errorI18n("user.properties.not.found");
        }
    }

    /**
     * 根据用户ID和键获取属性
     */
    @GetMapping("/getReturnByte")
    @BrotliCompress(quality = 4, threshold = 512)
    public void getByUserIdAndKeyReturnByte(@RequestParam String key,HttpServletResponse response) throws IOException {
        Long userId = LoginUserContextHolder.getUserId();
        UserProperties userProperties = userPropertiesService.getByUserIdAndKey(userId, key);
        if (userProperties != null && userProperties.getValue() != null) {
            byte[] value = userProperties.getValue();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;");
            response.setContentLength(value.length);
             // 写入响应流
            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(value);
                outputStream.flush();
            }
        }else {
            // 设置错误状态码
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            // 返回JSON格式的错误信息
            response.getWriter().write("{\"code\":404,\"msg\":\"未找到用户属性或属性值为空\",\"data\":null}");
        }
    }

    /**
     * 根据用户ID和键获取属性
     */
    @GetMapping("/getPropertiesByUserId")
    @BrotliCompress(quality = 4, threshold = 512)
    public AjaxResult getByUserIdAndKey() {
        Long userId = LoginUserContextHolder.getUserId();
        System.out.println(userId);
        List<UserProperties> list = userPropertiesService.list(new LambdaQueryWrapper<UserProperties>()
                .eq(UserProperties::getUserId, userId));
        return AjaxResult.success(list);
    }
} 