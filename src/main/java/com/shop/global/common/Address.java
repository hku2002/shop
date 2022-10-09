package com.shop.global.common;

import lombok.Builder;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Getter
@Embeddable
public class Address {

    @Column(name = "address", length = 100)
    private String address;

    @Column(name = "address_detail", length = 100)
    private String addressDetail;

    @Column(name = "zip_code", length = 5)
    private String zipCode;

    protected Address() {}

    @Builder
    public Address(String address, String addressDetail, String zipCode) {
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipCode = zipCode;
    }

}
