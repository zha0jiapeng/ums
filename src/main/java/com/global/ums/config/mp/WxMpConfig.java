package com.global.ums.config.mp;

import com.global.ums.config.WxConfig;
import lombok.Data;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import me.chanjar.weixin.mp.config.impl.WxMpRedisConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;


/**
 * 微信小程序配置
 */
@Data
@Configuration
public class WxMpConfig {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WxConfig wxConfig;

    /**
     * 服务实例
     */
    @Bean
    public WxMpService wxMpService() {

        WxConfig.Mp mp = wxConfig.getMp();

        WxMpDefaultConfigImpl config = new WxMpRedisConfigImpl(new RedisTemplateWxRedisOps(redisTemplate),mp.getAppId());
        config.setAppId(mp.getAppId());
        config.setSecret(mp.getAppSecret());
        config.setToken(mp.getToken());
        config.setAesKey(mp.getEncodingAESKey());

        WxMpService service = new WxMpServiceImpl();
        service.setWxMpConfigStorage(config);
        return service;
    }
} 