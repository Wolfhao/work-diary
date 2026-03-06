package com.workdiary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "商单详情展示对象")
public class WorkOrderVO {

    @Schema(description = "商单ID")
    private Long id;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "商单名称/合作项目名")
    private String title;

    @Schema(description = "商单描述/要求备注")
    private String description;

    @Schema(description = "发布平台")
    private String platform;

    @Schema(description = "商单相关截图URL集合")
    private List<String> imageUrls;

    @Schema(description = "需要垫付的金额")
    private BigDecimal advanceAmount;

    @Schema(description = "垫付是否已收回(0:未收回, 1:已收回)")
    private Integer isAdvanceRecovered;

    @Schema(description = "垫付实际收回的时间")
    private LocalDateTime advanceRecoverTime;

    @Schema(description = "商单收入/酬金")
    private BigDecimal incomeAmount;

    @Schema(description = "收入是否已到账(0:未到账, 1:已到账)")
    private Integer isIncomeReceived;

    @Schema(description = "收入实际到账的时间")
    private LocalDateTime incomeReceiveTime;

    @Schema(description = "商单状态(1:沟通中/待执行, 2:执行中, 3:待结算, 4:已完成, 9:已取消)")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
