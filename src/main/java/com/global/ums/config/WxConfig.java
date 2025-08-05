package com.global.ums.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wx")
public class WxConfig {

    private Integer qrcodeExpireSecond;
    private Mp mp;
    private Ma ma;

    @Data
    public static class Mp {
        private String appId;
        private String appSecret;
        private String token;
        private String encodingAESKey;
    }

    @Data
    public static class Ma{
        private String appId;
        private String appSecret;
        private String token;
        private String encodingAESKey;
    }
} 