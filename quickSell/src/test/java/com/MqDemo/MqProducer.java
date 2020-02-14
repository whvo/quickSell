package com.MqDemo;


import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * @author whvo
 * @date 2019/11/8 0008 -22:14
 */
public class MqProducer {

    public static void main(String[] args) {
        DefaultMQProducer producer = new DefaultMQProducer("producer1");

        //设置NameServer地址,此处应改为实际NameServer地址，多个地址之间用；分隔
        //NameServer的地址必须有，但是也可以通过环境变量的方式设置，不一定非得写死在代码里
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.setSendMsgTimeout(10000); // 默认的超时时间是3s， 但是因为我的网络带宽不够，必须设置大一些，不然会报超时异常。
        //调用start()方法启动一个producer实例
        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }

        //发送10条消息到Topic为TopicTest，tag为TagA，消息内容为“Hello RocketMQ”拼接上i的值
        for (int i = 0; i < 10; i++) {
            try {
                Message msg = new Message("TopicTest",// topic
                        "TagA",// tag
                        ("Hello RocketMQ " + i).getBytes("utf-8")// body
                );

                //调用producer的send()方法发送消息
                //这里调用的是同步的方式，所以会有返回结果
                SendResult sendResult = producer.send(msg);
               // System.out.println(sendResult.getSendStatus()); //发送结果状态
                //打印返回结果，可以看到消息发送的状态以及一些相关信息
                System.out.println(sendResult);
            } catch(MQClientException e) {
                e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        producer.shutdown();
    }
}
