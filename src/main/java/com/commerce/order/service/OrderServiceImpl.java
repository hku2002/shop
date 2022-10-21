package com.commerce.order.service;

import com.commerce.cart.domain.Cart;
import com.commerce.cart.repositiry.CartRepository;
import com.commerce.delivery.domain.Delivery;
import com.commerce.delivery.repository.DeliveryRepository;
import com.commerce.order.domain.Order;
import com.commerce.order.domain.OrderItem;
import com.commerce.order.dto.OrderResponseDto;
import com.commerce.order.repository.OrderItemRepository;
import com.commerce.order.repository.OrderRepository;
import com.commerce.product.domain.Item;
import com.commerce.product.repository.ItemRepository;
import com.commerce.user.domain.Member;
import com.commerce.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.commerce.delivery.domain.Delivery.DeliveryStatus.STAND_BY;
import static com.commerce.order.domain.Order.OrderStatus.CANCELED;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final DeliveryRepository deliveryRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    /**
     * 주문 목록 조회
     * @param userId
     */
    public List<OrderResponseDto> findOrders(Long userId, int limit, int offset) {
        return orderRepository.findWithOrderProductsAndDeliveryByUserId(userId, PageRequest.of(limit, offset))
                .stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    /**
     * 주문 추가 (주문 완료)
     * @param cartIds
     */
    @Transactional
    public void addOrder(List<Long> cartIds) {
        Member member = findMember(1L);
        List<Cart> carts = checkCarts(cartIds, member.getId());
        List<Long> itemIds = carts.stream().map(Cart -> Cart.getItem().getId()).collect(Collectors.toList());
        findItems(itemIds);
        checkItemStockAndSubtractStockByCarts(carts);

        Order order = orderRepository.save(
                Order.builder()
                .member(member)
                .name(createOrderName(carts))
                .totalPrice(calculateTotalPrice(carts))
                .build());

        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cart : carts) {
            orderItems.add(OrderItem.builder()
                    .order(order)
                    .itemId(cart.getItem().getId())
                    .price(cart.getItem().getPrice())
                    .supplyPrice(cart.getItem().getSupplyPrice())
                    .userPurchaseQuantity(cart.getUserPurchaseQuantity())
                    .itemUsedQuantity(cart.getItemUsedQuantity())
                    .build());
            cart.updateActivated(false);
        }
        orderItemRepository.saveAll(orderItems);
        deliveryRepository.save(Delivery.builder()
                .member(member)
                .order(order)
                .address(member.getAddress())
                .status(STAND_BY)
                .build());
    }

    /**
     * 주문 취소
     * @param orderId
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));
        order.updateOrderStatus(CANCELED);
    }

    /**
     * 회원 조회
     * @param memberId
     */
    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
    }

    /**
     * item 목록 조회
     * @param itemIds
     */
    private List<Item> findItems(List<Long> itemIds) {
        return itemRepository.findAllByIdInAndActivated(itemIds, true);
    }

    /**
     * Cart 정보로 Item 재고 체크 및 차감
     * @param carts
     */
    private void checkItemStockAndSubtractStockByCarts(List<Cart> carts) {
        for (Cart cart : carts) {
            Item item = itemRepository.findById(cart.getItem().getId())
                    .orElseThrow(() -> new IllegalArgumentException("재고 상품이 존재하지 않습니다."));
            if (item.getStockQuantity() < cart.getItemUsedQuantity()) {
                throw new IllegalArgumentException("재고가 부족합니다.");
            }
            item.subtractStock(cart.getItemUsedQuantity());
        }
    }

    /**
     * Cart 로 재고 차감
     * @param item
     * @param cart
     */
    private void subtractStockByCart(Item item, Cart cart) {
        item.subtractStock(cart.getItemUsedQuantity());
    }

    /**
     * 장바구니 목록 조회
     * @param cartIds
     */
    private List<Cart> checkCarts(List<Long> cartIds, Long memberId) {
        List<Cart> carts = cartRepository.findWithOptionAndItem(cartIds, memberId);
        if (carts.size() != cartIds.size()) {
            throw new IllegalArgumentException("잘못된 장바구니 아이디입니다.");
        }
        return carts;
    }

    /**
     * 총 주문 가격 계산
     * @param carts
     */
    private int calculateTotalPrice(List<Cart> carts) {
        int totalPrice = 0;
        for (Cart cart : carts) {
            totalPrice += cart.getItem().getPrice().getSalePrice();
        }
        return totalPrice;
    }

    /**
     * 주문 상품 이름 생성
     * @param carts
     */
    private String createOrderName(List<Cart> carts) {
        String name = carts.get(0).getProduct().getName();
        if (carts.size() > 1) {
            name += " 외 " + (carts.size() - 1) + "건";
        }
        return name;
    }

}
