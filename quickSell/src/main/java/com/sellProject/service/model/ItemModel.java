package com.sellProject.service.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author whvo
 * @date 2019/10/21 0021 -11:59
 *
 * 商品领域模型
 */

public class ItemModel implements Serializable {
    // 商品ID
    private Integer id;

    //商品名称
    @NotBlank(message = "商品名不能为空")
    private String title;

    //商品价格
    @NotNull(message = "商品价格不能为空")
    @Min(value = 0,message = "商品价格最低为0")
    private BigDecimal price;

    //商品库存
    @NotNull(message = "商品库存不能为空")
    private Integer stock;

    //商品描述
    @NotNull(message = "商品描述不能为空")
    private String description;

    //商品销量
    private Integer sales;

    //商品图片URL
    @NotNull(message = "商品图片信息不能为空")
    private String imgUrl;


    // 使用聚合模型 ：
    // promoModel对象来记录当前商品是否是参加秒杀活动的商品
    // 如果PromoModel不为空，则说明当前商品正在参加秒杀活动或者秒杀活动还未开始
    private PromoModel promoModel;


    public PromoModel getPromoModel() {
        return promoModel;
    }

    public void setPromoModel(PromoModel promoModel) {
        this.promoModel = promoModel;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
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
}
