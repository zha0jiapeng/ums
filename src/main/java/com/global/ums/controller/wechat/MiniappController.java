package com.global.ums.controller.wechat;


import cn.hutool.core.util.StrUtil;
import com.global.ums.dto.ConfirmLoginDTO;
import com.global.ums.dto.PhoneNumberDTO;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.IMiniappService;
import com.global.ums.utils.IpUtils;
import com.global.ums.utils.ServletUtils;
import com.global.ums.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 微信小程序 Controller
 */
@Slf4j
@RestController
@RequestMapping("/miniapp")
public class MiniappController{

    @Autowired
    private IMiniappService miniappService;
    /**
     * 生成小程序扫码登录二维码
     */
    @GetMapping("/qrcode/generate")
    public AjaxResult generateQrcode() {
        String userIp = IpUtils.getIpAddr();
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        // 调用服务层方法
        return miniappService.generateLoginQrcode(userIp, userAgent);
    }

    /**
     * 检查小程序扫码登录状态
     */
    @GetMapping("/qrcode/check/{sceneId}")
    public AjaxResult checkQrcodeStatus(
           @PathVariable("sceneId") String sceneId) {
        if (StrUtil.isEmpty(sceneId)) {
            return AjaxResult.errorI18n("common.param.empty");
        }
        
        // 调用服务层方法
        return miniappService.checkQrcodeStatus(sceneId);
    }

    /**
     * 小程序扫码
     */
    @PostMapping("/qrcode/scan/{sceneId}")
    public AjaxResult scanQrcode(
           @PathVariable("sceneId") String sceneId) {
        if (StringUtils.isEmpty(sceneId)) {
            return AjaxResult.errorI18n("common.param.empty");
        }
        
        // 调用服务层方法
        return miniappService.scanQrcode(sceneId);
    }

    /**
     * 确认扫码登录
     */
    @PostMapping("/qrcode/confirm/{sceneId}")
    public AjaxResult confirmQrcode(
           @PathVariable("sceneId") String sceneId) {
        if (StringUtils.isEmpty(sceneId)) {
            return AjaxResult.errorI18n("common.param.empty");
        }
        
        // 调用服务层方法
        return miniappService.confirmQrcode(sceneId);
    }

    /**
     * 取消扫码登录
     */
    @PostMapping("/qrcode/cancel/{sceneId}")
    public AjaxResult cancelQrcode(
           @PathVariable("sceneId") String sceneId) {
        if (StringUtils.isEmpty(sceneId)) {
            return AjaxResult.errorI18n("common.param.empty");
        }
        
        // 调用服务层方法
        return miniappService.cancelQrcode(sceneId);
    }

    /**
     * 根据openId 去看 有没有登录token 有的话直接改成已确认 不需要获取手机号了
     */
    @PostMapping("/qrcode/confirmLoginStatus")
    public AjaxResult getToken(@RequestBody ConfirmLoginDTO confirmLoginDTO) {
        String openId = confirmLoginDTO.getOpenId();
        String sceneId = confirmLoginDTO.getSceneId();
        if (StringUtils.isEmpty(openId)) {
            return AjaxResult.errorI18n("common.param.empty");
        }
        
        // 调用服务层方法
        return miniappService.confirmLoginStatus(openId, sceneId);
    }

    /**
     * 获取手机号API
     */
    @PostMapping("/user/phone")
    public AjaxResult getPhoneNumber(@RequestBody PhoneNumberDTO phoneNumberDTO) {
        String code = phoneNumberDTO.getCode();
        String encryptedData = phoneNumberDTO.getEncryptedData();
        String iv = phoneNumberDTO.getIv();
        String sceneId = phoneNumberDTO.getSceneId();
        
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(encryptedData) || StringUtils.isEmpty(iv)) {
            return AjaxResult.errorI18n("common.param.empty");
        }
        
        // 调用服务层方法
        return miniappService.getPhoneNumber(code, encryptedData, iv, sceneId);
    }

} 