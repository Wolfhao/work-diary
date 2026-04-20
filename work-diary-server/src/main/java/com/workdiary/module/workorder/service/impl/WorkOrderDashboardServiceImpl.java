package com.workdiary.module.workorder.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.workdiary.module.workorder.mapper.WorkOrderMapper;
import com.workdiary.module.workorder.service.WorkOrderDashboardService;
import com.workdiary.module.workorder.vo.WorkOrderDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商单看板统计服务实现类
 */
@Service
@RequiredArgsConstructor
public class WorkOrderDashboardServiceImpl implements WorkOrderDashboardService {

    private final WorkOrderMapper workOrderMapper;

    @Override
    public WorkOrderDashboardVO getDashboardStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        WorkOrderDashboardVO stats = workOrderMapper.getDashboardStatsByUserId(userId);
        return stats != null ? stats : new WorkOrderDashboardVO();
    }
}
