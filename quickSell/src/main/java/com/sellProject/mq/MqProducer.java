package com.sellProject.mq;

import com.alibaba.fastjson.JSON;

import com.sellProject.dao.StockLogDoMapper;
import com.sellProject.dataobject.StockLogDo;
import com.sellProject.error.BusinessException;
import com.sellProject.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author whvo
 * @date 2019/11/8 0008 -16:30
 */
@Component
public class MqProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionProducer;


    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private OrderService orderService;


    @Autowired
    private StockLogDoMapper stockLogDoMapper;
    // 初始化配置
    @PostConstruct
    public void init() throws MQClientException {
        //做mq producer的初始化
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.setSendMsgTimeout(10000);
        producer.start();

        transactionProducer = new TransactionMQProducer("transaction_producer_groupe");
        transactionProducer.setNamesrvAddr(nameAddr);
        transactionProducer.setSendMsgTimeout(10000);
        transactionProducer.start();

        transactionProducer.setTransactionListener(new TransactionListener() {
            // 事务监听器会监听本地事务的执行，如下：要进行落单减缓存中的库存
            // 如果本地事务执行完成未抛出异常，则会返回事务提交状态至broker上，broker通知消费者消费
            // 如果本地事务执行抛出异常，会返回事务回滚状至broker，broker上的消息会被丢失
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object o) {
                Integer itemId = (Integer) ((Map)o).get("itemId");
                Integer userId = (Integer) ((Map)o).get("userId");
                Integer promoId = (Integer) ((Map)o).get("promoId");
                Integer amount = (Integer) ((Map)o).get("amount");
                String stockLogId = (String)((Map)o).get("stockLogId");

                try {
                    // 真正执行本地的事务型操作
                    orderService.createOrder(userId,itemId,promoId,amount,stockLogId);

                } catch (BusinessException e) {
                    e.printStackTrace();
                    //一旦本地事务出错，就要设置对应的stockLogStatus为回滚状态
                    StockLogDo stockLogDo = stockLogDoMapper.selectByPrimaryKey(stockLogId);
                    stockLogDo.setStatus(3);  //3 代表出错回滚
                    stockLogDoMapper.updateByPrimaryKeySelective(stockLogDo);
                    return LocalTransactionState.ROLLBACK_MESSAGE;  // 出了异常就进行回滚
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            // 如果本地事务执行超时或者被阻塞，broker会发起回调信号，producer会调用该方法，
            // 根据自定义逻辑判断本地事务的执行情况，然后返回事务的执行状态给broker
            //
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
                // 本地事务被阻塞时 broker就会发起回调执行本方法，逻辑就是去检查交易流水中记录中的状态来判断。
                String jsonStr  =  new String(messageExt.getBody());
                Map<String ,Object> map = JSON.parseObject(jsonStr, Map.class);
                Integer itemId =(Integer) map.get("itemId");
                Integer amount =(Integer) map.get("amount");
                String stockLogId =(String) map.get("stockLogId");
                StockLogDo stockLogDo = stockLogDoMapper.selectByPrimaryKey(stockLogId);
                if( stockLogDo == null) {
                    return LocalTransactionState.UNKNOW;
                }else if(stockLogDo.getStatus() ==2 ) {  // 2代表可以提交事务
                    return LocalTransactionState.COMMIT_MESSAGE;
                }else if(stockLogDo.getStatus() ==1 ){  // 代表不能提交事务
                    return LocalTransactionState.UNKNOW;
                }
                return LocalTransactionState.ROLLBACK_MESSAGE;

            }
        });

    }


    // 投递事务型异步消息
    public Boolean transcationAsyanResuceStock(Integer userId,Integer promoId,Integer itemId ,Integer amount,String stockLogId){
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", itemId);
        map.put("amount",amount);
        map.put("stockLogId",stockLogId);

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("userId",userId);
        argsMap.put("promoId",promoId);
        argsMap.put("itemId", itemId);
        argsMap.put("amount",amount);
        argsMap.put("stockLogId",stockLogId);


        Message message = new Message(topicName,"increase",
                JSON.toJSON(map).toString().getBytes(Charset.forName("UTF-8")));
        TransactionSendResult transactionSendResult = null;
        try {

            // 如果他绑定的executeLocalTransaction中的操作成功，此条异步事务消息就可以成功发送，否则就不会发送。
            // 保证了如果订单价创建失败就不会导致库存减少。
             transactionSendResult = transactionProducer.sendMessageInTransaction(message, argsMap);// 会被事务监听器收到

        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }

        if(transactionSendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE ) {
            return false;
        }else if(transactionSendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE) {
            return true;
        }else {
            return false;
        }
    }
    // 投递普通异步消息投放
    public Boolean asyncReduceStock(Integer itemId, Integer amount)  {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", itemId);
        map.put("amount",amount);
        Message message = new Message(topicName,"increase",JSON.toJSON(map).toString().getBytes(Charset.forName("UTF-8")));
        try {
            producer.send(message);

        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;

        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
