package com.global.ums.entity;

import lombok.Data;
import lombok.ToString;


/**
 * 二维码信息对象
 */
@Data
@ToString(callSuper = true)
public class QrcodeInfo {
    private Long id;

    private String sceneId;

    /** 扫码状态 */
      private String status;

    /** 二维码类型 */
    private String type;

    /** 微信用户标识 */
     private String openId;

    /** 微信用户unionid */
     private String unionId;

    /** 用户IP */
    private String userIp;
    
    /** 用户设备信息 */
    private String userAgent;
    
    /** 关联的用户ID */
    private Long userId;
    
    /** 登录令牌 */
    private String token;
} 