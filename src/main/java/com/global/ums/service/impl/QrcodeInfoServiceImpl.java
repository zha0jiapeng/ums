package com.global.ums.service.impl;


import com.global.ums.entity.QrcodeInfo;
import com.global.ums.enums.QrcodeScanStatus;
import com.global.ums.service.IQrcodeInfoService;
import com.global.ums.utils.QRCodeUtil;
import com.global.ums.utils.RedisCache;
import com.global.ums.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 二维码信息Service实现
 */
@Service
public class QrcodeInfoServiceImpl implements IQrcodeInfoService {

    @Autowired
    private RedisCache redisCache;

    private static final String QR_INFO_KEY_PREFIX = "wx_qrcode:info:";
    private static final int QR_EXPIRE_TIME = 10;  // 二维码过期时间(分钟)

    /**
     * 创建登录二维码
     * 
     * @param userIp 用户IP
     * @param userAgent 用户设备信息
     * @return 二维码信息
     */
    @Override
    public QrcodeInfo createLoginQrcode(String userIp, String userAgent) {
        // 生成随机场景ID
        String sceneId = QRCodeUtil.generateSceneId();
        
        // 创建二维码信息
        QrcodeInfo qrcodeInfo = new QrcodeInfo();
        qrcodeInfo.setSceneId(sceneId);
        qrcodeInfo.setStatus(QrcodeScanStatus.WAITING.getCode());
        qrcodeInfo.setType("login");
        qrcodeInfo.setUserIp(userIp);
        qrcodeInfo.setUserAgent(userAgent);

        // 将二维码信息保存到Redis
        Map<String, Object> qrInfo = new HashMap<>();
        qrInfo.put("sceneId", sceneId);
        qrInfo.put("type", qrcodeInfo.getType());
        qrInfo.put("userIp", userIp);
        qrInfo.put("userAgent", userAgent);
        qrInfo.put("status", QrcodeScanStatus.WAITING.getCode());
        
        String infoKey = QR_INFO_KEY_PREFIX + sceneId;
        redisCache.setCacheMap(infoKey, qrInfo);
        redisCache.expire(infoKey, QR_EXPIRE_TIME, TimeUnit.MINUTES);
        
        return qrcodeInfo;
    }

    /**
     * 获取二维码状态
     * 
     * @param sceneId 场景ID
     * @return 状态码
     */
    @Override
    public String getQrcodeStatus(String sceneId) {
        if (StringUtils.isEmpty(sceneId)) {
            return QrcodeScanStatus.WAITING.getCode();
        }
        // 检查是否已过期
        String infoKey = QR_INFO_KEY_PREFIX + sceneId;
        Map<String, Object> qrInfo = redisCache.getCacheMap(infoKey);
        if(qrInfo==null){
            return QrcodeScanStatus.EXPIRED.getCode();
        }
        String status = qrInfo.get("status").toString();
        if (qrInfo.containsKey("expireTime")) {
            Long expireTime = (Long) qrInfo.get("expireTime");
            if (expireTime != null && System.currentTimeMillis() > expireTime) {
                // 更新状态为已过期
                if (!QrcodeScanStatus.EXPIRED.getCode().equals(status)) {
                    updateQrcodeStatus(sceneId, QrcodeScanStatus.EXPIRED.getCode());
                }
                return QrcodeScanStatus.EXPIRED.getCode();
            }
        }
        
        return status;
    }
    
    /**
     * 根据场景ID查询二维码信息
     * 
     * @param sceneId 场景ID
     * @return 二维码信息
     */
    @Override
    public QrcodeInfo selectQrcodeInfoBySceneId(String sceneId) {
        if (StringUtils.isEmpty(sceneId)) {
            return null;
        }
        
        // 从Redis获取二维码信息
        String infoKey = QR_INFO_KEY_PREFIX + sceneId;
        Map<String, Object> qrInfo = redisCache.getCacheMap(infoKey);
        
        if (qrInfo == null || qrInfo.isEmpty()) {
            return null;
        }
        
        QrcodeInfo qrcodeInfo = new QrcodeInfo();
        qrcodeInfo.setSceneId(sceneId);
        qrcodeInfo.setType((String) qrInfo.get("type"));

        String status = qrInfo.get("status").toString();
        qrcodeInfo.setStatus(status);
        
        // 设置其他字段
        if (qrInfo.containsKey("openId")) {
            qrcodeInfo.setOpenId((String) qrInfo.get("openId"));
        }
        
        if (qrInfo.containsKey("userIp")) {
            qrcodeInfo.setUserIp((String) qrInfo.get("userIp"));
        }
        
        if (qrInfo.containsKey("userAgent")) {
            qrcodeInfo.setUserAgent((String) qrInfo.get("userAgent"));
        }
        return qrcodeInfo;
    }
    
    /**
     * 更新二维码状态
     * 
     * @param sceneId 场景ID
     * @param status 状态码
     * @return 更新结果
     */
    @Override
    public int updateQrcodeStatus(String sceneId, String status) {
        if (StringUtils.isEmpty(sceneId) || StringUtils.isEmpty(status)) {
            return 0;
        }
        // 更新Redis中的状态
        String statusKey = QR_INFO_KEY_PREFIX + sceneId;
        Map<String, Object> cacheMap = redisCache.getCacheMap(statusKey);
        cacheMap.put("status", status);
        redisCache.setCacheMap(statusKey, cacheMap);
        return 1;
    }
    
    /**
     * 更新二维码扫码用户
     *
     * @param sceneId 场景ID
     * @param openId  微信用户openId
     */
    @Override
    public void updateQrcodeScanUser(String sceneId, String openId, String unionId, Long userId) {
        if (StringUtils.isEmpty(sceneId) || StringUtils.isEmpty(openId)) {
            return;
        }
        // 更新Redis中的二维码信息
        String infoKey = QR_INFO_KEY_PREFIX + sceneId;
        Map<String, Object> qrInfo = redisCache.getCacheMap(infoKey);
        
        if (qrInfo != null) {
            qrInfo.put("openId", openId);
            qrInfo.put("unionId", unionId);
            qrInfo.put("userId", userId);
            redisCache.setCacheMap(infoKey, qrInfo);
            
            // 更新状态为已扫码
            updateQrcodeStatus(sceneId, QrcodeScanStatus.SCANNED.getCode());
        }

    }

    @Override
    public void updateQrcodeKey(String sceneId, String key, String value) {
        // 更新Redis中的二维码信息
        String infoKey = QR_INFO_KEY_PREFIX + sceneId;
        Map<String, Object> qrInfo = redisCache.getCacheMap(infoKey);
        if (qrInfo != null) {
            qrInfo.put(key, value);
            redisCache.setCacheMap(infoKey, qrInfo);
        }
    }
} 