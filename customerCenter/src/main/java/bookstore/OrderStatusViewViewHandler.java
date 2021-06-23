package bookstore;

import bookstore.config.kafka.KafkaProcessor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;


@Service
public class OrderStatusViewViewHandler {

    @Autowired
    private OrderStatusViewRepository orderStatusViewRepository;
    
    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrdered_then_CREATE_1 (@Payload Ordered ordered) {

        try {

            if (ordered.validate()) {
               // View 객체 생성
               OrderStatusView orderStatusView = new OrderStatusView();
               orderStatusView.setOrderId(ordered.getId());
               orderStatusView.setBookName(ordered.getBookName());
               orderStatusView.setQty(ordered.getQty());
               orderStatusView.setStatus(ordered.getStatus());
               // View Repository save
               orderStatusViewRepository.save(orderStatusView);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaid_then_UPDATE_1 (@Payload Paid paid) {

        try {

            if (paid.validate()) {
               // View 객체 생성

               List<OrderStatusView> OrderStatusViewList = orderStatusViewRepository.findByOrderId(paid.getOrderId());
               for(OrderStatusView orderStatusView : OrderStatusViewList){

                    orderStatusView.setPayId(paid.getId());
                    orderStatusView.setStatus(paid.getStatus());
                    // View Repository save
                    orderStatusViewRepository.save(orderStatusView);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPayCancelled_then_UPDATE_1 (@Payload PayCancelled PaidCancelled) {

        try {

            if (PaidCancelled.validate()) {
               // View 객체 생성

               List<OrderStatusView> OrderStatusViewList = orderStatusViewRepository.findByOrderId(PaidCancelled.getOrderId());
               for(OrderStatusView orderStatusView : OrderStatusViewList){

                    orderStatusView.setPayId(PaidCancelled.getId());
                    orderStatusView.setStatus(PaidCancelled.getStatus());
                    // View Repository save
                    orderStatusViewRepository.save(orderStatusView);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryStarted_then_UPDATE_1 (@Payload DeliveryStarted deliveryStarted) {

        try {

            if (deliveryStarted.validate()) {
               // View 객체 생성

               List<OrderStatusView> OrderStatusViewList = orderStatusViewRepository.findByOrderId(deliveryStarted.getOrderId());
               for(OrderStatusView orderStatusView : OrderStatusViewList){

                    orderStatusView.setDeliveryId(deliveryStarted.getId());
                    orderStatusView.setPayId(deliveryStarted.getPayId());
                    orderStatusView.setStatus(deliveryStarted.getStatus());
                    // View Repository save
                    orderStatusViewRepository.save(orderStatusView);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryCancelled_then_UPDATE_1 (@Payload DeliveryCancelled deliveryCancelled) {

        try {

            if (deliveryCancelled.validate()) {
               // View 객체 생성

               List<OrderStatusView> OrderStatusViewList = orderStatusViewRepository.findByOrderId(deliveryCancelled.getOrderId());
               for(OrderStatusView orderStatusView : OrderStatusViewList){

                    orderStatusView.setDeliveryId(deliveryCancelled.getId());
                    orderStatusView.setPayId(deliveryCancelled.getPayId());
                    orderStatusView.setStatus(deliveryCancelled.getStatus());
                    // View Repository save
                    orderStatusViewRepository.save(orderStatusView);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}