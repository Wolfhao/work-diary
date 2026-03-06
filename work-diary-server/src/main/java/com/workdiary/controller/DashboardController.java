package com.workdiary.controller;

import com.workdiary.common.api.Result;
import com.workdiary.service.DashboardService;
import com.workdiary.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "首页数据看板聚合接口")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取当前用户的资产与接单数据追踪", description = "聚合计算：总垫付、待收回垫付、预计总收入、已收账等各项资金状态")
    @GetMapping("/stats")
    public Result<DashboardVO> getDashboardStats() {
        DashboardVO stats = dashboardService.getDashboardStats();
        return Result.success(stats);
    }
}
