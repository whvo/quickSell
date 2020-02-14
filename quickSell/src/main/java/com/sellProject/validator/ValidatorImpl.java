package com.sellProject.validator;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * @author whvo
 * @date 2019/10/21 0021 -10:14
 */
@Component
public class ValidatorImpl implements InitializingBean {

    private Validator validator;

    public ValidationResult validate(Object bean){
        ValidationResult result = new ValidationResult();
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);
        if(constraintViolationSet.size() > 0 ) {
            result.setHasError(true);
            constraintViolationSet.forEach(constraintViolation->{
                String errMsg = constraintViolation.getMessage();
                String propertyName = constraintViolation.getPropertyPath().toString();
                result.getErrorMsgMap().put(propertyName,errMsg );
            });
        }
        return  result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        // 对validator 对象进行工厂初始化
        this.validator = (Validator) Validation.buildDefaultValidatorFactory().getValidator();
    }
}
