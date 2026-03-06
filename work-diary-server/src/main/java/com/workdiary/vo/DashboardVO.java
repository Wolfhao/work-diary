package com.workdiary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "数据大盘统计对象")
public class DashboardVO {

    @Schema(description = "总垫付金额", defaultValue = "0.00")
    private BigDecimal totalAdvanceAmount = BigDecimal.ZERO;

    @Schema(description = "已收回垫付金额", defaultValue = "0.00")
    private BigDecimal recoveredAdvanceAmount = BigDecimal.ZERO;

    @Schema(description = "待收回垫付金额", defaultValue = "0.00")
    private BigDecimal pendingAdvanceAmount = BigDecimal.ZERO;

    @Schema(description = "总预计收入", defaultValue = "0.00")
    private BigDecimal totalIncomeAmount = BigDecimal.ZERO;

    @Schema(description = "已到账收入", defaultValue = "0.00")
    private BigDecimal receivedIncomeAmount = BigDecimal.ZERO;

    @Schema(description = "待到账收入", defaultValue = "0.00")
    private BigDecimal pendingIncomeAmount = BigDecimal.ZERO;

    @Schema(description = "历史总接单数", defaultValue = "0")
    private Integer totalOrderCount = 0;

    @Schema(description = "已完成单数", defaultValue = "0")
    private Integer completedOrderCount = 0;

    @Schema(description = "进行中单数", defaultValue = "0")
    private Integer inProgressOrderCount = 0;
}
