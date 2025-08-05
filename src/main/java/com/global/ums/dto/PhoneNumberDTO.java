package com.global.ums.dto;


import lombok.Data;

/**
 * 微信手机号获取入参DTO
 */
@Data
public class PhoneNumberDTO {
    
   private String code;
    
   private String encryptedData;
    
   private String iv;

    private String sceneId;
} 