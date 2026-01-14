package com.ecommerce.store.repositories;

import com.ecommerce.store.entities.Cart;
import com.ecommerce.store.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {


    Optional<Cart> findByUser(User user);

}
