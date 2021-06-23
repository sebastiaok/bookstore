package bookstore;

import bookstore.config.kafka.KafkaProcessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired PayRepository payRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCancelled_PayCancel(@Payload OrderCancelled orderCancelled){

        if(!orderCancelled.validate()) return;        

        Pay pay = new Pay();
        pay.setOrderId(orderCancelled.getId());
        pay.setQty(orderCancelled.getQty());
        pay.setPrice(orderCancelled.getPrice());
        pay.setStatus(orderCancelled.getStatus());
        pay.setStatus("PayCancelled");
        
        payRepository.save(pay);    
    }

}
