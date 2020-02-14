package com.sellProject.service.serviceImpl;

import com.sellProject.dao.ItemDoMapper;
import com.sellProject.dao.ItemStockDoMapper;
import com.sellProject.dao.StockLogDoMapper;
import com.sellProject.dataobject.ItemDo;
import com.sellProject.dataobject.ItemStockDo;
import com.sellProject.dataobject.StockLogDo;
import com.sellProject.error.BusinessException;
import com.sellProject.error.EmBusinessError;
import com.sellProject.mq.MqProducer;
import com.sellProject.service.ItemService;
import com.sellProject.service.PromoService;
import com.sellProject.service.model.ItemModel;
import com.sellProject.service.model.PromoModel;
import com.sellProject.validator.ValidationResult;
import com.sellProject.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author whvo
 * @date 2019/10/21 0021 -13:06
 */
@Service
public class ItemServiceImpl implements ItemService {


    // 注入数据校验对象
    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDoMapper itemDoMapper;

    @Autowired
    private ItemStockDoMapper itemStockDoMapper;

    @Autowired
    private PromoService promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer producer;

    @Autowired
    private StockLogDoMapper stockLogDoMapper;

    /**
     * @Description: 创建商品
     * @Param:
     * @return:
     */
    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        // 入参校验
        ValidationResult validate = validator.validate(itemModel);
        if (validate.isHasError()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validate.getErrMsg());
        }
        // 模型转换 ItemoModel --> itemDO
        ItemDo itemDo = this.convertFromModel(itemModel);

        // 写入数据库
        itemDoMapper.insertSelective(itemDo);

        itemModel.setId(itemDo.getId());

        // 模型转换 ItemoStockModel --> itemStockDO
        ItemStockDo itemStockDo = this.convertStockFromModel(itemModel);

        itemStockDoMapper.insertSelective(itemStockDo);

        // 返回对象
        return this.getItemById(itemModel.getId());
    }

    // 获得商品列表
    @Override
    public List<ItemModel> listItem() {
        List<ItemDo> itemDos = itemDoMapper.itemList();

        // 此处使用java8特性之一的流式遍历集合
        List<ItemModel> itemModels =  itemDos.stream().map(itemDo -> {
            ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(itemDo.getId());
            ItemModel itemModel = this.convertFromDataObject(itemDo, itemStockDo);
            return itemModel;
        }).collect(Collectors.toList());

        return itemModels;
    }

    /**
    * @Description: 获得商品详情（包括对该商品是否参加了秒杀活动的判断）
    */
    @Override
    public ItemModel getItemById(Integer id) {
        ItemDo itemDo = this.itemDoMapper.selectByPrimaryKey(id);
        if (itemDo == null) return null;
        // 获取库存数量
        ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(itemDo.getId());
        ItemModel itemModel = this.convertFromDataObject(itemDo, itemStockDo);
        PromoModel promoByItemId = promoService.getPromoByItemId(itemModel.getId());
        // 判断当前商品是否有正在进行或者未开始的秒杀活动
        if(promoByItemId != null && promoByItemId.getStatus().intValue() != 3) {
            itemModel.setPromoModel(promoByItemId);
        }
        return itemModel;
    }

    
    /**
    * @Description:  从缓存里面获取item以及promo模型
    */
    @Override
    public ItemModel getItemByIdInCacha(Integer id) {
        ItemModel itemModel=(ItemModel)redisTemplate.opsForValue().get("item_validate_"+id);
        if(itemModel == null ){
             itemModel = this.getItemById(id);
             redisTemplate.opsForValue().set("item_validate"+id,itemModel);
             redisTemplate.expire("item_validate"+id, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }


    /**
    * @Description: 库存扣减实现
    * @Param:  商品编号， 扣减数量
    * @return:   是否成功
    */
    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount)  {
        //返回的是受影响的行数
        //int row = itemStockDoMapper.decreaseStock(itemId, amount);
        long  row = redisTemplate.opsForValue().increment("promo_item_stock_"+itemId,amount.intValue() * -1);
        if(row > 0 ) {
            return true;
        }else if(row == 0) {
            // 已经售罄, 在缓存中打上标签
            redisTemplate.opsForValue().set("promo_item_stock_invalid_"+itemId,"true" );

            return true;
        }else{
            // 更新失败，库存回滚
            increaseStock(itemId,amount);
            return false;
        }
    }

    // 库存回滚
    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {
        // increment  就是 “加” 操作
        redisTemplate.opsForValue().increment("promo_item_stock_"+itemId,amount.intValue());
        return false;
    }


    // 异步扣减库存
    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        Boolean aBoolean = producer.asyncReduceStock(itemId, amount);
        return aBoolean;
    }

    /**
    * @Description: 将该参加活动的商品的库存信息放入缓存
    */
    @Override
    @Transactional
    public void increaseSales(Integer id, Integer amount) {
         itemDoMapper.increaseSales(id, amount);
    }

    
    /**
    * @Description:  初始化库存流水 
    * @Param:  
    * @return:  
    */
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDo stockLogDo = new StockLogDo();
        stockLogDo.setItemId(itemId);
        stockLogDo.setAmount(amount);
        stockLogDo.setStockLogId(UUID.randomUUID().toString().replace("-",""));
        stockLogDo.setStatus(1);

        stockLogDoMapper.insertSelective(stockLogDo);
        return stockLogDo.getStockLogId();
    }


    /**
     * @Description: itemModel --> ItemDO 对象
     */
    private ItemDo convertFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemDo itemDo = new ItemDo();
        BeanUtils.copyProperties(itemModel, itemDo);

        // 因为数据库中price 是decimal类型 , 和ItempDo中的priec 类型不匹配，所以单独设置
        itemDo.setPrice(itemModel.getPrice().doubleValue());

        return itemDo;
    }

    /**
     * @Description: 将ItemModel --> ItemStockDo 对象
     */
    private ItemStockDo convertStockFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemStockDo itemStockDo = new ItemStockDo();
        itemStockDo.setItemId(itemModel.getId());
        itemStockDo.setStock(itemModel.getStock());
        return itemStockDo;
    }

    /**
     * @Description: DO --> MODEL
     * @Param: itemDo + itemStockDo = itemModel
     * @return:
     */
    private ItemModel convertFromDataObject(ItemDo itemDo, ItemStockDo itemStockDo) {
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDo, itemModel);
        // 价格 类型特别转换
        itemModel.setPrice(new BigDecimal(itemDo.getPrice()));
        // stock设置
        itemModel.setStock(itemStockDo.getStock());
        return itemModel;
    }
}
