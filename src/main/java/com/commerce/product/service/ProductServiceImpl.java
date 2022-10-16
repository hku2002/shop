package com.commerce.product.service;

import com.commerce.global.common.dto.PagingCommonRequestDto;
import com.commerce.product.domain.Product;
import com.commerce.product.dto.OptionVo;
import com.commerce.product.dto.ProductDetailResponseDto;
import com.commerce.product.dto.ProductResponseDto;
import com.commerce.product.repository.OptionRepository;
import com.commerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.commerce.product.domain.Product.DisplayStatus.DISPLAY;
import static com.commerce.product.domain.Product.DisplayStatus.OUT_OF_STOCK;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl {

    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    /**
     * 상품 목록 조회
     * param : productsRequestDto
     */
    public List<ProductResponseDto> findProducts(PagingCommonRequestDto productsRequestDto) {
        List<Product> products = productRepository.findProductsByActivatedAndStatusIn(true
                , List.of(DISPLAY, OUT_OF_STOCK)
                , PageRequest.of(productsRequestDto.getLimit(), productsRequestDto.getOffset()));
        return products.stream().map(ProductResponseDto::new).collect(Collectors.toList());
    }

    /**
     * 상품 상세 조회
     * param id
     */
    public ProductDetailResponseDto findProduct(Long id) {
        ProductDetailResponseDto responseDto = new ProductDetailResponseDto(productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다.")));
        responseDto.setOptions(findOptions(id));
        return responseDto;
    }

    /**
     * 옵션 목록 조회
     * @param productId
     */
    private List<OptionVo> findOptions(Long productId) {
        return optionRepository.findByProductIdAndActivated(productId, true).stream().map(OptionVo::new).collect(Collectors.toList());
    }
}
