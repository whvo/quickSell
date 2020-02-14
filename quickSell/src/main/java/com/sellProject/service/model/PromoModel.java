package com.sellProject.service.model;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author whvo
 * @date 2019/10/23 0023 -23:30
 */
// 秒杀商品模型
public class PromoModel implements Serializable {
    // id
    private Integer id;

    // 秒杀活动状态 1:未开始 2:进行中 3:已结束
    private Integer status;  // 此字段与数据库无关联

    // 秒杀活动名称
    private String promoName;

    //秒杀活动开始时间
    private DateTime startDate;

    //秒杀活动结束时间
    private DateTime endDate;

    //秒杀活动的使用商品

    private Integer itemId;
    //秒杀活动的商品价格


    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    private BigDecimal promoItemPrice;
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPromoName() {
        return promoName;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(DateTime endDate) {
        this.endDate = endDate;
    }

    public void setPromoName(String promoName) {
        this.promoName = promoName;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getPromoItemPrice() {
        return promoItemPrice;
    }

    public void setPromoItemPrice(BigDecimal promoItemPrice) {
        this.promoItemPrice = promoItemPrice;
    }
}
