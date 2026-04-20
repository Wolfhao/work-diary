package com.workdiary.module.workorder.service;

import com.workdiary.module.workorder.vo.WorkOrderDashboardVO;

/**
 * 商单看板统计服务接口
 */
public interface WorkOrderDashboardService {

    WorkOrderDashboardVO getDashboardStats();
}
