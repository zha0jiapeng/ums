package com.global.ums.service;

import com.global.ums.entity.QrcodeInfo;
import com.global.ums.result.AjaxResult;

/**
 * 微信小程序服务接口
 */
public interface IMiniappService {

    /**
     * 生成登录二维码
     * 
     * @param userIp 用户IP
     * @param userAgent 用户设备信息
     * @return 结果
     */
    AjaxResult generateLoginQrcode(String userIp, String userAgent);


    /**
     * 处理扫码事件
     * 
     * @param sceneId 场景ID
     * @param status 状态
     * @return 处理结果
     */
    boolean handleScan(String sceneId, String status);
    
    /**
     * 处理扫码事件并返回结果
     * 
     * @param sceneId 场景ID
     * @return 处理结果
     */
    AjaxResult scanQrcode(String sceneId);


    /**
     * 获取二维码详情
     * 
     * @param sceneId 场景ID
     * @return 二维码信息
     */
    QrcodeInfo getQrcodeInfo(String sceneId);
    
    /**
     * 检查小程序扫码登录状态
     * 
     * @param sceneId 场景ID
     * @return 处理结果
     */
    AjaxResult checkQrcodeStatus(String sceneId);

    
    /**
     * 处理用户邀请
     * 
     * @param openId 邀请人的openId
     * @return 处理结果
     */
    //AjaxResult processInvitation(String openId);
    
    /**
     * 获取用户手机号
     *
     * @param code 微信临时登录凭证
     * @param encryptedData 加密数据
     * @param iv 初始向量
     * @param sceneId 场景ID
     * @return 处理结果
     */
    AjaxResult getPhoneNumber(String code, String encryptedData, String iv, String sceneId);
    
    /**
     * 确认登录状态
     *
     * @param openId 用户openId
     * @param sceneId 场景ID
     * @return 处理结果
     */
    AjaxResult confirmLoginStatus(String openId, String sceneId);
    
    /**
     * 确认扫码登录并处理用户关联
     *
     * @param sceneId 场景ID
     * @return 处理结果
     */
    AjaxResult confirmQrcode(String sceneId);
    
    /**
     * 取消扫码登录
     *
     * @param sceneId 场景ID
     * @return 处理结果
     */
    AjaxResult cancelQrcode(String sceneId);
} 