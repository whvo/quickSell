package com.sellProject.controller;

import com.sellProject.controller.viewobject.UserVO;
import com.sellProject.error.BusinessException;
import com.sellProject.error.EmBusinessError;
import com.sellProject.response.CommonReturnType;
import com.sellProject.service.model.UserModel;
import com.sellProject.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author whvo
 * @date 2019/10/19 0019 -0:28
 */

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", origins = {"*"})    // 解决ajax跨域请求报错
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;


    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * @Description: 获取用户基本信息接口
     * @Param: [id]
     * @return: UserVO  领域模型，只包含前端需要展示的信息
     */
    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        // 调用service层 返回对应id的userModel对象
        UserModel userModel = userService.getUserByID(id);
        // 如果用户不存在，直接抛出此异常
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_EXITS);
        }
        UserVO userVO = convertFromObject(userModel);
        return CommonReturnType.create(userVO);
    }


    /**
     * @Description: 数据模型转换 userModel --》 UserVO
     * @Param: UserModel
     * @return: UserVO
     */
    private UserVO convertFromObject(UserModel userModel) {
        if (userModel == null) return null;
        UserVO userVo = new UserVO();
        BeanUtils.copyProperties(userModel, userVo);
        return userVo;
    }

    /**
     * @Description: 用户获取OTP短信接口
     * @Param: 用户电话
     * @return:
     */
    @RequestMapping(value = "/getotp", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone") String telphone) {
        // 生成随机数opt验证码(统一设置为12345)
        String otpCode = "12345";

        /*Random random = new Random();
        int randomInt = random.nextInt(99999) + 10000; //生成[10000,109999)的随机数
        String otpCode = String.valueOf(randomInt);
        */

        redisTemplate.opsForValue().set(telphone, otpCode);
        redisTemplate.expire(telphone, 5, TimeUnit.MINUTES); // 失效时间1H

        // 将用户telphone 和 optCode 绑定 ，此处使用 HttpServletRequest  的  Session 进行实现
       // httpServletRequest.getSession().setAttribute(telphone, otpCode);

        // 将用户的opt验证码发送给用户的手机 （省略）
        System.out.println("telphone :" + telphone + " & otpCode: " + otpCode);

        return CommonReturnType.create(null);
    }


    /**
     * @Description: 用户注册接口
     * @Param: 用户基本数据
     * @return:
     */
    @RequestMapping(value = "/register", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone") String telphone,
                                     @RequestParam(name = "otpCode") String otpCode,
                                     @RequestParam(name = "name") String name,
                                     @RequestParam(name = "gender") Integer gender,
                                     @RequestParam(name = "age") Integer age,
                                     @RequestParam(name = "password") String password) throws BusinessException, NoSuchAlgorithmException {
        // 验证手机号和对应的otpCode是否相符合

        String inSessionOtpCode = (String)redisTemplate.opsForValue().get(telphone);
         //String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
//        System.out.println("session中的OTP:"+ inSessionOtpCode);
        if (!StringUtils.equals(inSessionOtpCode, otpCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码不匹配。");
        }
        // 注册
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setAge(age);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setTelphone(telphone);
        userModel.setEncrptPassword(this.EncodeByMd5(password)); // 将密码加密
        userModel.setRegisterMode("ByPhone");
        userService.register(userModel);
        return CommonReturnType.create(null);
    }

    /**
     * @Description: 对密码进行加密（因为jdk自带的MD5加密方式有问题）
     * @Param: 用户的password
     * @return: 加密之后的password
     */
    private String EncodeByMd5(String str) throws NoSuchAlgorithmException {
        //System.out.println("用户的密码："+str);
        // 确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64eNcoder = new BASE64Encoder();
        // 加密字符串
        String newstr = base64eNcoder.encode(md5.digest(str.getBytes()));
       // System.out.println("加密之后："+newstr);
        return newstr;
    }
    
    /**
    * @Description: 用户登录接口 
    * @Param:  
    * @return:  
    */
    @RequestMapping(value = "/login", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telphone") String telphone, @RequestParam(name = "password") String password) throws BusinessException, NoSuchAlgorithmException {
        // 入参校验
        if (StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        // 用户登录， 如果用户登录失败会在userService中抛出异常。
        UserModel userModel = userService.validateLogin(telphone, this.EncodeByMd5(password));

        // 使用uuid取代session ， 生成用户登陆凭证
        String uuid = UUID.randomUUID().toString(); // 并将这个uuid返回给客户端

        redisTemplate.opsForValue().set(uuid, userModel);
        redisTemplate.expire(uuid, 1, TimeUnit.HOURS); // 失效时间1H

        /*// 将登录成功的凭证加入到用户登录的session中
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);
        */

        //下发了token
        return CommonReturnType.create(uuid);
    }
}
