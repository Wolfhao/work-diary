package com.workdiary.service;

import com.workdiary.vo.DashboardVO;

/**
 * 数据统计面板服务接口
 */
public interface DashboardService {

    /**
     * 获取当前用户的数据大盘统计信息
     */
    DashboardVO getDashboardStats();
}
