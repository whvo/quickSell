package com.sellProject.response;

/**
 * @author whvo
 * @date 2019/10/19 0019 -13:28
 *  通用信息处理类
 */
public class CommonReturnType {

    /*
        返回success  或者 fail
     */
    private String status; // 返回的状态

    /*
        如果 status 是 success  则data返回的是正确的的json数据
        如果 status 是 fail     则data是通用的错误信息
     */
    private Object data;

    // 如果不带状态参数 直接创建的话默认是成功状态
    public static CommonReturnType create(Object result) {
        return CommonReturnType.create(result,"success");
    }

    public static CommonReturnType create(Object result , String status) {
        CommonReturnType type = new CommonReturnType();
        type.setStatus(status);
        type.setData(result);
        return type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
