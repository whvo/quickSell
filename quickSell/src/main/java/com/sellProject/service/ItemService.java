package com.sellProject.service;

import com.sellProject.error.BusinessException;
import com.sellProject.service.model.ItemModel;

import java.util.List;

/**
 * @author whvo
 * @date 2019/10/21 0021 -13:02
 */
public interface ItemService {

    // 创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    // 获取商品列表
    List<ItemModel> listItem();

    //商品详情
    ItemModel getItemById(Integer id);

    //item 以及promo model的缓存模型
    ItemModel getItemByIdInCacha(Integer id);

    // 库存扣减
    boolean decreaseStock(Integer itemId, Integer amount)throws BusinessException ;

    //库存回滚
    boolean increaseStock(Integer itemId,Integer amount);

    //异步扣减库存
    boolean asyncDecreaseStock(Integer itemId,Integer amount);

    // 商品销量增加
    void increaseSales(Integer id,Integer amount);

    // 初始化库存流水
    String initStockLog(Integer itemId,Integer amount);
}

