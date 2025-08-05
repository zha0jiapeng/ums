package com.global.ums.config.ma;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaRedisBetterConfigImpl;
import com.global.ums.config.WxConfig;
import lombok.Data;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;


/**
 * 微信小程序配置
 */
@Data
@Configuration
public class WxMaConfig {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WxConfig wxConfig;
    /**
     * 小程序服务实例
     */
    @Bean
    public WxMaService wxMaService() {
        WxConfig.Ma ma = wxConfig.getMa();
        if (ma == null || ma.getAppId() == null) {
            throw new IllegalStateException("Wx ma config is not complete, please check your application.yml");
        }
        WxMaDefaultConfigImpl config = new WxMaRedisBetterConfigImpl(new RedisTemplateWxRedisOps(redisTemplate), ma.getAppId());
        config.setAppid(ma.getAppId());
        config.setSecret(ma.getAppSecret());
        config.setToken(ma.getToken());
        config.setAesKey(ma.getEncodingAESKey());

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
} 