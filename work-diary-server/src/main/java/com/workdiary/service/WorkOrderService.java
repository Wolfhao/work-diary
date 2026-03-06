package com.workdiary.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.workdiary.dto.WorkOrderAddDTO;
import com.workdiary.dto.WorkOrderQueryDTO;
import com.workdiary.dto.WorkOrderUpdateDTO;
import com.workdiary.entity.WorkOrder;
import com.workdiary.vo.WorkOrderVO;

/**
 * 商单服务接口
 */
public interface WorkOrderService extends IService<WorkOrder> {

    /**
     * 新增商单
     */
    boolean addWorkOrder(WorkOrderAddDTO dto);

    /**
     * 修改商单
     */
    boolean updateWorkOrder(WorkOrderUpdateDTO dto);

    /**
     * 删除商单
     */
    boolean deleteWorkOrder(Long id);

    /**
     * 商单详情
     */
    WorkOrderVO getWorkOrderDetail(Long id);

    /**
     * 分页查询当前用户的商单
     */
    Page<WorkOrderVO> pageWorkOrders(WorkOrderQueryDTO queryDTO);
}
