package com.sellProject.service;

import com.sellProject.error.BusinessException;
import com.sellProject.service.model.OrderModel;

/**
 * @author whvo
 * @date 2019/10/22 0022 -12:44
 */
public interface OrderService {
    /**
    * @Description:  创建订单
    * @Param:  用户id，商品id，购买数量
    * @return:  订单模型
    */
    OrderModel createOrder(Integer userId,Integer itemId,Integer promoId,Integer amount,String stockLogId) throws BusinessException;
}
