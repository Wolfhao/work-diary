package com.workdiary.module.workorder.controller;

import com.workdiary.common.api.Result;
import com.workdiary.module.workorder.service.WorkOrderDashboardService;
import com.workdiary.module.workorder.vo.WorkOrderDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "WorkOrder Dashboard", description = "商单数据看板聚合接口")
public class WorkOrderDashboardController {

    private final WorkOrderDashboardService workOrderDashboardService;

    @Operation(summary = "获取当前用户的资产与接单数据追踪", description = "聚合计算：总垫付、待收回垫付、预计总收入、已收账等各项资金状态")
    @GetMapping("/stats")
    public Result<WorkOrderDashboardVO> getDashboardStats() {
        WorkOrderDashboardVO stats = workOrderDashboardService.getDashboardStats();
        return Result.success(stats);
    }
}
