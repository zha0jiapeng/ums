package com.global.ums.controller.report;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.date.DateUtil;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.WxMpTemplateMsgService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/report")
public class ReportController {
    @Autowired
    private final WxMpService wxMpService;

    @Autowired
    private final WxMaService wxMaService;

    public ReportController(WxMpService wxMpService, WxMaService wxMaService) {
        this.wxMpService = wxMpService;
        this.wxMaService = wxMaService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/pushWechat")
    public String pushWechat() throws WxErrorException {
        WxMpTemplateMsgService templateMsgService = wxMpService.getTemplateMsgService();
        WxMpTemplateMessage wxMpTemplateMessage = new WxMpTemplateMessage();
        wxMpTemplateMessage.setTemplateId("RvvWaQI0at6oQ_X3U5x42-kXKdRXvu15x3QGeMWEzBo");
        //微信公众号的对应openId
        wxMpTemplateMessage.setToUser("");
        WxMpTemplateMessage.MiniProgram miniProgram = new WxMpTemplateMessage.MiniProgram();
        miniProgram.setAppid(wxMaService.getWxMaConfig().getAppid());
        // miniProgram.setPagePath("pages/report/index");
        miniProgram.setPagePath("pages/index");
        wxMpTemplateMessage.setMiniProgram(miniProgram);
        //防重入id。对于同一个openid + client_msg_id, 只发送一条消息,10分钟有效,超过10分钟不保证效果。若无防重入需求，可不填
        //这里传报警记录的主键 确保一次报警只推送一次
        wxMpTemplateMessage.setClientMsgId("11");
        List<WxMpTemplateData> dataList = new ArrayList<>();
        WxMpTemplateData wxMpTemplateData = new WxMpTemplateData();
        wxMpTemplateData.setName("thing73");
        wxMpTemplateData.setValue("SD400MP");
        dataList.add(wxMpTemplateData);
        WxMpTemplateData wxMpTemplateData2 = new WxMpTemplateData();
        wxMpTemplateData2.setName("thing52");
        wxMpTemplateData2.setValue("220kV I母压变");
        dataList.add(wxMpTemplateData2);
        WxMpTemplateData wxMpTemplateData3 = new WxMpTemplateData();
        wxMpTemplateData3.setName("time3");
        wxMpTemplateData3.setValue(DateUtil.now());
        dataList.add(wxMpTemplateData3);
        WxMpTemplateData wxMpTemplateData4 = new WxMpTemplateData();
        wxMpTemplateData4.setName("thing46");
        wxMpTemplateData4.setValue("数据中断");
        dataList.add(wxMpTemplateData4);
        WxMpTemplateData wxMpTemplateData5 = new WxMpTemplateData();
        wxMpTemplateData5.setName("thing25");
        wxMpTemplateData5.setValue("数据中断超过3小时，请检查！");
        dataList.add(wxMpTemplateData5);
        wxMpTemplateMessage.setData(dataList);
        return templateMsgService.sendTemplateMsg(wxMpTemplateMessage);
    }
} 