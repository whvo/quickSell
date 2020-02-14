package com.sellProject.service.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author whvo
 * @date 2019/10/22 0022 -11:57
 */
// 交易订单模型
public class OrderModel implements Serializable {

    //订单编号
    private String id;

    //购买者的id
    private Integer userId;

    //商品id
    private Integer itemId;

    //秒杀商品ID，如果非空，则说明当前商品是秒杀商品
    private Integer promoId;


    //商品单价 如果promoId非空， 则意味着是秒杀商品的价格
    private BigDecimal itemPrice;

    //购买的数量
    private Integer amount;

    //购买的总金额
    private BigDecimal orderPrice;

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public BigDecimal getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(BigDecimal orderAmount) {
        this.orderPrice = orderAmount;
    }

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }
}
