package com.commerce.user.service;

import com.commerce.global.common.Address;
import com.commerce.global.common.exception.BadRequestException;
import com.commerce.user.dto.JoinMemberDto;
import com.commerce.user.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Description;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class MemberServiceImplTest {

    @Autowired MemberServiceImpl memberService;
    @Autowired MemberRepository memberRepository;

    @Test
    @Description("회원가입")
    public void joinTest() {
        // given
        Address address = Address.builder()
                .address("서울시 강남구 테헤란로 427")
                .addressDetail("아이파크몰 test 호")
                .zipCode("12345")
                .build();
        JoinMemberDto joinMemberDto = new JoinMemberDto();
        joinMemberDto.setUserId("testId");
        joinMemberDto.setPassword("1234");
        joinMemberDto.setUsername("홍길동");
        joinMemberDto.setEmail("test01@test.com");
        joinMemberDto.setPhoneNumber("01012345678");
        joinMemberDto.setAddress(address);

        // when
        memberService.join(joinMemberDto);

        // then
        assertEquals(joinMemberDto.getUserId(), memberRepository.findByUserIdAndActivated("testId", true).getUserId());
    }

    @Test
    @Description("중복 회원 검사")
    public void validateDuplicateMemberTest() {
        // given
        Address address = Address.builder()
                .address("서울시 강남구 테헤란로 427")
                .addressDetail("아이파크몰 test 호")
                .zipCode("12345")
                .build();
        JoinMemberDto joinMemberDto = new JoinMemberDto();
        joinMemberDto.setUserId("testId");
        joinMemberDto.setPassword("1234");
        joinMemberDto.setUsername("홍길동");
        joinMemberDto.setEmail("test01@test.com");
        joinMemberDto.setPhoneNumber("01012345678");
        joinMemberDto.setAddress(address);

        // when
        memberService.join(joinMemberDto);

        // then
        assertThrows(BadRequestException.class, () -> memberService.join(joinMemberDto));
    }

    @Test
    @Description("회원 조회")
    public void findOneTest() {
        // given
        Address address = Address.builder()
                .address("서울시 강남구 테헤란로 427")
                .addressDetail("아이파크몰 test 호")
                .zipCode("12345")
                .build();
        JoinMemberDto joinMemberDto = new JoinMemberDto();
        joinMemberDto.setUserId("testId");
        joinMemberDto.setPassword("1234");
        joinMemberDto.setUsername("홍길동");
        joinMemberDto.setEmail("test01@test.com");
        joinMemberDto.setPhoneNumber("01012345678");
        joinMemberDto.setAddress(address);

        // when
        Long memberId = memberService.join(joinMemberDto);

        // then
        assertEquals(memberId, memberService.findOne(joinMemberDto.getUserId()).getId());
    }

    @Test
    @Description("회원이 없을 경우")
    public void userEmptyTest() {
        // given
        Address address = Address.builder()
                .address("서울시 강남구 테헤란로 427")
                .addressDetail("아이파크몰 test 호")
                .zipCode("12345")
                .build();
        JoinMemberDto joinMemberDto = new JoinMemberDto();
        joinMemberDto.setUserId("testId");
        joinMemberDto.setPassword("1234");
        joinMemberDto.setUsername("홍길동");
        joinMemberDto.setEmail("test01@test.com");
        joinMemberDto.setPhoneNumber("01012345678");
        joinMemberDto.setAddress(address);

        // when
        memberService.join(joinMemberDto);

        // then
        assertThrows(BadRequestException.class, () -> memberService.findOne("emptyId"));
    }
}