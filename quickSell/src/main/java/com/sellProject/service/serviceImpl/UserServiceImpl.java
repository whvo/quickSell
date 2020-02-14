package com.sellProject.service.serviceImpl;

import com.sellProject.dao.UserDoMapper;
import com.sellProject.dao.UserPasswordDoMapper;
import com.sellProject.dataobject.UserDo;
import com.sellProject.dataobject.UserPasswordDo;
import com.sellProject.error.BusinessException;
import com.sellProject.error.EmBusinessError;
import com.sellProject.service.model.UserModel;
import com.sellProject.service.UserService;
import com.sellProject.validator.ValidationResult;
import com.sellProject.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author whvo
 * @date 2019/10/19 0019 -0:31
 */
@Service
public class UserServiceImpl implements UserService {


    // 注入dao层对象
    @Autowired
    private UserDoMapper userDoMapper;

    @Autowired
    private UserPasswordDoMapper userPasswordDoMapper;

    // 注入数据校验对象
    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;
    /*
        此处的疑问： 为什么要将getUserById方法的返回值设置为 void 呢？
        此处遵守一个原则，不能将service层获取的数据库的映射透传给其他想要此数据的的服务。
        必须要在service层中创建一个mode ，
     */



    @Override
    public UserModel getUserByID(Integer id) {
        // 此处就是调用dao层的userDoMapper来对数据库进行操作
        UserDo userDo = userDoMapper.selectByPrimaryKey(id);
        if(userDo == null ) return null ;

        // 通过用户id在密码表中查找用户的密码
        UserPasswordDo userPasswordDo = userPasswordDoMapper.selectByUserId(userDo.getId());
        // 将用户的所有信息集中在model对象中返回。
        return convertFromDataObject(userDo, userPasswordDo);
    }

    /**
     * @Description: 用户注册
     * @Param:   UserModel
     * @return:  void
     */
    @Override
    @Transactional // 启用事务
    public void register(UserModel userModel) throws BusinessException {
        // 非空校验
        if(userModel == null ) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        //参数校验
       /* if(StringUtils.isEmpty(userModel.getName())
                || userModel.getGender() == null
                || userModel.getAge() == null
                || StringUtils.isEmpty(userModel.getTelphone())
                || StringUtils.isEmpty(userModel.getEncrptPassword())) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR) ;
        }*/
       // 新的参数检验方式
        ValidationResult validate = validator.validate(userModel);
        if(validate.isHasError()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,validate.getErrMsg());
        }

        // 启用事务

        //  将userModel对象 -- >  DataObject 对象
        UserDo userDo =convertFromModel(userModel);

        try{
            userDoMapper.insertSelective(userDo);  // userDo对应数据库中的主键id是自增的，在执行insertActive之后将主键自动更新
        }catch (DuplicateKeyException ex) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号重复注册");
        }

        userModel.setId(userDo.getId());  // 将userDo的自增主键id拿出来放入到userModel中

        UserPasswordDo  userPasswordDo= convertPasswordFromModel(userModel);
        userPasswordDoMapper.insertSelective(userPasswordDo);
        return ;
    }


    @Override
    public UserModel getUserByIdInCache(Integer id) {
        UserModel userModel = (UserModel)redisTemplate.opsForValue().get("user_"+id);
        if(userModel == null ) {
            userModel = this.getUserByID(id);
            redisTemplate.opsForValue().set("user_"+id, userModel);
            redisTemplate.expire("user_"+id, 10, TimeUnit.MINUTES);
        }
        return userModel;
    }


    /**
    * @Description: 用户登录接口
    * @Param:  encrptPassword: 用户输入的加密之后的密码； （注意：此处是先将用户输入的密码进行加密，然后和数据库中的的密码进行比对
    */
    @Override
    public UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException {
        // 通过用户手机获取用户
        UserDo userDo = userDoMapper.selectByTelphone(telphone);
        if(userDo == null ) {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        UserPasswordDo  userPasswordDo = userPasswordDoMapper.selectByUserId(userDo.getId());

        UserModel userModel = this.convertFromDataObject(userDo, userPasswordDo);
        // 对比数据库中加密的密码和用户传进来的密码是否相等
        if(!StringUtils.equals(userModel.getEncrptPassword(), encrptPassword)) {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return  userModel;
    }


    /**
    * @Description:  将用户的基本信息和用户密码信息放入model对象中
    * @Param:  userDo：包含的是用户基本信息 userPassDo： 包含的是用户密码
    * @return:   返回的是一个Model对象，包含用户的所有信息
    */
    private UserModel convertFromDataObject(UserDo userDo , UserPasswordDo userPasswordDo) {
        if(userDo == null ) return null;
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDo, userModel); // 将userDo中的数据拷贝到userModel中
        if(userPasswordDo != null ) {
            userModel.setEncrptPassword(userPasswordDo.getEncrptPassword());
        }
        return userModel;
    }



    /**
    * @Description:  将UserModel 模型 转为 UserDo 模型
    * @Param:   UserModel
    * @return:  UserDo
    */
    private UserDo convertFromModel(UserModel userModel) {
        if(userModel == null ) {
            return null;
        }
        UserDo  userDo = new UserDo();
        BeanUtils.copyProperties(userModel, userDo);
        return userDo;
    }

    /**
    * @Description:  将UserModel转为 UserPasswordDo
    * @Param:   UserModel
    * @return:  UserPasswordDo
    */
    private UserPasswordDo convertPasswordFromModel(UserModel userModel) {
        if(userModel == null ) {
            return null;
        }
        UserPasswordDo userPasswordDo = new UserPasswordDo();
        userPasswordDo.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDo.setUserId(userModel.getId());
        return userPasswordDo;
    }



}
