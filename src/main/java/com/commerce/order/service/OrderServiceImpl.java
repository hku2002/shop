package com.commerce.order.service;

import com.commerce.cart.domain.Cart;
import com.commerce.cart.repositiry.CartRepository;
import com.commerce.delivery.domain.Delivery;
import com.commerce.delivery.repository.DeliveryRepository;
import com.commerce.global.common.exception.BadRequestException;
import com.commerce.global.common.token.JwtTokenManager;
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
    private final JwtTokenManager jwtTokenManager;

    /**
     * 주문 목록 조회
     * @param memberId 회원번호
     */
    public List<OrderResponseDto> findOrders(Long memberId, int limit, int offset) {
        return orderRepository.findWithMemberAndDeliveryByMemberId(memberId, PageRequest.of(limit, offset))
                .stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    /**
     * 주문 추가 (주문 완료)
     * @param cartIds 장바구니 아이디
     */
    @Transactional
    public void addOrder(List<Long> cartIds) {
        Member member = memberRepository.findByUserIdAndActivated(jwtTokenManager.getUserIdByToken(), true);
        member.checkMemberExist(member);
        List<Cart> carts = checkCarts(cartIds, member.getId());
        List<Long> itemIds = carts.stream().map(Cart -> Cart.getItem().getId()).collect(Collectors.toList());
        checkItems(itemIds);
        checkItemStockAndSubtractStockByCarts(carts);
        Order order = saveOrder(member, carts);
        saveOrderItems(carts, order);
        saveDelivery(member, order);
    }

    /**
     * 주문 취소
     * @param orderId 주문번호
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findWithDeliveryByOrderId(orderId);
        order.checkOrderExist(order);
        order.checkOrderCanceled();
        order.checkDeliveryCancelPossibility(order);
        order.updateOrderStatus(CANCELED);

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdAndActivated(orderId, true);
        List<Long> itemIds = orderItems.stream().map(OrderItem::getItemId).collect(Collectors.toList());
        checkItems(itemIds);
        checkItemAndAddStockByOrderItems(orderItems);
    }

    /**
     * item 존재 체크
     * @param itemIds item id 목록
     */
    private void checkItems(List<Long> itemIds) {
        List<Item> items = itemRepository.findAllByIdInAndActivated(itemIds, true);
        if (items.size() < 1) {
            throw new BadRequestException("재고 상품이 존재하지 않습니다.");
        }
    }

    /**
     * Cart 정보로 Item 재고 체크 및 차감
     * @param carts 장바구니 목록
     */
    private void checkItemStockAndSubtractStockByCarts(List<Cart> carts) {
        carts.forEach(cart -> {
            Item item = itemRepository.findById(cart.getItem().getId())
                    .orElseThrow(() -> new BadRequestException("재고 상품이 존재하지 않습니다."));
            item.compareStockQuantityWithCartItemQuantity(cart.getItemUsedQuantity());
            item.subtractStock(cart.getItemUsedQuantity());
        });
    }

    /**
     * OrderItem 정보로 재고 체크 및 추가
     * @param orderItems orderItem 목록
     */
    private void checkItemAndAddStockByOrderItems(List<OrderItem> orderItems) {
        orderItems.forEach(orderItem -> {
            Item item = itemRepository.findById(orderItem.getItemId())
                    .orElseThrow(() -> new BadRequestException("재고 상품이 존재하지 않습니다."));
            item.addStock(orderItem.getItemUsedQuantity());
        });
    }

    /**
     * 장바구니 목록 조회
     * @param cartIds 장바구니 ID 목록
     */
    private List<Cart> checkCarts(List<Long> cartIds, Long memberId) {
        List<Cart> carts = cartRepository.findCartsByCartIdsAndMemberId(cartIds, memberId);
        if (carts.size() != cartIds.size()) {
            throw new BadRequestException("잘못된 장바구니 아이디입니다.");
        }
        return carts;
    }

    /**
     * 총 주문 가격 계산
     * @param carts 장바구니 목록
     */
    private int calculateTotalPrice(List<Cart> carts) {
        return carts.stream().mapToInt(cart -> cart.getItem().getPrice().getSalePrice()).sum();
    }

    /**
     * 주문 상품 이름 생성
     * @param carts 장바구니 목록
     */
    private String createOrderName(List<Cart> carts) {
        String name = carts.get(0).getProduct().getName();
        if (carts.size() > 1) {
            name += " 외 " + (carts.size() - 1) + "건";
        }
        return name;
    }

    /**
     * 배송정보 저장
     * @param member 회원 객체
     * @param order 주문 객체
     */
    private void saveDelivery(Member member, Order order) {
        deliveryRepository.save(Delivery.builder()
                .member(member)
                .order(order)
                .address(member.getAddress())
                .status(STAND_BY)
                .build());
    }

    /**
     * 주문 아이템 저장
     * @param carts 장바구니 목록
     * @param order 주문 객체
     */
    private void saveOrderItems(List<Cart> carts, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();
        carts.forEach(cart -> {
            orderItems.add(OrderItem.builder()
                    .order(order)
                    .itemId(cart.getItem().getId())
                    .price(cart.getItem().getPrice())
                    .supplyPrice(cart.getItem().getSupplyPrice())
                    .userPurchaseQuantity(cart.getUserPurchaseQuantity())
                    .itemUsedQuantity(cart.getItemUsedQuantity())
                    .build());
            cart.updateActivated(false);
        });
        orderItemRepository.saveAll(orderItems);
    }

    /**
     * 주문 저장
     * @param member 회원 객체
     * @param carts 장바구니 목록
     */
    private Order saveOrder(Member member, List<Cart> carts) {
        return orderRepository.save(
                Order.builder()
                        .member(member)
                        .name(createOrderName(carts))
                        .totalPrice(calculateTotalPrice(carts))
                        .build());
    }

}
