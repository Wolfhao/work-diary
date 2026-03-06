package com.workdiary.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.workdiary.mapper.WorkOrderMapper;
import com.workdiary.service.DashboardService;
import com.workdiary.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 数据统计面板服务实现类
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final WorkOrderMapper workOrderMapper;

    @Override
    public DashboardVO getDashboardStats() {
        Long userId = StpUtil.getLoginIdAsLong();

        DashboardVO stats = workOrderMapper.getDashboardStatsByUserId(userId);
        if (stats == null) {
            return new DashboardVO(); // 如果完全没有数据，返回默认值均为0的对象
        }

        return stats;
    }
}
