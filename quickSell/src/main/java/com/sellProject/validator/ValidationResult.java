package com.sellProject.validator;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author whvo
 * @date 2019/10/21 0021 -9:58
 *
 * 参数校验的工具类
 */
public class ValidationResult {

    //是否有错误
    private boolean hasError = false ;

    // 存放错误信息
    private Map<String , String > errorMsgMap;



    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public Map<String, String> getErrorMsgMap() {
        return errorMsgMap;
    }

    public void setErrorMsgMap(Map<String, String> errorMsgMap) {
        this.errorMsgMap = errorMsgMap;
    }

    // 获取错误信息
    public String getErrMsg () {
        return StringUtils.join(errorMsgMap.values().toArray(),",");
    }

}
