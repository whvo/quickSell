package com.sellProject.service.serviceImpl;

import com.sellProject.dao.*;
import com.sellProject.dataobject.OrderDo;
import com.sellProject.dataobject.SequenceDo;
import com.sellProject.dataobject.StockLogDo;
import com.sellProject.error.BusinessException;
import com.sellProject.error.EmBusinessError;
import com.sellProject.service.OrderService;
import com.sellProject.service.model.ItemModel;
import com.sellProject.service.model.OrderModel;
import com.sellProject.service.model.UserModel;
import com.sellProject.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;


/**
 * @author whvo
 * @date 2019/10/22 0022 -12:46
 */
@Service
public class OrderServiceImpl implements OrderService {



    @Autowired
    private ItemDoMapper itemDoMapper;

    @Autowired
    private ItemStockDoMapper itemStockDoMapper;

    @Autowired
    private ItemServiceImpl itemService;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private OrderDoMapper orderDoMapper;

    @Autowired
    private SequenceDoMapper sequenceDoMapper;

    @Autowired
    private SequenceServiceImpl sequenceService;

    @Autowired
    private StockLogDoMapper stockLogDoMapper;


    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId, Integer amount,String stockLogId) throws BusinessException {

        //1、 检查当前商品是否存在，用户是否合法，购买数量是否正确
                // 已经在获取秒杀令牌时校验
//         ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCacha(itemId);// 首先在缓存中查找
        if(itemModel == null ) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
//
//        // UserModel userModel = userService.getUserByID(userId);
//        UserModel userModel = userService.getUserByIdInCache(userId); // 首先在缓存中查找
//        if(userModel == null ) {
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
//        }

        if(amount <= 0 || amount > 99 ) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }
//        //校验活动信息 ： 同样也在获取秒杀令牌时校验过
//        if(promoId != null ) {
//            //(1) 校验当前活动是否对应商品
//            if(promoId != itemModel.getPromoModel().getId()) {
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
//            }else {
//             //(2) 校验活动是否正在进行中
//                if(itemModel.getPromoModel().getStatus()!= 2) {
//                    throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动状态错误");
//                }
//            }
//        }
        //2、落单减库存：  减的是reids中的库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if(!result) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUTH);
        }

        //3 、 订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        if(promoId != null ) {  //如果是秒杀商品，则保存的是活动价格
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else {// 如果是平销商品，保存的则是普通价格
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setAmount(amount);
        orderModel.setPromoId(promoId);

        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
            //4、 生成交易流水号（订单号）

        orderModel.setId(sequenceService.generateOrderNo());

        orderDoMapper.insertSelective(convertFromModel(orderModel));

            ///5、商品销量增加
        itemService.increaseSales(itemId,amount);

        // 设置库存流水状态为成功
        StockLogDo stockLogDo = stockLogDoMapper.selectByPrimaryKey(stockLogId);
        if(stockLogDo == null ) {
            throw  new BusinessException(EmBusinessError.UNKNOW_ERROR );
        }
        stockLogDo.setStatus(2);
        stockLogDoMapper.updateByPrimaryKeySelective(stockLogDo);
        // 此处将异步更新库存的消息留在最后才发送，是为了：如果前面的操作出现问题，只用回滚redis中的库存数据，而不用回滚数据库中的数据。
            // 从而避免了由于先执行了库存更新，而后面的操作失败导致下单失败，从而导致数据库中的库存无法回滚

        ///6、异步更新库存
        // 由于SpringBoot的事务是在所在方法执行完之后才会commit，为了让异步更新库存在最后操作，使用springboot自带的机制
      /*  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {  // 此方法会在事务commit之后才会执行
                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
                *//*if(!mqResult) {
                    itemService.increaseStock(itemId, amount);
                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
                }     *//*
            }
        });
        */

        // 7、 返回前端
        return orderModel;
    }

    /**
    * @Description:  模型转换  Model ---> OrderDo ;
    */
    private OrderDo convertFromModel(OrderModel orderModel) {
        if(orderModel == null ) return null;
        OrderDo orderDo = new OrderDo();
        BeanUtils.copyProperties(orderModel,orderDo );
        orderDo.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDo.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDo;
    }

}
