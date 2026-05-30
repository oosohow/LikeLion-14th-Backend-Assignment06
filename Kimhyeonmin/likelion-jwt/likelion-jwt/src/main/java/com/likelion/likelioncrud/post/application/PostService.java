package com.likelion.likelioncrud.post.application;

import com.likelion.likelioncrud.common.exception.BusinessException;
import com.likelion.likelioncrud.common.response.code.ErrorCode;
import com.likelion.likelioncrud.member.domain.Member;
import com.likelion.likelioncrud.member.domain.Part;
import com.likelion.likelioncrud.member.domain.repository.MemberRepository;
import com.likelion.likelioncrud.post.api.dto.request.PostSaveRequestDto;
import com.likelion.likelioncrud.post.api.dto.request.PostUpdateRequestDto;
import com.likelion.likelioncrud.post.api.dto.response.PostInfoResponseDto;
import com.likelion.likelioncrud.post.domain.Post;
import com.likelion.likelioncrud.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    // 게시물 저장
    @Transactional
    public void postSave(Long userId, PostSaveRequestDto postSaveRequestDto) {
        //Member member = memberRepository.findById(postSaveRequestDto.memberId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND_EXCEPTION, ErrorCode.MEMBER_NOT_FOUND_EXCEPTION.getMessage() + postSaveRequestDto.memberId()));
        Member member = findMemberById(userId);

        //BE파트만 작성하기 위해 권한 검사
        checkBackendPermission(member);

        Post post = Post.builder()
                .title(postSaveRequestDto.title())
                .contents(postSaveRequestDto.contents())
                .member(member)
                .build();

        postRepository.save(post);
    }

    // 특정 작성자가 작성한 게시글 목록을 조회( 조회 요청 userId 추가)
    public Page<PostInfoResponseDto> postFindMember(Long userId, Long memberId, Pageable pageable) {
        //Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND_EXCEPTION, ErrorCode.MEMBER_NOT_FOUND_EXCEPTION.getMessage() + memberId));
        //조회 요청자 확인
        Member requester = findMemberById(userId);

        //AI파트 조회 차단하기 위해 권한 검사
        checkReadPermission(requester);

        //조회 대상자 확인
        Member targetMember = findMemberById(memberId);

        Page<Post> posts = postRepository.findByMember(targetMember, pageable);
        return posts.map(PostInfoResponseDto::from);
    }

    // 게시물 수정
    @Transactional
    public void postUpdate(Long userId, Long postId, PostUpdateRequestDto postUpdateRequestDto) {
        Member member = findMemberById(userId);

        Post post = findPostById(postId);
        //Post post = postRepository.findById(postId).orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND_EXCEPTION, ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId));

        //BE파트만 수정하기 위해 권한 검사 + 본인 작성 여부 검사
        checkBackendPermission(member, post);

        post.update(postUpdateRequestDto);
    }

    // 게시물 삭제
    @Transactional
    public void postDelete(Long userId, Long postId) {
        Member member = findMemberById(userId);

        Post post = findPostById(postId);
        //Post post = postRepository.findById(postId).orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND_EXCEPTION, ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId));

        //BE파트만 삭제하기 위해 권한 검사 + 본인 작성 여부 검사
        checkBackendPermission(member, post);

        postRepository.delete(post);
    }

    //회원 조회 공통 메서드
    private Member findMemberById(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(
                        ErrorCode.MEMBER_NOT_FOUND_EXCEPTION,
                        ErrorCode.MEMBER_NOT_FOUND_EXCEPTION.getMessage() + userId
                ));
    }

    //게시글 조회 공통 메서드
    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(()-> new BusinessException(
                        ErrorCode.POST_NOT_FOUND_EXCEPTION,
                        ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId
                ));
    }

    //권한 검사 공통 메서드
    //작성 권한(BE파트 허용)
    private void checkBackendPermission(Member member) {
        if (member.getPart() != Part.BACKEND) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN_EXCEPTION,
                    ErrorCode.FORBIDDEN_EXCEPTION.getMessage()
            );
        }
    }

    //조회 권한(AI파트 차단)
    private void checkReadPermission(Member member) {
        if (member.getPart() == Part.AI) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN_EXCEPTION,
                    ErrorCode.FORBIDDEN_EXCEPTION.getMessage()
//                    ErrorCode.AI_READ_FORBIDDEN,
//                    ErrorCode.AI_READ_FORBIDDEN.getMessage()
            );
        }
    }

    //수정, 삭제 권한(BE확인 및 본인 작성 게시글 권한)
    private void checkBackendPermission(Member member, Post post) {
        //BE 파트인지 확인
        if (member.getPart() != Part.BACKEND) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN_EXCEPTION,
                    ErrorCode.FORBIDDEN_EXCEPTION.getMessage()
//                  ErrorCode.BACKEND_ONLY_FORBIDDEN,
//                  ErrorCode.BACKEND_ONLY_FORBIDDEN.getMessage()
            );
        }

        //자기가 작성한 게시글이 맞는지 확인
        if (!post.getMember().getMemberId().equals(member.getMemberId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN_EXCEPTION,
                    ErrorCode.FORBIDDEN_EXCEPTION.getMessage()
//                  ErrorCode.NOT_POST_OWNER_FORBIDDEN,
//                  ErrorCode.NOT_POST_OWNER_FORBIDDEN.getMessage()
            );
        }
    }
}


