package bookstore;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Pay_table")
public class Pay extends AbstractEvent {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private String bookName;
    private Integer qty;
    private Integer price;
    private String status;

    @PostPersist
    public void onPostPersist(){
            		
        // try{
        //     Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        // } catch (InterruptedException e){
        //    e.printStackTrace();
        // }

        String status = this.getStatus();

        System.out.println("\n\n##### Pay  onPostPersist : " + this.getOrderId() + ":" + status +"\n\n");

        if (status.equals("Paid")) {
            
            Paid paid = new Paid();
            paid.setId(this.getId());
            paid.setOrderId(this.getOrderId());
            paid.setQty(this.getQty());
            paid.setPrice(this.getPrice());
            paid.setStatus("Paid");
            paid.publishAfterCommit();

        } else if (status.equals("PayCancelled")) {

            PayCancelled PayCancelled = new PayCancelled();
            PayCancelled.setId(this.getId());
            PayCancelled.setOrderId(this.getOrderId());
            PayCancelled.setQty(this.getQty());
            PayCancelled.setPrice(this.getPrice());
            PayCancelled.setStatus("PayCancelled");
            PayCancelled.publishAfterCommit();

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
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


}
