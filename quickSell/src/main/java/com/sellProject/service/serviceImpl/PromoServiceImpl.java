package com.sellProject.service.serviceImpl;

import com.sellProject.dao.PromoDoMapper;
import com.sellProject.dataobject.PromoDo;
import com.sellProject.error.BusinessException;
import com.sellProject.error.EmBusinessError;
import com.sellProject.service.PromoService;
import com.sellProject.service.UserService;
import com.sellProject.service.model.ItemModel;
import com.sellProject.service.model.PromoModel;
import com.sellProject.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author whvo
 * @date 2019/10/24 0024 -11:10
 */

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDoMapper promoDoMapper;

    @Autowired
    private ItemServiceImpl itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;
    /**
     * @Description: 获取秒杀商品模型
     */
    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDo promoDo = promoDoMapper.selectByItemId(itemId);
        // 模型转换
        PromoModel promoModel = convertFromDO(promoDo);
        // 判断当前商品的是否参与秒杀活动
        if (promoModel == null) {
            return null;
        }

        // 判断活动状态
        if (promoModel.getStartDate().isAfterNow()) { // startTime > now();
            promoModel.setStatus(1);
        } else if (promoModel.getEndDate().isBeforeNow()) {  // startTime < now()
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }


        return promoModel;
    }

    //运营上线活动
    @Override
    public void publishPromo(Integer promoId) {
        PromoDo promoDo = promoDoMapper.selectByPrimaryKey(promoId);
        if(promoDo.getItemId() == null || promoDo.getItemId().intValue() == 0) {
            return ;
        }
        ItemModel itemModel = itemService.getItemById(promoDo.getItemId());

        // 将库存存入redis中
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(),itemModel.getStock() );

        // 将秒杀大闸限制设置到redis中  ,将闸门流量设置为活动商品的五倍
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock().intValue() * 5 );
    }


    //生成秒杀令牌
    @Override
    public String generateSeconeKillToken(Integer promoId,Integer itemId,Integer userId) {

        // 判断库存是否售罄： 如果redis中存在这个键，就说明该商品售罄
        if(redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)){
            return null;
        }

        PromoDo promoDo = promoDoMapper.selectByPrimaryKey(promoId);
        // 模型转换
        PromoModel promoModel = convertFromDO(promoDo);
        // 判断当前商品的是否参与秒杀活动
        if (promoModel == null) {
            return null;
        }

        // 判断商品信息
        ItemModel itemModel = itemService.getItemByIdInCacha(itemId);// 首先在缓存中查找
        if(itemModel == null ) {
            return null;
        }
        //判断用户信息
        // UserModel userModel = userService.getUserByID(userId);
        UserModel userModel = userService.getUserByIdInCache(userId); // 首先在缓存中查找
        if(userModel == null ) {
            return null;
        }

        // 判断活动状态
        if (promoModel.getStartDate().isAfterNow()) { // startTime > now();  判断活动是否开始
            promoModel.setStatus(1);  // 未开始
        } else if (promoModel.getEndDate().isBeforeNow()) {  // startTime < now() 判断活动是否已经结束
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2); // 说明活动正在进行
        }


        //如果当前活动未进行，便不能生成秒杀令牌
        if(promoModel.getStatus().intValue() != 2){
            return null;
        }

        // 检查秒杀大闸的count数量
        // 对大闸-1，然后检查返回值是否是正数，如果是正数，就说明可以进行下一步
        Long increment = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if(increment < 0) {
            return null; // 否则返回null;
        }

        // 一个秒杀令牌存入缓存，设置失效时间为5分钟。
        String token = UUID.randomUUID().toString().replace("-","" );

        redisTemplate.opsForValue().set("promo_token_"+promoId+"userId"+userId+"itemId"+itemId,token );
        //设置有效时间
        redisTemplate.expire("promo_token_"+promoId+"userId"+userId+"itemId"+itemId,5 , TimeUnit.MINUTES );

        return token;
    }


    // DataObject --->  model
    private PromoModel convertFromDO(PromoDo promoDo) {
        if (promoDo == null) return null;
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDo, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDo.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDo.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDo.getEndDate()));
        return promoModel;
    }


}
