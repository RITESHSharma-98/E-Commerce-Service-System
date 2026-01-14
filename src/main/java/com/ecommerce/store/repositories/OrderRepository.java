package com.ecommerce.store.repositories;

import com.ecommerce.store.entities.Order;
import com.ecommerce.store.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByUser(User user);

}
