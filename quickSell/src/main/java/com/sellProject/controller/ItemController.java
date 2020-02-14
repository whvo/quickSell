package com.sellProject.controller;

import com.sellProject.controller.viewobject.ItemVO;
import com.sellProject.dao.PromoDoMapper;
import com.sellProject.error.BusinessException;
import com.sellProject.response.CommonReturnType;
import com.sellProject.service.CacheService;
import com.sellProject.service.ItemService;
import com.sellProject.service.PromoService;
import com.sellProject.service.model.ItemModel;
import com.sellProject.service.model.PromoModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author whvo
 * @date 2019/10/21 0021 -16:38
 */

@Controller("/item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")    // 解决ajax跨域请求报错
public class ItemController extends BaseController {


    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;



    
    /**
    * @Description:  创建商品
    * @Param:
    * @return:
    */
    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "description") String description,
                                       @RequestParam(name = "stock") Integer stock,
                                       @RequestParam(name = "imgUrl") String imgUrl) throws BusinessException {

        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setDescription(description);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn  = itemService.createItem(itemModel);
        ItemVO itemVO = convertVOFromModel(itemModelForReturn);
        return CommonReturnType.create(itemVO);
    }


    /**
    * @Description: 获取商品详情  此处采用多级缓存优化查询
     *                             本地guava缓存 -- > redis缓存 -- >  数据库查询
    */
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id) {

        // 多级缓存，首先在本地热点缓存中查找
        ItemModel itemModel =(ItemModel) cacheService.getCommonCache("item_"+id);

        // 在redis缓存中查找
        if(itemModel == null ) {

            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_"+id);

            // 如果redis中没有对应的itemModel，就在数据库中去查找，并将将数据存入redis和本地热点缓存中
            if (itemModel == null) {
                itemModel = itemService.getItemById(id);
                redisTemplate.opsForValue().set("item_"+id,itemModel ); // 存入缓存
                redisTemplate.expire("item_"+id,10 , TimeUnit.MINUTES); //将失效时间设置为10分钟
            }
            cacheService.setCommonCache("item_"+id, itemModel);
        }
        ItemVO itemVo = convertVOFromModel(itemModel);

        return CommonReturnType.create(itemVo );
    }

    /**
    * @Description:  获取商品列表
    * @Param:
    * @return:
    */
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType itemList(){
        List<ItemModel> itemModels = itemService.listItem();

        List<ItemVO> itemVOS = itemModels.stream().map(itemModel -> {
            ItemVO itemVO = this.convertVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());

        return CommonReturnType.create(itemVOS );
    }

    /**
    * @Description:  将此方法暴露给运营的同时把活动上线
    * @Param:
    * @return:  
    */
    @RequestMapping(value = "/publishpromo", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam(name = "id") Integer promoId){
        promoService.publishPromo(promoId);
        return CommonReturnType.create(null);
    }


    /**
    * @Description: 模型转换  ItemModel --》 ItemVO
    */
    private ItemVO convertVOFromModel(ItemModel itemModel) {
        if (itemModel == null) return null;
        ItemVO itemVo = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVo);
        PromoModel promoModel = itemModel.getPromoModel();
        if(promoModel != null ) {
            // 说明有正在进行的或者还未发生的秒杀活动

            itemVo.setPromoId(promoModel.getId());
            itemVo.setPromoPrice(promoModel.getPromoItemPrice());
            itemVo.setPromoStatus(promoModel.getStatus());
            itemVo.setStartDate(promoModel.getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVo.setEndDate(promoModel.getEndDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }else {
            itemVo.setPromoStatus(0);
        }
        return itemVo;
    }
}
