package com.global.ums.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaCodeLineColor;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.global.ums.dto.TokenDTO;
import com.global.ums.entity.QrcodeInfo;
import com.global.ums.entity.User;
import com.global.ums.entity.UserGroup;
import com.global.ums.entity.UserProperties;
import com.global.ums.enums.QrcodeScanStatus;
import com.global.ums.enums.UserType;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.*;
import com.global.ums.utils.*;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

import com.global.ums.constant.UserPropertiesConstant;

/**
 * 微信小程序服务实现
 */
@Slf4j
@Service
public class MiniappServiceImpl implements IMiniappService {

    @Autowired
    private IQrcodeInfoService qrcodeInfoService;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private UserService userService;
    
    private static final String QR_INFO_KEY_PREFIX = "wx_qrcode:info:";

    @Autowired
    private UserPropertiesService userPropertiesService;

    @Autowired
    private UserGroupService userGroupService;

    @Value("${user.default-password:123456}")
    private String defaultPassword;

    @Autowired
    private JwtUtils jwtUtils;


    @Override
    public AjaxResult generateLoginQrcode(String userIp, String userAgent) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 创建二维码信息
            QrcodeInfo qrcodeInfo = qrcodeInfoService.createLoginQrcode(userIp, userAgent);

            try {
                // 使用无限制小程序码API
                String page = "pages/login"; // 小程序页面路径，使用首页更可靠
                String scene = qrcodeInfo.getSceneId(); // 场景值，长度限制32位

                log.info("生成小程序码 - scene:{}, page:{}", scene, page);

                // 调用微信API生成小程序码（接口B - 无限制小程序码）
                byte[] qrcodeBytes = wxMaService.getQrcodeService().createWxaCodeUnlimitBytes(
                        scene,         // 场景值
                        page,          // 小程序页面（使用首页）
                        false,         // checkPath设为false，允许页面未发布
                        "release",     // envVersion使用正式版
                        430,           // 宽度
                        true,          // 自动配置线条颜色
                        new WxMaCodeLineColor("0", "0", "0"), // 线条颜色
                        true          // 不使用透明底色
                );

                // 转为Base64
                String qrcodeBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrcodeBytes);

                result.put("scene_id", qrcodeInfo.getSceneId());
                result.put("qrcode_img", qrcodeBase64);

                return AjaxResult.success(result);
            } catch (WxErrorException e) {
                log.error("调用微信API生成小程序码失败: {}", e.getMessage(), e);
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("miniapp.qrcode.generate.error", e.getMessage()));
            }
        } catch (Exception e) {
            log.error("生成小程序码失败", e);
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("miniapp.qrcode.generate.error", e.getMessage()));
        }
    }


    /**
     * 处理扫码事件
     * 
     * @param sceneId 场景ID
     * @param status 状态
     * @return 处理结果
     */
    @Override
    public boolean handleScan(String sceneId, String status) {
        if (StringUtils.isEmpty(sceneId) || StringUtils.isEmpty(status)) {
            return false;
        }
        
        // 获取二维码信息
        QrcodeInfo qrcodeInfo = qrcodeInfoService.selectQrcodeInfoBySceneId(sceneId);
        if (qrcodeInfo == null) {
            return false;
        }
        
        // 检查状态转换的合法性
        boolean canUpdateStatus = isCanUpdateStatus(status, qrcodeInfo);

        // 如果状态转换合法，则更新状态
        if (canUpdateStatus) {
            int result = qrcodeInfoService.updateQrcodeStatus(sceneId, status);
            return result > 0;
        }
        
        return false;
    }

    private static boolean isCanUpdateStatus(String status, QrcodeInfo qrcodeInfo) {
        boolean canUpdateStatus = false;

        if (status.equals(QrcodeScanStatus.SCANNED.getCode())) {
            // 只有等待状态可以转为已扫码状态
            canUpdateStatus = QrcodeScanStatus.WAITING.getCode().equals(qrcodeInfo.getStatus());
        } else if (status.equals(QrcodeScanStatus.CONFIRMED.getCode())) {
            // 只有已扫码状态可以转为已确认状态
            canUpdateStatus = QrcodeScanStatus.SCANNED.getCode().equals(qrcodeInfo.getStatus());
        } else if (status.equals(QrcodeScanStatus.CANCELED.getCode())) {
            // 等待状态和已扫码状态都可以转为已取消状态
            canUpdateStatus = QrcodeScanStatus.WAITING.getCode().equals(qrcodeInfo.getStatus()) 
                           || QrcodeScanStatus.SCANNED.getCode().equals(qrcodeInfo.getStatus());
        }
        return canUpdateStatus;
    }

    /**
     * 获取二维码详情
     * 
     * @param sceneId 场景ID
     * @return 二维码信息
     */
    @Override
    public QrcodeInfo getQrcodeInfo(String sceneId) {
        return qrcodeInfoService.selectQrcodeInfoBySceneId(sceneId);
    }
    
