package com.sellProject.controller.viewobject;

import java.io.Serializable;

/**
 * @author whvo
 * @date 2019/10/19 0019 -11:58
 * 如果前端直接将UserModel对象获取， 此对象里面包含的用户密码等敏感数据就会被获取，所以创建此模型对象，
 * 只是为了展示据用户的部分数
 */
public class UserVO implements Serializable {
    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telphone;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getTelphone() {
        return telphone;
    }

    public void setTelphone(String telphone) {
        this.telphone = telphone;
    }
}
