package com.ecommerce.order_service.utils;

import com.ecommerce.order_service.dto.OrderRequestDTO;
import com.ecommerce.order_service.dto.ShippingAddressDTO;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.model.ShippingAddress;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequestDTO dto) {
        Order order = new Order();
        order.setTotalAmount(dto.getTotalAmount());

        // Set shipping address
        ShippingAddress address = new ShippingAddress();
        ShippingAddressDTO addrDto = dto.getShippingAddress();
        address.setFullName(addrDto.getFullName());
        address.setStreet(addrDto.getStreet());
        address.setCity(addrDto.getCity());
        address.setState(addrDto.getState());
        address.setPostalCode(addrDto.getPostalCode());
        address.setCountry(addrDto.getCountry());
        address.setPhoneNumber(addrDto.getPhoneNumber());
        order.setShippingAddress(address);

        // Convert order items
        List<OrderItem> items = dto.getOrderItems().stream().map(itemDto -> {
            OrderItem item = new OrderItem();
            item.setProductId(itemDto.getProductId());
            item.setProductName(itemDto.getProductName());
            item.setQuantity(itemDto.getQuantity());
            item.setPricePerUnit(itemDto.getPricePerUnit());
            return item;
        }).collect(Collectors.toList());

        order.setOrderItems(items);
        return order;
    }
}