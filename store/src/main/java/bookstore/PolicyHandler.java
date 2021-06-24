package bookstore;

import bookstore.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_Delivery(@Payload Paid paid){
        
        if(!paid.validate()) return;

        Delivery delivery= new Delivery();
        delivery.setOrderId(paid.getOrderId());
        delivery.setPayId(paid.getId());
        delivery.setStatus("DeliveryStarted");        
        deliveryRepository.save(delivery);      
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCancelled_DeliveryCancel(@Payload PayCancelled payCancelled){

        System.out.println("\n\n##### listener DeliveryCancel : " + payCancelled.toJson() + "\n\n");

        if(!payCancelled.validate()) return;

        System.out.println("\n\n##### 주문번호 : " + payCancelled.getOrderId() + "\n\n");
                
        Delivery delivery= new Delivery();
        delivery.setOrderId(payCancelled.getOrderId());
        delivery.setPayId(payCancelled.getId());
        delivery.setStatus("DeliveryCancelled");
        deliveryRepository.save(delivery);
            
    }

}
