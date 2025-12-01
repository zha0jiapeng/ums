package com.global.ums.controller.report;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.global.ums.constant.UserPropertiesConstant;
import com.global.ums.dto.AlarmPushDTO;
import com.global.ums.entity.User;
import com.global.ums.entity.UserGroup;
import com.global.ums.entity.UserProperties;
import com.global.ums.enums.UserType;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UserGroupService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 报告控制器
 */
@Slf4j
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final WxMpService wxMpService;
    private final WxMaService wxMaService;
    private final UserService userService;
    private final UserGroupService userGroupService;
    private final UserPropertiesService userPropertiesService;

    /**
     * 微信模板消息模板ID
     */
    private static final String ALARM_TEMPLATE_ID = "RvvWaQI0at6oQ_X3U5x42-kXKdRXvu15x3QGeMWEzBo";

    /**
     * 推送告警消息到微信公众号
     * 根据用户组ID遍历所有子节点中type=1且有mp_openid属性的用户进行推送
     *
     * @param dto 告警推送请求参数
     * @return 推送结果
     */
    @PostMapping("/push")
    public AjaxResult pushWechat(@RequestBody @Validated AlarmPushDTO dto) {
        // 获取用户组下所有需要推送的用户openId列表
        List<String> openIdList = getOpenIdListByGroupId(dto.getUserId());

        if (openIdList.isEmpty()) {
            return AjaxResult.error("没有找到需要推送的用户");
        }

        // 构建模板数据
        String alarmTime = StrUtil.isNotBlank(dto.getAlarmTime()) ? dto.getAlarmTime() : DateUtil.now();
        List<WxMpTemplateData> dataList = Arrays.asList(
                new WxMpTemplateData("thing73", dto.getSystemName()),    // 所属系统
                new WxMpTemplateData("thing52", dto.getPointName()),     // 测点名称
                new WxMpTemplateData("time3", alarmTime),                // 告警时间
                new WxMpTemplateData("thing46", dto.getAlarmType()),     // 告警类型
                new WxMpTemplateData("thing25", dto.getExceptionReason()) // 异常原因
        );

        int successCount = 0;
        int failCount = 0;
        List<String> failOpenIds = new ArrayList<>();

        // 遍历推送
        for (int i = 0; i < openIdList.size(); i++) {
            String openId = openIdList.get(i);
            try {
                WxMpTemplateMessage message = new WxMpTemplateMessage();
                message.setTemplateId(ALARM_TEMPLATE_ID);
                message.setToUser(openId);

                // 设置小程序跳转
                WxMpTemplateMessage.MiniProgram miniProgram = new WxMpTemplateMessage.MiniProgram();
                miniProgram.setAppid(wxMaService.getWxMaConfig().getAppid());
                miniProgram.setPagePath("pages/index");
                message.setMiniProgram(miniProgram);

                // 防重入id，确保一次报警只推送一次（每个用户使用不同的clientMsgId）
                message.setClientMsgId(dto.getClientMsgId() + "_" + openId);
                message.setData(dataList);

                wxMpService.getTemplateMsgService().sendTemplateMsg(message);
                successCount++;
            } catch (WxErrorException e) {
                log.error("推送消息给用户 {} 失败: {}", openId, e.getMessage());
                failCount++;
                failOpenIds.add(openId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", openIdList.size());
        result.put("success", successCount);
        result.put("fail", failCount);
        if (!failOpenIds.isEmpty()) {
            result.put("failOpenIds", failOpenIds);
        }

        if (failCount == 0) {
            return AjaxResult.success("推送成功", result);
        } else if (successCount == 0) {
            return AjaxResult.error("推送全部失败", result);
        } else {
            return AjaxResult.success("部分推送成功", result);
        }
    }

    /**
     * 根据用户组ID获取所有需要推送的用户openId列表
     * 递归遍历所有子节点，筛选type=1且有mp_openid属性的用户
     *
     * @param groupId 用户组ID
     * @return openId列表
     */
    private List<String> getOpenIdListByGroupId(Long groupId) {
        List<String> openIdList = new ArrayList<>();
        Set<Long> visitedIds = new HashSet<>();
        collectOpenIds(groupId, openIdList, visitedIds);
        return openIdList;
    }

    /**
     * 递归收集用户openId
     *
     * @param parentId 父节点ID
     * @param openIdList openId列表
     * @param visitedIds 已访问的ID集合（防止循环引用）
     */
    private void collectOpenIds(Long parentId, List<String> openIdList, Set<Long> visitedIds) {
        if (parentId == null || visitedIds.contains(parentId)) {
            return;
        }
        visitedIds.add(parentId);

        // 获取该节点的所有子节点
        List<UserGroup> children = userGroupService.getByParentUserId(parentId);

        for (UserGroup child : children) {
            Long userId = child.getUserId();
            if (userId == null || visitedIds.contains(userId)) {
                continue;
            }

            User user = userService.getById(userId);
            if (user == null) {
                continue;
            }

            // 如果是普通用户(type=1)，检查是否有mp_openid属性
            if (UserType.USER.getValue() == user.getType()) {
                UserProperties mpOpenIdProp = userPropertiesService.getByUserIdAndKey(userId, UserPropertiesConstant.KEY_MP_OPENID);
                if (mpOpenIdProp != null && mpOpenIdProp.getValue() != null) {
                    String openId = new String(mpOpenIdProp.getValue());
                    if (StrUtil.isNotBlank(openId)) {
                        openIdList.add(openId);
                    }
                }
                visitedIds.add(userId);
            } else if (UserType.USER_GROUP.getValue() == user.getType()) {
                // 如果是用户组(type=2)，递归处理
                collectOpenIds(userId, openIdList, visitedIds);
            }
        }
    }
}
