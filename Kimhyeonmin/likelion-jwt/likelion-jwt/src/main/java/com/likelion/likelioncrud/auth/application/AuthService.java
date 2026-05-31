package com.likelion.likelioncrud.auth.application;

import com.likelion.likelioncrud.auth.JwtUtil;
import com.likelion.likelioncrud.auth.api.dto.request.LoginRequestDto;
import com.likelion.likelioncrud.auth.api.dto.request.SignupRequestDto;
import com.likelion.likelioncrud.auth.api.dto.request.TokenRefreshRequestDto;
import com.likelion.likelioncrud.auth.api.dto.response.LoginResponseDto;
import com.likelion.likelioncrud.auth.api.dto.response.TokenRefreshResponseDto;
import com.likelion.likelioncrud.auth.domain.RefreshToken;
import com.likelion.likelioncrud.auth.domain.RefreshTokenRepository;
import com.likelion.likelioncrud.common.exception.BusinessException;
import com.likelion.likelioncrud.common.response.code.ErrorCode;
import com.likelion.likelioncrud.member.domain.Member;
import com.likelion.likelioncrud.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 트랜잭션 적용 (조회 성능 최적화)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;  // SecurityConfig에서 빈으로 등록한 BCryptPasswordEncoder
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    // application.yml의 jwt.refresh-expiration 값 (밀리초)
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // 회원가입
    @Transactional  // DB에 저장하는 작업이므로 쓰기 트랜잭션 적용
    public void signup(SignupRequestDto request) {

        // 1. 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL_EXCEPTION, ErrorCode.DUPLICATE_EMAIL_EXCEPTION.getMessage());
        }

        // 2. 비밀번호 BCrypt 암호화 후 Member 생성
        Member member = Member.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))  // 비밀번호 암호화
                .part(request.part())
                .build();

        // 3. DB 저장
        memberRepository.save(member);
    }

    // 로그인
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {

        // 1. 이메일로 회원 조회 (없으면 예외 처리)
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND_BY_EMAIL_EXCEPTION, ErrorCode.MEMBER_NOT_FOUND_BY_EMAIL_EXCEPTION.getMessage()));

        // 2. 입력한 비밀번호와 암호화된 비밀번호 비교
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_EXCEPTION, ErrorCode.INVALID_PASSWORD_EXCEPTION.getMessage());
        }

        // 3. 인증 성공 → Access Token 발급
        String accessToken = jwtUtil.generateToken(member.getMemberId());

        // [과제] Refresh Token 발급 및 DB 저장
        // TODO (1): jwtUtil.generateRefreshToken()을 호출해서 refreshToken 문자열을 발급하세요.
        String refreshToken = jwtUtil.generateRefreshToken(member.getMemberId());
        //1. jwt 유틸 사용해서 리프레시 토근 문자열 발급

        // TODO (2): 기존에 저장된 이 사용자의 refresh token을 먼저 삭제하세요.
        refreshTokenRepository.deleteByMemberId(member.getMemberId());
        //2. refreshTokenRepository.deleteByMemberId(...) 리프레시 토큰 삭제
        //예전에 로그인해서 남은 리프레시 토큰이 있을 수 있어서...? 계속 쌓이는거 방지

        // TODO (3): RefreshToken 엔티티를 빌더로 생성하고 DB에 저장하세요.
        //           만료 시각 계산: LocalDateTime.now().plusSeconds(refreshExpiration / 1000)
        //           힌트: refreshTokenRepository.save(RefreshToken.builder()...build())
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(refreshExpiration / 1000);
        //3. yml에 있는 만료시간 = 밀리초 단위, 1000으로 나눠서 초단위

        refreshTokenRepository.save(
                RefreshToken.builder()
                    .memberId(member.getMemberId())
                        .token(refreshToken)
                        .expiredAt(expireTime)
                .build());
        //3. RefreshToken 엔티티를 빌더로 생성하고 DB에 저장

        // 4. Access Token + Refresh Token 반환
        return new LoginResponseDto(accessToken, refreshToken);
    }

    // [과제] Access Token 재발급
    @Transactional
    public TokenRefreshResponseDto reissue(TokenRefreshRequestDto request) {

        // TODO (1): request에서 refreshToken 문자열을 꺼내세요.
        String refreshToken = request.refreshToken();
        //1. 리퀘스트에서 리프레시 토큰 문자열 꺼내기

        // TODO (2): jwtUtil.validateToken()으로 refresh token의 서명/만료를 검증하세요.
        //           유효하지 않으면 INVALID_REFRESH_TOKEN_EXCEPTION을 throw하세요.
        if(!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN_EXCEPTION, ErrorCode.INVALID_REFRESH_TOKEN_EXCEPTION.getMessage());
        }
        //2. 토큰 기간 지났는지, 이상한 토큰이 아닌지 검사 -> 에러 표시

        // TODO (3): DB에서 refreshToken 문자열로 RefreshToken 엔티티를 조회하세요.
        //           없으면 INVALID_REFRESH_TOKEN_EXCEPTION을 throw하세요.
        //           힌트: refreshTokenRepository.findByToken(refreshToken).orElseThrow(...)
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(refreshToken); //토큰 담기

        if (tokenOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN_EXCEPTION, ErrorCode.INVALID_REFRESH_TOKEN_EXCEPTION.getMessage());
        } //토큰 없을때
        //3. 디비에 토큰이 있는지 확인하고 없으면 에러

        // TODO (4): refresh token에서 userId를 추출하고, 새로운 Access Token을 발급하세요.
        //           힌트: jwtUtil.getUserId(refreshToken), jwtUtil.generateToken(userId)
        Long userId = jwtUtil.getUserId(refreshToken);
        String newAccessToken = jwtUtil.generateToken(userId);
        //4. 리프레시 토큰에서 유저 아이디 꺼내고 새로운 액세스 토큰 발급

        // 5. 새로 발급한 Access Token 반환
        return new TokenRefreshResponseDto(newAccessToken);
    }
}
