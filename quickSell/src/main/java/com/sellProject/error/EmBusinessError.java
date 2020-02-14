package com.sellProject.error;

/**
 * @author whvo
 * @date 2019/10/19 0019 -14:30
 * 通用错误信息处理类
 */
public enum EmBusinessError implements CommonError {
    // 10000开头  通用错误类型
    PARAMETER_VALIDATION_ERROR(10001,"参数不合法"),
    UNKNOW_ERROR(10002,"未知错误"),

    // 20000开头 为 用户错误信息相关定义
    USER_NOT_EXITS(20001,"用户不存在"),
    USER_LOGIN_FAIL(20002,"用户手机号或者密码错误"),
    USER_NOT_LOGIN(20003,"用户未登录"),
    // 30000开头为交易信息错误
    STOCK_NOT_ENOUTH(30001,"库存不足"),
    MQ_SEND_FAIL(30002,"库存异步消息发送失败"),
    RATELIMIT(30003,"当前参与人数过多，请刷新重试")
    ;

    private EmBusinessError(int errCode,String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    private int errCode;
    private String errMsg;

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errorMsg) {
        this.errMsg = errorMsg;
        return this;
    }
}
