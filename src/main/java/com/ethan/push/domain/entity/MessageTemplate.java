package com.ethan.push.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 消息模板实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("message_template")
public class MessageTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String name;

    /**
     * 当前消息状态：0.待审核 10.审核失败 20.审核成功 30.被删除
     */
    private Integer auditStatus;

    /**
     * 工单ID
     */
    private String flowId;

    /**
     * 当前消息状态：10.新建 20.停用 30.启用 40.等待发送 50.发送中 60.发送成功 70.发送失败
     */
    private Integer msgStatus;

    /**
     * id_type：10.userId 20.did 30.phone 40.openId 50.email
     */
    private Integer idType;

    /**
     * 发送渠道：10.IM 20.Push 30.短信 40.Email 50.公众号 60.小程序 70.企业微信
     */
    private Integer sendChannel;

    /**
     * 模板类型：10.运营类 20.技术类通知 30.福利促销 40.验证码
     */
    private Integer templateType;

    /**
     * 消息类型：10.通知类 20.营销类 30.验证码类
     */
    private Integer msgType;

    /**
     * 屏蔽类型：10.夜间不屏蔽 20.夜间屏蔽
     */
    private Integer shieldType;

    /**
     * 消息内容 占位符用{$var}表示
     */
    private String msgContent;

    /**
     * 发送账号 一个渠道下可存在多个账号
     */
    private Integer sendAccount;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 更新者
     */
    private String updator;

    /**
     * 审核人
     */
    private String auditor;

    /**
     * 涓氬姟鏂瑰洟闃?
     */
    private String team;

    /**
     * 涓氬姟鏂?
     */
    private String proposer;

    /**
     * 鏄惁鍒犻櫎锛?.涓嶅垹闄?1.鍒犻櫎
     */
    private Integer isDeleted;

    /**
     * 鍒涘缓鏃堕棿
     */
    private Integer created;

    /**
     * 鏇存柊鏃堕棿
     */
    private Integer updated;
}

