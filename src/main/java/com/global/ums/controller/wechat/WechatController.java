package com.global.ums.controller.wechat;

import cn.hutool.core.util.StrUtil;
import com.global.ums.constant.UserPropertiesConstant;
import com.global.ums.entity.UserProperties;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.utils.MessageUtils;
import com.global.ums.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.util.crypto.WxMpCryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 微信公众号事件接收控制器
 */
@Slf4j
@RestController
@RequestMapping("/wechat")
public class WechatController {

    @Autowired
    private WxMpService wxMpService;

    @Autowired
    private UserPropertiesService userPropertiesService;

    /**
     * 微信服务器验证URL
     */
    @GetMapping("/receive")
    public String verify(@RequestParam("signature") String signature,
                        @RequestParam("timestamp") String timestamp,
                        @RequestParam("nonce") String nonce,
                        @RequestParam("echostr") String echostr) {
        
        log.info("微信服务器验证请求: signature={}, timestamp={}, nonce={}, echostr={}", 
                signature, timestamp, nonce, echostr);
        
        try {
            if (wxMpService.checkSignature(timestamp, nonce, signature)) {
                log.info("微信服务器验证成功");
                return echostr;
            } else {
                log.error("微信服务器验证失败");
                return SpringUtils.getBean(MessageUtils.class).getMessage("wechat.verify.failed");
            }
        } catch (Exception e) {
            log.error("微信服务器验证异常", e);
            return SpringUtils.getBean(MessageUtils.class).getMessage("wechat.verify.exception");
        }
    }

    /**
     * 接收微信服务器推送的消息和事件
     */
    @PostMapping("/receive")
    public String receive(@RequestParam("signature") String signature,
                         @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce,
                         @RequestParam(value = "encrypt_type", required = false) String encType,
                         @RequestParam(value = "msg_signature", required = false) String msgSignature,
                         @RequestBody String requestBody,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        
        log.info("接收微信消息: signature={}, timestamp={}, nonce={}, encType={}, msgSignature={}", 
                signature, timestamp, nonce, encType, msgSignature);
        log.info("微信消息内容: {}", requestBody);

        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");

        try {
            // 验证签名
            if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
                log.error("微信消息签名验证失败");
                return "";
            }

            WxMpXmlMessage xmlMessage;
            
            // 如果是加密消息
            if ("aes".equals(encType)) {
                log.info("处理加密消息");
                WxMpCryptUtil cryptUtil = new WxMpCryptUtil(wxMpService.getWxMpConfigStorage());
                xmlMessage = WxMpXmlMessage.fromEncryptedXml(requestBody, 
                        wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
            } else {
                // 明文消息
                log.info("处理明文消息");
                xmlMessage = WxMpXmlMessage.fromXml(requestBody);
            }

            log.info("解析后的消息类型: {}, 事件类型: {}", xmlMessage.getMsgType(), xmlMessage.getEvent());

            // 处理消息/事件
            WxMpXmlOutMessage outMessage = handleMessage(xmlMessage);
            
            if (outMessage != null) {
                String responseContent;
                if ("aes".equals(encType)) {
                    // 加密响应
                    WxMpCryptUtil cryptUtil = new WxMpCryptUtil(wxMpService.getWxMpConfigStorage());
                    responseContent = cryptUtil.encrypt(outMessage.toXml());
                } else {
                    // 明文响应
                    responseContent = outMessage.toXml();
                }
                log.info("响应微信消息: {}", responseContent);
                return responseContent;
            }
            
        } catch (Exception e) {
            log.error("处理微信消息异常", e);
        }
        
        return "";
    }

    /**
     * 处理微信消息和事件
     */
    private WxMpXmlOutMessage handleMessage(WxMpXmlMessage xmlMessage) {
        String msgType = xmlMessage.getMsgType();
        String event = xmlMessage.getEvent();
        
        log.info("处理消息类型: {}, 事件: {}", msgType, event);
        
        try {
            // 处理事件消息
            if ("event".equals(msgType)) {
                return handleEvent(xmlMessage);
            }
            
            // 处理文本消息
            // if ("text".equals(msgType)) {
            //     return handleTextMessage(xmlMessage);
            // }
            
        } catch (Exception e) {
            log.error("处理微信消息异常", e);
        }
        
        return null;
    }

