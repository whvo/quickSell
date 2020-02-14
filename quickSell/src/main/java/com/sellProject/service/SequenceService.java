package com.sellProject.service;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author whvo
 * @date 2019/10/23 0023 -0:16
 *
 *  订单编号生成器， 由于事务设置的原因：
 *                       因为@Transactional对私有方法无效，所以只能将获取订单编号写成单独service
 *  在OrderServiceImpl.createOrder方法中有调用
 */
public interface SequenceService {
    // 生成订单流水号
     String generateOrderNo();
}
