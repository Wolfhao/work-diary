package com.workdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workdiary.entity.WorkOrder;
import com.workdiary.vo.DashboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商单 Mapper 接口
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    @Select("SELECT " +
            "IFNULL(SUM(advance_amount), 0) AS totalAdvanceAmount, " +
            "IFNULL(SUM(CASE WHEN is_advance_recovered = 1 THEN advance_amount ELSE 0 END), 0) AS recoveredAdvanceAmount, "
            +
            "IFNULL(SUM(CASE WHEN is_advance_recovered = 0 THEN advance_amount ELSE 0 END), 0) AS pendingAdvanceAmount, "
            +
            "IFNULL(SUM(income_amount), 0) AS totalIncomeAmount, " +
            "IFNULL(SUM(CASE WHEN is_income_received = 1 THEN income_amount ELSE 0 END), 0) AS receivedIncomeAmount, " +
            "IFNULL(SUM(CASE WHEN is_income_received = 0 THEN income_amount ELSE 0 END), 0) AS pendingIncomeAmount, " +
            "COUNT(id) AS totalOrderCount, " +
            "SUM(CASE WHEN status = 40 THEN 1 ELSE 0 END) AS completedOrderCount, " +
            "SUM(CASE WHEN status IN (10, 20, 30) THEN 1 ELSE 0 END) AS inProgressOrderCount " +
            "FROM work_order " +
            "WHERE user_id = #{userId} AND is_deleted = 0")
    DashboardVO getDashboardStatsByUserId(@Param("userId") Long userId);
}