    /**
     * 处理微信事件
     */
    private WxMpXmlOutMessage handleEvent(WxMpXmlMessage xmlMessage) {
        String event = xmlMessage.getEvent();
        String openId = xmlMessage.getFromUser();
        
        log.info("处理微信事件: {}, openId: {}", event, openId);
        
        try {
            // 处理关注事件
            if ("subscribe".equals(event)) {
                return handleSubscribeEvent(xmlMessage);
            }
            
            // 处理取消关注事件
            if ("unsubscribe".equals(event)) {
                log.info("用户取消关注: {}", openId);
                // 可以在这里处理取消关注逻辑
                return null;
            }
            
        } catch (Exception e) {
            log.error("处理微信事件异常", e);
        }
        
        return null;
    }

    /**
     * 处理关注事件
     */
    private WxMpXmlOutMessage handleSubscribeEvent(WxMpXmlMessage xmlMessage) {
        String openId = xmlMessage.getFromUser();
        
        log.info("用户关注事件: openId={}", openId);
        
        try {
            // 获取用户信息
            me.chanjar.weixin.mp.bean.result.WxMpUser wxUser = wxMpService.getUserService().userInfo(openId);
            String unionId = wxUser.getUnionId();
            
            log.info("获取用户信息: openId={}, unionId={}", openId, unionId);
            
            if (StrUtil.isNotBlank(unionId)) {
                // 通过 unionId 查询用户
                UserProperties unionIdProperty = userPropertiesService.getKeyisExist(
                        UserPropertiesConstant.KEY_UNIONID, unionId.getBytes());
                
                if (unionIdProperty != null) {
                    Long userId = unionIdProperty.getUserId();
                    log.info("找到用户: userId={}, unionId={}", userId, unionId);
                    
                    // 检查是否已经绑定了公众号 openId
                    UserProperties mpOpenIdProperty = userPropertiesService.getByUserIdAndKey(
                            userId, UserPropertiesConstant.KEY_MP_OPENID);
                    
                    if (mpOpenIdProperty == null) {
                        // 创建新的公众号 openId 属性
                        UserProperties newProperty = new UserProperties();
                        newProperty.setUserId(userId);
                        newProperty.setKey(UserPropertiesConstant.KEY_MP_OPENID);
                        newProperty.setValue(openId.getBytes());
                        newProperty.setScope(2);
                        
                        userPropertiesService.save(newProperty);
                        log.info("成功绑定公众号 openId: userId={}, openId={}", userId, openId);
                        
                        // 返回欢迎消息
                        return createTextMessage(xmlMessage, SpringUtils.getBean(MessageUtils.class).getMessage("wechat.welcome.bind.success"));
                    } else {
                        // 更新现有的公众号 openId
                        mpOpenIdProperty.setValue(openId.getBytes());
                        userPropertiesService.updateById(mpOpenIdProperty);
                        log.info("更新公众号 openId: userId={}, openId={}", userId, openId);
                        
                        return createTextMessage(xmlMessage, SpringUtils.getBean(MessageUtils.class).getMessage("wechat.welcome.update.success"));
                    }
                } else {
                    log.info("未找到对应的用户: unionId={}", unionId);
                    return createTextMessage(xmlMessage, SpringUtils.getBean(MessageUtils.class).getMessage("wechat.welcome.register.first"));
                }
            } else {
                log.warn("用户 unionId 为空: openId={}", openId);
                return createTextMessage(xmlMessage, SpringUtils.getBean(MessageUtils.class).getMessage("wechat.welcome.default"));
            }
            
        } catch (WxErrorException e) {
            log.error("获取用户信息失败: openId={}", openId, e);
            return createTextMessage(xmlMessage, SpringUtils.getBean(MessageUtils.class).getMessage("wechat.welcome.default"));
        } catch (Exception e) {
            log.error("处理关注事件异常: openId={}", openId, e);
            return createTextMessage(xmlMessage, SpringUtils.getBean(MessageUtils.class).getMessage("wechat.welcome.default"));
        }
    }

    /**
     * 处理文本消息
     */
    private WxMpXmlOutMessage handleTextMessage(WxMpXmlMessage xmlMessage) {
        String content = xmlMessage.getContent();
        log.info("收到文本消息: {}", content);
        
        // 简单的自动回复
        return createTextMessage(xmlMessage, SpringUtils.getBean(MessageUtils.class).getMessage("wechat.message.reply", content));
    }

    /**
     * 创建文本回复消息
     */
    private WxMpXmlOutMessage createTextMessage(WxMpXmlMessage xmlMessage, String content) {
        return WxMpXmlOutMessage.TEXT()
                .content(content)
                .fromUser(xmlMessage.getToUser())
                .toUser(xmlMessage.getFromUser())
                .build();
    }
} 