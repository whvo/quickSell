package com.sellProject.error;

/**
 * @author whvo
 * @date 2019/10/19 0019 -13:59
 */
public interface CommonError {
    public int getErrCode();
    public String getErrMsg();
    public CommonError setErrMsg(String errorMsg);

}
