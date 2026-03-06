package com.workdiary.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workdiary.common.exception.ApiException;
import com.workdiary.dto.WorkOrderAddDTO;
import com.workdiary.dto.WorkOrderQueryDTO;
import com.workdiary.dto.WorkOrderUpdateDTO;
import com.workdiary.entity.WorkOrder;
import com.workdiary.mapper.WorkOrderMapper;
import com.workdiary.service.WorkOrderService;
import com.workdiary.vo.WorkOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商单服务实现类
 */
@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements WorkOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWorkOrder(WorkOrderAddDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        WorkOrder workOrder = new WorkOrder();
        BeanUtil.copyProperties(dto, workOrder);
        workOrder.setUserId(userId);

        // 设置初始状态
        workOrder.setStatus(10); // 10:待开工
        workOrder.setIsAdvanceRecovered(0);
        workOrder.setIsIncomeReceived(0);

        return this.save(workOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWorkOrder(WorkOrderUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        WorkOrder existingOrder = this.getById(dto.getId());

        if (existingOrder == null || !existingOrder.getUserId().equals(userId)) {
            throw new ApiException("商单不存在或无权操作");
        }

        WorkOrder updateEntity = new WorkOrder();
        BeanUtil.copyProperties(dto, updateEntity);

        // 如果标记垫付已收回，并且原先未收回，则记录时间
        if (dto.getIsAdvanceRecovered() != null && dto.getIsAdvanceRecovered() == 1
                && existingOrder.getIsAdvanceRecovered() == 0) {
            updateEntity.setAdvanceRecoverTime(LocalDateTime.now());
        }

        // 如果标记收入已到账，并且原先未到账，则记录时间
        if (dto.getIsIncomeReceived() != null && dto.getIsIncomeReceived() == 1
                && existingOrder.getIsIncomeReceived() == 0) {
            updateEntity.setIncomeReceiveTime(LocalDateTime.now());
        }

        // 状态流转规则：到达 30(待结款) 时，如果垫付已收回(或无需垫付) 且 尾款已收回(或无尾款)，自动变更为主状态 40(已完成)
        Integer targetStatus = dto.getStatus() != null ? dto.getStatus() : existingOrder.getStatus();
        if (targetStatus != null && targetStatus == 30) {
            java.math.BigDecimal currentAdvance = existingOrder.getAdvanceAmount();
            boolean isAdvanceOk = (currentAdvance == null || currentAdvance.compareTo(java.math.BigDecimal.ZERO) <= 0)
                    ||
                    ((dto.getIsAdvanceRecovered() != null ? dto.getIsAdvanceRecovered()
                            : existingOrder.getIsAdvanceRecovered()) == 1);

            java.math.BigDecimal currentIncome = existingOrder.getIncomeAmount();
            boolean isIncomeOk = (currentIncome == null || currentIncome.compareTo(java.math.BigDecimal.ZERO) <= 0) ||
                    ((dto.getIsIncomeReceived() != null ? dto.getIsIncomeReceived()
                            : existingOrder.getIsIncomeReceived()) == 1);

            if (isAdvanceOk && isIncomeOk) {
                updateEntity.setStatus(40); // 满足核销条件，自动标记已完成
            }
        }

        return this.updateById(updateEntity);
    }

    @Override
    public boolean deleteWorkOrder(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        WorkOrder existingOrder = this.getById(id);

        if (existingOrder == null || !existingOrder.getUserId().equals(userId)) {
            throw new ApiException("商单不存在或无权操作");
        }

        return this.removeById(id);
    }

    @Override
    public WorkOrderVO getWorkOrderDetail(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        WorkOrder existingOrder = this.getById(id);

        if (existingOrder == null || !existingOrder.getUserId().equals(userId)) {
            throw new ApiException("商单不存在或无权操作");
        }

        WorkOrderVO vo = new WorkOrderVO();
        BeanUtil.copyProperties(existingOrder, vo);
        return vo;
    }

    @Override
    public Page<WorkOrderVO> pageWorkOrders(WorkOrderQueryDTO queryDTO) {
        Long userId = StpUtil.getLoginIdAsLong();

        Page<WorkOrder> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(WorkOrder::getUserId, userId)
                .like(StrUtil.isNotBlank(queryDTO.getTitle()), WorkOrder::getTitle, queryDTO.getTitle())
                .eq(queryDTO.getStatus() != null, WorkOrder::getStatus, queryDTO.getStatus())
                .in(queryDTO.getStatuses() != null && !queryDTO.getStatuses().isEmpty(), WorkOrder::getStatus,
                        queryDTO.getStatuses())
                .eq(queryDTO.getIsAdvanceRecovered() != null, WorkOrder::getIsAdvanceRecovered,
                        queryDTO.getIsAdvanceRecovered())
                .eq(queryDTO.getIsIncomeReceived() != null, WorkOrder::getIsIncomeReceived,
                        queryDTO.getIsIncomeReceived())
                .orderByDesc(WorkOrder::getCreateTime);

        Page<WorkOrder> resultPage = this.page(page, queryWrapper);

        Page<WorkOrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());

        List<WorkOrderVO> voList = resultPage.getRecords().stream().map(order -> {
            WorkOrderVO vo = new WorkOrderVO();
            BeanUtil.copyProperties(order, vo);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}
