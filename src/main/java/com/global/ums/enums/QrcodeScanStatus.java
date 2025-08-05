package com.global.ums.enums;

import com.global.ums.utils.MessageUtils;
import com.global.ums.utils.SpringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 二维码扫描状态枚举
 */
@Getter
@AllArgsConstructor
public enum QrcodeScanStatus {

    WAITING("0", "等待扫码", "qrcode.status.waiting"),
    SCANNED("1", "已扫码", "qrcode.status.scanned"),
    CONFIRMED("2", "已确认", "qrcode.status.confirmed"),
    CANCELED("3", "已取消", "qrcode.status.canceled"),
    EXPIRED("4", "已过期", "qrcode.status.expired");

    private final String code;
    private final String desc;
    private final String messageKey;
    
    /**
     * 根据状态码获取状态枚举
     */
    public static QrcodeScanStatus getByCode(String code) {
        for (QrcodeScanStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return WAITING;
    }
    
    /**
     * 是否是等待扫码状态
     */
    public static boolean isWaiting(String status) {
        return WAITING.getCode().equals(status);
    }
    
    /**
     * 是否是已扫码状态
     */
    public static boolean isScanned(String status) {
        return SCANNED.getCode().equals(status);
    }
    
    /**
     * 是否是已确认状态
     */
    public static boolean isConfirmed(String status) {
        return CONFIRMED.getCode().equals(status);
    }
    
    /**
     * 是否是已取消状态
     */
    public static boolean isCanceled(String status) {
        return CANCELED.getCode().equals(status);
    }
    
    /**
     * 是否是已过期状态
     */
    public static boolean isExpired(String status) {
        return EXPIRED.getCode().equals(status);
    }
    
    /**
     * 获取国际化描述
     * 根据当前语言环境返回对应的状态描述
     * 
     * @return 国际化的状态描述
     */
    public String getI18nDesc() {
        try {
            MessageUtils messageUtils = SpringUtils.getBean(MessageUtils.class);
            return messageUtils.getMessage(this.messageKey);
        } catch (Exception e) {
            // 如果获取国际化消息失败，返回默认描述
            return this.desc;
        }
    }
    
    /**
     * 根据状态码获取国际化描述
     * 
     * @param code 状态码
     * @return 国际化的状态描述
     */
    public static String getI18nDescByCode(String code) {
        QrcodeScanStatus status = getByCode(code);
        return status.getI18nDesc();
    }
} 