package bookstore;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderStatusViewRepository extends CrudRepository<OrderStatusView, Long> {

    List<OrderStatusView> findByOrderId(Long orderIdyLong);

    void deleteByOrderId(Long orderId);
}