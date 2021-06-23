package bookstore;

import bookstore.config.kafka.KafkaProcessor;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired OrderRepository orderRepository;
    
    
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryStarted_StatusUpdate(@Payload DeliveryStarted deliveryStarted){

        if(!deliveryStarted.validate()) return;

        System.out.println("\n\n##### listener StatusUpdate : " + deliveryStarted.toJson() + "\n\n");       

        // Correlation id 는 'orderId' 임
        orderRepository.findById(Long.valueOf(deliveryStarted.getOrderId())).ifPresent(Match -> {

            System.out.println("##### wheneverDeliveryStarted_StatusUpdate.findById : exist");

            System.out.println("deliveryStarted 주문 번호 : "+ deliveryStarted.getOrderId());

            Order order = new Order();
            
            order.setId(deliveryStarted.getOrderId());
            order.setStatus(deliveryStarted.getStatus());
            System.out.println("\n\n##### wheneverDeliveryStarted_StatusUpdate order ID: " + order.getId() + "\n\n");
            
            orderRepository.save(order);
        });
           
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCancelled_StatusUpdate(@Payload DeliveryCancelled deliveryCancelled){

        if(!deliveryCancelled.validate()) return;

        System.out.println("\n\n##### listener StatusUpdate : " + deliveryCancelled.toJson() + "\n\n");       

        // Correlation id =  'orderId'
        orderRepository.findById(Long.valueOf(deliveryCancelled.getOrderId())).ifPresent(Match -> {

            System.out.println("##### wheneverDeliveryCancelled_StatusUpdate.findById : exist");

            System.out.println("deliveryCancelled 주문 번호 : "+ deliveryCancelled.getOrderId());

            Order order = new Order();
            
            order.setId(deliveryCancelled.getOrderId());
            order.setStatus(deliveryCancelled.getStatus());
            System.out.println("\n\n##### wheneverDeliveryCancelled_StatusUpdate order ID: " + order.getId() + "\n\n");
            
            orderRepository.save(order);
        });        
            
    }

}
