package cn.com.sherhom.reno.kafka.producer;

import cn.com.sherhom.reno.kafka.producer.fairyland.KafkaProducerFairyLand;
import org.junit.Test;

/**
 * @author Sherhom
 * @date 2020/9/8 14:41
 */
public class ProducerTest {
    @Test
    public void produTest(){
        new KafkaProducerFairyLand().entrance(
                1,3,1,0,1*1024,160*1024,0
        );
    }
}