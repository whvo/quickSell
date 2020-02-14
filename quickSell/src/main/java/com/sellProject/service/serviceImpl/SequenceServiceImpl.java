package com.sellProject.service.serviceImpl;

import com.sellProject.dao.SequenceDoMapper;
import com.sellProject.dataobject.SequenceDo;
import com.sellProject.service.SequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author whvo
 * @date 2019/10/23 0023 -0:25
 */
@Service
public class SequenceServiceImpl implements SequenceService {

    @Autowired
    private  SequenceDoMapper sequenceDoMapper;


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderNo(){

        StringBuilder sb = new StringBuilder();
        // 流水号一共16位
        // 前八位为时间信息（年月日）
        LocalDateTime now =  LocalDateTime.now();  // 获取当前时间
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        sb.append(nowDate);
        // 中间六位为自增序列（其实也就是每天的订单量最大为六位）
        int sequence = 0;
        SequenceDo sequenceDo = sequenceDoMapper.getSequenceByName("order_info");
        sequence = sequenceDo.getCurrentValue();
        sequenceDo.setCurrentValue(sequenceDo.getCurrentValue() + sequenceDo.getStep());
        sequenceDoMapper.updateByPrimaryKeySelective(sequenceDo);
        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6-sequenceStr.length(); i++) {
            sb.append(0);
        }
        sb.append(sequenceStr);
        // 最后两位为分库分表序列 暂时写死为 00
        sb.append("00");
        return sb.toString();
    }

}
