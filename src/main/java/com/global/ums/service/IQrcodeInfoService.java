package com.global.ums.service;


import com.global.ums.entity.QrcodeInfo;

/**
 * 二维码信息Service接口
 */
public interface IQrcodeInfoService {
    
    /**
     * 创建登录二维码
     * 
     * @param userIp 用户IP
     * @param userAgent 用户设备信息
     * @return 二维码信息
     */
    public QrcodeInfo createLoginQrcode(String userIp, String userAgent);
    
    /**
     * 获取二维码状态
     * 
     * @param sceneId 场景ID
     * @return 状态码
     */
    public String getQrcodeStatus(String sceneId);
    
    /**
     * 根据场景ID查询二维码信息
     * 
     * @param sceneId 场景ID
     * @return 二维码信息
     */
    public QrcodeInfo selectQrcodeInfoBySceneId(String sceneId);
    
    /**
     * 更新二维码状态
     * 
     * @param sceneId 场景ID
     * @param status 状态码
     * @return 更新结果
     */
    public int updateQrcodeStatus(String sceneId, String status);
    
    /**
     * 更新二维码扫码用户
     *
     * @param sceneId 场景ID
     * @param openId  微信用户openId
     */
    public void updateQrcodeScanUser(String sceneId, String openId, String unionId, Long userId);

    public void updateQrcodeKey(String sceneId, String key, String value);
} 