package bookstore;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Delivery_table")
public class Delivery {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private Long payId;
    private String status;

    @PostPersist
    public void onPostPersist(){

        String status = this.getStatus();

        System.out.println("\n\n##### Delivery  onPostPersist : " + this.getOrderId() + ":" + status +"\n\n");


        if (status.equals("DeliveryStarted")) {
            
            DeliveryStarted deliveryStarted = new DeliveryStarted();

            deliveryStarted.setId(this.getId());
            deliveryStarted.setOrderId(this.getOrderId());
            deliveryStarted.setPayId(this.getPayId());
            deliveryStarted.setStatus("DeliveryStarted");

            deliveryStarted.publishAfterCommit();

        } else if (status.equals("DeliveryCancelled")) {

            DeliveryCancelled deliveryCancelled = new DeliveryCancelled();

            deliveryCancelled.setId(this.getId());
            deliveryCancelled.setOrderId(this.getOrderId());
            deliveryCancelled.setPayId(this.getPayId());
            deliveryCancelled.setStatus("DeliveryCancelled");

            deliveryCancelled.publishAfterCommit();

        } else {
            return;
        }

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}
