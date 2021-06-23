package bookstore;

import javax.persistence.*;

import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String bookName;
    private Integer qty;
    private Integer price;
    private String status;
    private String userName;

    @PostPersist
    public void onPostPersist(){        

        setStatus("Ordered");

        // 주문내역 Pub
        Ordered ordered = new Ordered();
        ordered.setId(this.getId());
        ordered.setBookName(this.getBookName());
        ordered.setQty(this.getQty());
        ordered.setPrice(this.getPrice());
        ordered.setStatus("Ordered");
        ordered.publishAfterCommit();

        // pay request
        bookstore.external.Pay pay = new bookstore.external.Pay();

        pay.setOrderId(this.getId());
        pay.setQty(this.getQty());
        pay.setPrice(this.getPrice());
        pay.setStatus("Paid");

        AppApplication.applicationContext.getBean(bookstore.external.PayService.class).pay(pay);

    }

    @PreRemove
    public void onPreRemove(){

        OrderCancelled orderCancelled = new OrderCancelled();
        orderCancelled.setId(this.getId());
        orderCancelled.setBookName(this.getBookName());
        orderCancelled.setQty(this.getQty());
        orderCancelled.setPrice(this.getPrice());
        orderCancelled.setStatus("OrderCancelled");    
        orderCancelled.publishAfterCommit();

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
