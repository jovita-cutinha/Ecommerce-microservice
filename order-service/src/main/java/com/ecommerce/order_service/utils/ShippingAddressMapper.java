package com.ecommerce.order_service.utils;

import com.ecommerce.order_service.dto.ShippingAddressDTO;
import com.ecommerce.order_service.model.ShippingAddress;
import org.springframework.stereotype.Component;

@Component
public class ShippingAddressMapper {
    public ShippingAddress toEntity(ShippingAddressDTO dto) {
        ShippingAddress address = new ShippingAddress();
        address.setFullName(dto.getFullName());
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());
        address.setPhoneNumber(dto.getPhoneNumber());
        address.setEmail(dto.getEmail());
        return address;
    }
}

