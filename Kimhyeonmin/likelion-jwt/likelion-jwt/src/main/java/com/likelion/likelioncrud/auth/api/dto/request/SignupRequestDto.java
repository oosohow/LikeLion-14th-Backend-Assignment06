package com.likelion.likelioncrud.auth.api.dto.request;

import com.likelion.likelioncrud.member.domain.Part;

// 회원가입 요청 시 클라이언트에서 받는 데이터
public record SignupRequestDto(
        String name,
        String email,
        String password,
        Part part
        //Part가 없다면 권한 검사 항상 실패하므로 회원가입사 part를 함께 받아 기본값으로 지정
) {
}