//    /**
//     * 处理用户邀请
//     *
//     * @param openId 邀请人的openId
//     * @param loginUser 被邀请的用户登录信息
//     * @return 处理结果
//     */
//    @Override
//    @Transactional
//    public AjaxResult processInvitation(String openId) {
//        Long userId = LoginUserContextHolder.getUserId();
//        User invitedUser = userService.getById(userId);
//
//        if (StringUtils.isEmpty(openId) || userId == null || invitedUser == null) {
//            return AjaxResult.error("邀请参数无效");
//        }
//
//        if (invitedUser.getDeptId() != null) {
//            return AjaxResult.success("被邀请人已有部门，无法绑定。");
//        }
//
//        try {
//            // 通过openId查询邀请人在微信用户映射表中的信息
//            WechatUserMap inviterWechatMap = wechatUserMapService.selectWechatUserMapByOpenId(openId);
//            if (inviterWechatMap == null) {
//                return AjaxResult.success("邀请人不存在或未关联微信");
//            }
//
//            // 获取邀请人的用户ID
//            Long inviterId = inviterWechatMap.getUserId();
//            if (inviterId == null) {
//                return AjaxResult.success("邀请人未绑定系统用户");
//            }
//
//            // 查询邀请人信息
//            SysUser inviter = userService.selectUserById(inviterId);
//            if (inviter == null) {
//                return AjaxResult.success("邀请人不存在");
//            }
//
//            if (inviter.getDeptId() == null) {
//                return AjaxResult.error("邀请人未绑定部门，无法完成邀请");
//            }
//
//            // 将被邀请人的部门ID修改为邀请人的部门ID
//            invitedUser.setDeptId(inviter.getDeptId());
//
//            // 更新被邀请人信息
//            userService.updateUser(invitedUser);
//
//            // 获取被邀请人的openId
//            String invitedOpenId = null;
//            WechatUserMap invitedWechatMap = wechatUserMapService.selectWechatUserMapByUserId(invitedUser.getUserId());
//            if (invitedWechatMap != null) {
//                invitedOpenId = invitedWechatMap.getOpenId();
//            }
//
//            // 创建邀请记录
//            invitationRecordService.createInvitationRecord(
//                inviterId,
//                openId,
//                invitedUser.getUserId(),
//                invitedOpenId,
//                inviter.getDeptId()
//            );
//
//            invitedUser.setDept(inviter.getDept());
//            loginUser.setUser(invitedUser);
//
//            log.info("用户邀请处理成功：被邀请人[{}]加入邀请人[{}]的部门[{}]", invitedUser.getUserId(), inviter.getUserId(), inviter.getDeptId());
//            return AjaxResult.success("加入成功，您已加入邀请人的部门");
//        } catch (Exception e) {
//            log.error("处理用户邀请失败", e);
//            return AjaxResult.error("接受邀请失败：" + e.getMessage());
//        }
//    }
    
    /**
     * 获取用户手机号
     *
     * @param code 微信临时登录凭证
     * @param encryptedData 加密数据
     * @param iv 初始向量
     * @param sceneId 场景ID
     * @return 处理结果
     */
    @Override
    @Transactional
    public AjaxResult getPhoneNumber(String code, String encryptedData, String iv, String sceneId) {
        try {
            if (StringUtils.isEmpty(code) || StringUtils.isEmpty(encryptedData) || StringUtils.isEmpty(iv)) {
                return AjaxResult.errorI18n("common.param.empty");
            }
            
            // 通过code获取session信息(包含openid和session_key)
            WxMaJscode2SessionResult sessionInfo = wxMaService.jsCode2SessionInfo(code);
            String openid = sessionInfo.getOpenid();
            String sessionKey = sessionInfo.getSessionKey();
            String unionid = sessionInfo.getUnionid();
            
            // 使用sessionKey解密encryptedData获取手机号信息
            WxMaPhoneNumberInfo phoneInfo = wxMaService.getUserService().getPhoneNoInfo(sessionKey,encryptedData,iv);
            String phoneNumber = phoneInfo.getPhoneNumber();

            //根据手机号查询 如果没注册过 那就注册。
            User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUniqueId, unionid));
            if(user == null){
                //没注册过
                user = new User();
                user.setUniqueId(unionid);
                user.setType(UserType.USER.getValue());
                userService.save(user);
                wechatRegister(phoneNumber, openid, unionid, user);
            }


            TokenDTO tokenDTO = jwtUtils.generateToken(user.getId(), user.getType(), phoneNumber);

            qrcodeInfoService.updateQrcodeScanUser(sceneId, openid, unionid, user.getId());
            Map<String,Object> responseMap = new HashMap<>();
            responseMap.put("openId", openid);
            responseMap.put("token", tokenDTO);
            return AjaxResult.success(SpringUtils.getBean(MessageUtils.class).getMessage("miniapp.phone.get.success"), responseMap);
        } catch (WxErrorException e) {
            log.error("获取手机号失败", e);
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("miniapp.phone.get.error", e.getMessage()));
        }
    }

    private void wechatRegister(String phoneNumber, String openid, String unionid, User user) {
        Map<String,byte[]> map = new HashMap<>();
        map.put(UserPropertiesConstant.KEY_MA_OPENID, openid.getBytes(StandardCharsets.UTF_8));
        map.put(UserPropertiesConstant.KEY_UNIONID, unionid.getBytes(StandardCharsets.UTF_8));
        map.put(UserPropertiesConstant.KEY_PHONE_NUMBER, phoneNumber.getBytes(StandardCharsets.UTF_8));
        map.put(UserPropertiesConstant.KEY_USERNAME, phoneNumber.getBytes(StandardCharsets.UTF_8));
        String salt = PasswordUtils.generateSalt();
        map.put(UserPropertiesConstant.KEY_PASSWORD, PasswordUtils.toBytes(PasswordUtils.encryptPassword(defaultPassword, salt), salt));
        map.put(UserPropertiesConstant.KEY_NICKNAME,("微信用户"+ phoneNumber).getBytes(StandardCharsets.UTF_8));
        map.put(UserPropertiesConstant.KEY_CREATE_TIME, DateUtil.now().getBytes(StandardCharsets.UTF_8));
 
        userPropertiesService.saveUserPropertiesMap(user.getId(),map);
    }
    /**
     * 确认登录状态
     *
     * @param openId 用户openId
     * @param sceneId 场景ID
     * @return 处理结果
     */
    @Override
    public AjaxResult confirmLoginStatus(String openId, String sceneId) {
        if (StringUtils.isEmpty(openId)) {
            return AjaxResult.errorI18n("common.param.empty");
        }
        
        try {
            // 1. 根据openId查询微信用户映射关系
            UserProperties userProperties = userPropertiesService.getKeyisExist("ma_openid", openId.getBytes(StandardCharsets.UTF_8));
            if (userProperties == null) {
                return AjaxResult.errorI18n("miniapp.wechat.user.not.found");
            }
            
            // 2. 查询对应的系统用户信息
            User user = userService.getById(userProperties.getUserId());
            if (user == null) {
                return AjaxResult.errorI18n("miniapp.system.user.not.found");
            }

            String phoneNumber = null;
            List<UserProperties> properties = userPropertiesService.getByUserId(user.getId(),null);
            for(UserProperties item : properties){
                if(item.getKey().equals("phone_number")){
                    phoneNumber =  new String(item.getValue());
                    break;
                }
            }
            if(phoneNumber == null){
                return AjaxResult.errorI18n("miniapp.user.no.phone");
            }

            TokenDTO tokenDTO = jwtUtils.generateToken(user.getId(), user.getType(), phoneNumber);

            Map<String, Object> responseMap = new HashMap<>();
            
            if (sceneId != null) {
                // 4. 查询二维码信息
                String qrCodeKey = QR_INFO_KEY_PREFIX + sceneId;
                responseMap = redisCache.getCacheMap(qrCodeKey);
                if (responseMap == null || responseMap.isEmpty()) {
                    return AjaxResult.errorI18n("miniapp.qrcode.expired");
                }
                responseMap.put("status", QrcodeScanStatus.CONFIRMED.getCode());
                responseMap.put("token", tokenDTO);
                redisCache.setCacheMap(qrCodeKey, responseMap);
                return AjaxResult.success(SpringUtils.getBean(MessageUtils.class).getMessage("auth.login.success"), responseMap);
            } else {
                responseMap.put("token", tokenDTO);
                return AjaxResult.success(SpringUtils.getBean(MessageUtils.class).getMessage("auth.login.success"), responseMap);
            }
        } catch (Exception e) {
            log.error("获取token异常", e);
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("miniapp.token.get.error", e.getMessage()));
        }
    }
    
    /**
     * 确认扫码登录并处理用户关联
     *
     * @param sceneId 场景ID
     * @return 处理结果
     */
    @Override
    @Transactional
    public AjaxResult confirmQrcode(String sceneId) {
        try {
            // 处理扫码事件
            boolean result = handleScan(sceneId, QrcodeScanStatus.CONFIRMED.getCode());
            if (!result) {
                return AjaxResult.errorI18n("miniapp.qrcode.scan.error");
            }
            
            // 获取二维码信息
            QrcodeInfo qrcodeInfo = qrcodeInfoService.selectQrcodeInfoBySceneId(sceneId);
            if (qrcodeInfo == null) {
                return AjaxResult.errorI18n("miniapp.qrcode.not.exists.or.expired");
            }
            
            // 判断二维码是否已经关联了用户
            Map<String, Object> tokenInfo = new HashMap<>();
            Long userId = null;

            if (StringUtils.isNotEmpty(qrcodeInfo.getOpenId())) {
                // 通过openId查找关联的用户
                String openId = qrcodeInfo.getOpenId();
                // 1. 根据openId查询微信用户映射关系
                UserProperties userProperties = userPropertiesService.getKeyisExist("ma_openid", openId.getBytes(StandardCharsets.UTF_8));
                if (userProperties == null) {
                    return AjaxResult.errorI18n("miniapp.wechat.user.not.found");
                }

                // 2. 查询对应的系统用户信息
                User user = userService.getById(userProperties.getUserId());
                if (user == null) {
                    return AjaxResult.errorI18n("miniapp.system.user.not.found");
                }
                String phoneNumber = null;
                List<UserProperties> properties = userPropertiesService.getByUserId(user.getId(),null);
                for(UserProperties item : properties){
                    if(item.getKey().equals("phone_number")){
                        phoneNumber =  new String(item.getValue());
                        break;
                    }
                }
                if(phoneNumber == null){
                    return AjaxResult.errorI18n("miniapp.user.no.phone");
                }
                TokenDTO tokenDTO = jwtUtils.generateToken(user.getId(), user.getType(), phoneNumber);
                tokenInfo.put("token", tokenDTO);
                qrcodeInfoService.updateQrcodeKey(sceneId, "token", tokenDTO.getAccessToken());
            } else {
                tokenInfo.put("message", "二维码未关联微信用户");
            }

            // 返回token和相关信息
            return AjaxResult.success(SpringUtils.getBean(MessageUtils.class).getMessage("miniapp.confirm.success"), tokenInfo);
        } catch (Exception e) {
            log.error("处理扫码登录失败", e);
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("miniapp.scan.login.process.error", e.getMessage()));
        }
    }

    /**
     * 取消扫码登录
     *
     * @param sceneId 场景ID
     * @return 处理结果
     */
    @Override
    public AjaxResult cancelQrcode(String sceneId) {
        try {
            // 处理扫码事件
            boolean result = handleScan(sceneId, QrcodeScanStatus.CANCELED.getCode());
            if (!result) {
                return AjaxResult.errorI18n("miniapp.qrcode.scan.error");
            }
            return AjaxResult.successI18n("miniapp.cancel.success");
        } catch (Exception e) {
            log.error("处理扫码事件失败", e);
            return AjaxResult.errorI18n("miniapp.qrcode.scan.error");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult processInvitation(String openId, Long userId) {
        // 1. 验证参数
        if (openId == null || openId.trim().isEmpty()) {
            return AjaxResult.errorI18n("miniapp.invitation.openid.empty");
        }

        // 2. 验证被邀请用户是否存在
        User invitedUser = userService.getById(userId);
        if (invitedUser == null) {
            return AjaxResult.errorI18n("miniapp.user.not.found");
        }

        // 3. 根据 openId (ma_openid) 找到邀请人
        UserProperties inviterProperty = userPropertiesService.getKeyisExist(
            UserPropertiesConstant.KEY_MA_OPENID,
            openId.getBytes(StandardCharsets.UTF_8)
        );

        if (inviterProperty == null || inviterProperty.getUserId() == null) {
            return AjaxResult.errorI18n("miniapp.invitation.inviter.not.found");
        }

        Long inviterId = inviterProperty.getUserId();

        // 4. 验证邀请人不能是自己
        if (inviterId.equals(userId)) {
            return AjaxResult.errorI18n("miniapp.invitation.cannot.invite.self");
        }

        // 5. 验证邀请人是否存在
        User inviter = userService.getById(inviterId);
        if (inviter == null) {
            return AjaxResult.errorI18n("miniapp.invitation.inviter.not.found");
        }

        // 6. 获取邀请人的 department-admin 属性
        UserProperties departmentProperty = userPropertiesService.getByUserIdAndKey(
            inviterId,
            UserPropertiesConstant.KEY_DEPARTMENT_ADMIN
        );

        if (departmentProperty == null || departmentProperty.getValue() == null) {
            return AjaxResult.errorI18n("miniapp.invitation.inviter.no.department");
        }

        // 7. 检查被邀请用户是否已经在该邀请人的用户组中
        List<UserGroup> existingGroups = userGroupService.getByUserId(userId);
        boolean alreadyInGroup = existingGroups.stream()
            .anyMatch(group -> inviterId.equals(group.getParentUserId()));

        if (alreadyInGroup) {
            return AjaxResult.errorI18n("miniapp.invitation.already.in.group");
        }

        // 8. 添加用户组关系
        boolean success = userGroupService.addUserGroup(userId, inviterId);

        if (success) {
            return AjaxResult.successI18n("miniapp.invitation.success");
        } else {
            return AjaxResult.errorI18n("miniapp.invitation.failed");
        }
    }

    /**
     * 处理扫码事件并返回结果
     * 
     * @param sceneId 场景ID
     * @return 处理结果
     */
    @Override
    public AjaxResult scanQrcode(String sceneId) {
        try {
            // 处理扫码事件
            boolean result = handleScan(sceneId, QrcodeScanStatus.SCANNED.getCode());
            if (!result) {
                return AjaxResult.errorI18n("miniapp.qrcode.scan.error");
            }
            
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("处理扫码事件失败", e);
            return AjaxResult.errorI18n("miniapp.qrcode.scan.error");
        }
    }

    /**
     * 检查小程序扫码登录状态
     * 
     * @param sceneId 场景ID
     * @return 处理结果
     */
    @Override
    public AjaxResult checkQrcodeStatus(String sceneId) {
        Map<String, Object> result = new HashMap<>();
        QrcodeInfo qrcodeInfo = getQrcodeInfo(sceneId);
        
        if (qrcodeInfo == null) {
            result.put("scene_id", sceneId);
            result.put("status", "4");
            result.put("statusDesc", QrcodeScanStatus.getByCode("4").getI18nDesc());
            return AjaxResult.success(SpringUtils.getBean(MessageUtils.class).getMessage("miniapp.qrcode.not.exists.or.expired"), result);
        }

        result.put("scene_id", sceneId);
        result.put("status", qrcodeInfo.getStatus());
        result.put("statusDesc", QrcodeScanStatus.getByCode(qrcodeInfo.getStatus()).getI18nDesc());
        
        String infoKey = QR_INFO_KEY_PREFIX + sceneId;
        Map<String, Object> qrInfo = redisCache.getCacheMap(infoKey);
         
        if (qrInfo != null && !qrInfo.isEmpty()) {
            if (qrInfo.get("userId")!=null) {
                User user = userService.getUserWithProperties(Long.parseLong(qrInfo.get("userId").toString()));
                if (user != null) {
                    result.put("token", jwtUtils.generateToken(user.getId(), user.getType(), user.getUsernameFromProperties()));
                }
            }
        }
        return AjaxResult.success(result);
    }
} 