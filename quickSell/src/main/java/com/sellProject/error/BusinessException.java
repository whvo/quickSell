package com.sellProject.error;

/**
 * @author whvo
 * @date 2019/10/19 0019 -14:43
 *
 *  使用包装器设计模式实现业务异常
 */
public class BusinessException  extends  Exception implements  CommonError {

    private CommonError commonError;

    // 直接接受CommonError的传参用于构建业务异常
    public BusinessException(CommonError commonError) {
        super();
        this.commonError = commonError;
    }
    // 接收自定义errMsg的方式直接构建异常
    public BusinessException(CommonError commonError,String errMsg) {
        super();
        this.commonError = commonError;
        this.commonError.setErrMsg(errMsg);
    }




    @Override
    public int getErrCode() {
        return this.commonError.getErrCode();
    }

    @Override
    public String getErrMsg() {
        return this.commonError.getErrMsg();
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.commonError.setErrMsg(errMsg);
        return this;
    }
}
