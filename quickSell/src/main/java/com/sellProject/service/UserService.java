package com.sellProject.service;

import com.sellProject.error.BusinessException;
import com.sellProject.service.model.UserModel;

/**
 * @author whvo
 * @date 2019/10/19 0019 -0:31
 */
public interface UserService {

    /**
     * @Description: 获取用户模型接口
     * @Param: 用户id
     * @return: serivce层模型UserModel
     */
    UserModel getUserByID(Integer id);

    /**
     * @Description: 用户注册接口
     * @Param: UserModel模型
     */
    void register(UserModel userModel) throws BusinessException;

    // 查找用户 缓存--》 数据库
    UserModel getUserByIdInCache(Integer id);

    /**
     * @Description: 用户登录接口
     * @Param: encrptPassword: 用户输入的加密之后的密码
     */
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
