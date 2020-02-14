package com.sellProject;

import com.sellProject.dao.UserDoMapper;
import com.sellProject.dataobject.UserDo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello world!
 */
@SpringBootApplication(scanBasePackages = {"com.sellProject"})
@RestController
@MapperScan("com.sellProject.dao")

public class App {

    @Autowired
    private UserDoMapper userDoMapper;

    @RequestMapping("/")
    public String home() {
        UserDo userDo = userDoMapper.selectByPrimaryKey(1);
        if (userDo == null) {
            return "抱歉，星玲查不到该用户";
        }
        return userDo.getName();
    }


    public static void main(String[] args) {
        System.out.println("Hello World!");
        SpringApplication.run(App.class, args); // springBoot启动， 8080端口默认被监听
    }
}
