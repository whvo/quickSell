package com.sellProject.service;

import com.sellProject.service.model.PromoModel;

/**
 * @author whvo
 * @date 2019/10/24 0024 -11:08
 */
public interface PromoService {
    // 根据商品ID获取即将进行或者正将进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId );

    // 将商品库存存入缓存中
    void publishPromo(Integer promoId);

    // 生成秒杀令牌
    String generateSeconeKillToken(Integer promoId,Integer itemId,Integer userId);
}
