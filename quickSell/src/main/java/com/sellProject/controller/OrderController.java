package com.sellProject.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.sellProject.error.BusinessException;
import com.sellProject.error.EmBusinessError;
import com.sellProject.mq.MqProducer;
import com.sellProject.response.CommonReturnType;
import com.sellProject.service.ItemService;
import com.sellProject.service.OrderService;
import com.sellProject.service.PromoService;
import com.sellProject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

/**
 * @author whvo
 * @date 2019/10/23 0023 -0:40
 */

@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", origins = {"*"})    // 解决ajax跨域请求报错
public class OrderController extends  BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer producer;


    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;


    private ExecutorService executorService;  // 做秒杀泄洪，往线程池中提交任务，可以提高并发处理事务的能力

    private RateLimiter orderCreateRataLimiter;  // 做限流

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRataLimiter = RateLimiter.create(150);// 限流，一旦并发超过此数值，就会就超出的部分拒绝
    }



    /**
    * @Description: 生成秒杀令牌：  在用户进入商品详情页点击下单后会调用此方法，去生成一个令牌
    * @Param:  商品信息、 活动id， 外加从前端ajax中获取的用户token
    * @return:
    */
    @RequestMapping(value = "/generatetoken", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generatetoken(@RequestParam(name="itemId")Integer itemId,@RequestParam(name="promoId")Integer promoId ) throws BusinessException {
        // 根据token 获取用户登录信息
        String userToken = httpServletRequest.getParameterMap().get("token")[0];

        if(StringUtils.isEmpty(userToken)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录");
        }
        // 获取用户登录信息

        UserModel userModel =(UserModel) redisTemplate.opsForValue().get(userToken);
        if(userModel == null ) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"请重新登录");
        }
        // 获取秒杀令牌
        String promoToken = promoService.generateSeconeKillToken(promoId, itemId, userModel.getId());
        if(promoToken == null ) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"生成令牌失败" );
        }
        return CommonReturnType.create(promoToken);
    }




        /**
        * @Description: 创建订单
        * @Param:  商品ID，活动ID，秒杀令牌（promoToken）， 下单数量
        * @return:
        */
    @RequestMapping(value = "/createorder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody // required = false 表示这个参数不一定会传进来
    public CommonReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                        @RequestParam(name="promoId",required = false)Integer promoId ,
                                        @RequestParam(name="promoToken",required = false)String promoToken,
                                        @RequestParam(name="amount") Integer amount) throws BusinessException { //

        // 获取用户的登录状态
       // HttpSession session = httpServletRequest.getSession();
       // System.out.println("creatOrder:" + session.getAttribute("IS_LOGIN"));
       // System.out.println("creatOrder:" + session.getAttribute("LOGIN_USER"));
        //Boolean is_login = (Boolean)httpServletRequest.getSession().getAttribute("IS_LOGIN");

        // 限流操作，如果返回false，就说明被限流，需要重试
        if(!orderCreateRataLimiter.tryAcquire()) {
            throw new BusinessException(EmBusinessError.RATELIMIT);
        }
        // 验证 用户信息
        //  getItem.html的ajax逻辑中，将当前请求用户的token跟在这个URL的后面，所以此处调用getParameterMap方法在url中获取到token做验证。
        String userToken = httpServletRequest.getParameterMap().get("token")[0];

        if(StringUtils.isEmpty(userToken)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录");
        }

        // 获取用户登录信息  从redis中获取
        UserModel userModel =(UserModel) redisTemplate.opsForValue().get(userToken);
        if(userModel == null ) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"请重新登录");
        }

        //检验秒杀令牌是否正确
        if(promoId != null ) {
            String inCacheToken = (String)redisTemplate.opsForValue().get("promo_token_"+promoId+"userId"+userModel.getId()+"itemId"+itemId);
            if(!StringUtils.equals(inCacheToken, promoToken)){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌验证失败" );
            }
        }
        //  UserModel loginUser = (UserModel)httpServletRequest.getSession().getAttribute("LOGIN_USER");
        //OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId,promoId, amount);


        //同步调用线程池的submit方法，也就是每次只能同时执行指定数目大小的任务，将其余任务放进等待队列
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                // 首先添加库存流水状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                //再去完成对应的下单事务型消息机制
                if (!producer.transcationAsyanResuceStock(userModel.getId(), promoId, itemId, amount, stockLogId)) {
                    throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "下单失败");
                }
                return null;
            }
        });


        try {
            future.get();  // 阻塞自身，等待返回前端结果
        } catch (InterruptedException e) { 
            throw new BusinessException(EmBusinessError.UNKNOW_ERROR );
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOW_ERROR );
        }
        return CommonReturnType.create(null );
    }
}
