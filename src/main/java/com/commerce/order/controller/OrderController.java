package com.commerce.order.controller;

import com.commerce.global.common.CommonResponse;
import com.commerce.global.common.dto.PagingCommonRequestDto;
import com.commerce.order.service.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;

    @GetMapping("/v1/orders")
    public ResponseEntity<?> orders(PagingCommonRequestDto pagingCommonRequestDto) {
        return CommonResponse.setResponse(orderServiceImpl.findOrders(1L, pagingCommonRequestDto.getLimit(), pagingCommonRequestDto.getOffset()));
    }

    @PostMapping("/v1/order")
    public ResponseEntity<?> addOrder() {
        orderServiceImpl.addOrder();
        return CommonResponse.setResponse();
    }

    @PutMapping("/v1/order/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        orderServiceImpl.cancelOrder();
        return CommonResponse.setResponse();
    }
}
