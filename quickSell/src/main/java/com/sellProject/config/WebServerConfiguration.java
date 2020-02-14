package com.sellProject.config;


import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * @author whvo
 * @date 2019/10/29 0029 -22:33
 *
 * 当Spring容器内没有TomcatEmbeddedServletContainerFactory这个bean时，会把此bean加载进Spring容器中
 */
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        //使用对应工厂类提供给我们的的接口定制化我们的tomcat connector
        ((TomcatServletWebServerFactory)factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                  Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                //定制keepalivetimeout 设置30秒内没有请求则服务端自动断开长连接
                protocol.setKeepAliveTimeout(30000);
                // 一个长连接最多只能请求10000次 ，超过则自动断开
                protocol.setMaxKeepAliveRequests(10000);
            }
        });
    }
}
