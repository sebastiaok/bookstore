
package bookstore;

public class DeliveryCancelled extends AbstractEvent {

    private Long id;
    private Long orderId;
    private Long payId;
    private String status;

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
    public boolean isMe() {
        return false;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

