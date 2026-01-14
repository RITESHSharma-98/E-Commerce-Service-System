package com.ecommerce.store.dtos;

import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class OrderDto {

    private String orderId;
    @Builder.Default
    private String orderStatus="PENDING";
    @Builder.Default
    private String paymentStatus="NOTPAID";
    private int orderAmount;
    private String billingAddress;
    private String billingPhone;
    private String billingName;
    @Builder.Default
    private Date orderedDate=new Date();
    private Date deliveredDate;
    //private UserDto user;
    @Builder.Default
    private List<OrderItemDto> orderItems = new ArrayList<>();

    //add this to get user information with order
    private  UserDto user;


}
