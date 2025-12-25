package com.global.ums.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 告警推送请求DTO
 * 用于微信公众号模板消息推送
 */
@Data
public class AlarmPushDTO {

    /**
     * 用户组ID，将遍历该用户组下所有type=1且有mp_openid属性的用户进行推送
     */
    @NotNull(message = "userId不能为空")
    private Long userId;


    /**
     * 所属系统 (thing73)
     */
    @NotBlank(message = "所属系统不能为空")
    private String systemName;

    /**
     * 测点名称 (thing52)
     */
    @NotBlank(message = "测点名称不能为空")
    private String pointName;

    /**
     * 告警时间 (time3)
     * 如果不传，默认使用当前时间
     */
    private String alarmTime;

    /**
     * 告警类型 (thing46)
     */
    @NotBlank(message = "告警类型不能为空")
    private String alarmType;

    /**
     * 异常原因 (thing25)
     */
    @NotBlank(message = "异常原因不能为空")
    private String exceptionReason;
}
