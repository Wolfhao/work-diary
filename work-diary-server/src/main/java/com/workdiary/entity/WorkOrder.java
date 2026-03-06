package com.workdiary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商单实体类
 */
@Data
@Accessors(chain = true)
@TableName(value = "work_order", autoResultMap = true)
public class WorkOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID(关联user.id)
     */
    private Long userId;

    /**
     * 商单名称/合作项目名
     */
    private String title;

    /**
     * 商单描述/要求备注
     */
    private String description;

    /**
     * 发布平台(例如:小红书,抖音,B站,微博)
     */
    private String platform;

    /**
     * 商单相关截图URL集合(JSON数组)
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object imageUrls;

    /**
     * 需要垫付的金额(0代表无需垫付)
     */
    private BigDecimal advanceAmount;

    /**
     * 垫付是否已收回(0:未收回, 1:已收回)
     */
    private Integer isAdvanceRecovered;

    /**
     * 垫付实际收回的时间
     */
    private LocalDateTime advanceRecoverTime;

    /**
     * 商单收入/酬金
     */
    private BigDecimal incomeAmount;

    /**
     * 收入是否已到账(0:未到账, 1:已到账)
     */
    private Integer isIncomeReceived;

    /**
     * 收入实际到账的时间
     */
    private LocalDateTime incomeReceiveTime;

    /**
     * 商单状态(10:待开工, 20:制作中, 30:待结款, 40:已完成, 90:已取消)
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
