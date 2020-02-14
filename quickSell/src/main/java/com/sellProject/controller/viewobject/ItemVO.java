package com.sellProject.controller.viewobject;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author whvo
 * @date 2019/10/21 0021 -16:39
 */
public class ItemVO implements Serializable {
    // 商品ID
    private Integer id;

    //商品名称
    private String title;

    //商品价格
    private BigDecimal price;

    //商品库存
    private Integer stock;

    //商品描述
    private String description;

    //商品销量
    private Integer sales;

    //商品图片URL
    private String imgUrl;

    //商品是否参加秒杀活动以及状态 0: 未参加 1 ：还未开始 2：正在进行
    private Integer promoStatus ;

    // 秒杀活动价格
    private BigDecimal promoPrice;

    // 秒杀活动ID
    private Integer promoId;

    //秒杀活动开始时间
    private String startDate;

    //秒杀活动结束时间
    private String endDate;


    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getPromoStatus() {
        return promoStatus;
    }

    public void setPromoStatus(Integer promoStatus) {
        this.promoStatus = promoStatus;
    }

    public BigDecimal getPromoPrice() {
        return promoPrice;
    }

    public void setPromoPrice(BigDecimal promoPrice) {
        this.promoPrice = promoPrice;
    }

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
