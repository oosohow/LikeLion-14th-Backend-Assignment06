package com.likelion.likelioncrud.post.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PostSaveRequestDto(

        //Long memberId,
        //기존에는 클라이언트가 직접 보냈지만 JWT 인증 기반에서는 Authorization 헤더에 담긴 Access Token에서 로그인한 사용자의 id를 꺼내야 함.

        @NotBlank(message = "제목을 필수로 입력해야 합니다.")
        String title,

        @NotBlank(message = "내용을 필수로 입력해야 합니다.")
        String contents
) {
}
