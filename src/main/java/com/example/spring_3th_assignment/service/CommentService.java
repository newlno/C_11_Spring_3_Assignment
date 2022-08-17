package com.example.spring_3th_assignment.service;


import com.example.spring_3th_assignment.Controller.request.CommentRequestDto;
import com.example.spring_3th_assignment.Controller.response.CommentResponseDto;
import com.example.spring_3th_assignment.Controller.response.ReCommentResponseDto;
import com.example.spring_3th_assignment.Controller.response.ResponseDto;
import com.example.spring_3th_assignment.domain.*;
import com.example.spring_3th_assignment.jwt.TokenProvider;
import com.example.spring_3th_assignment.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReCommentRepository reCommentRepository;
    private final TokenProvider tokenProvider;
    private final PostService postService;


  private final CommentLikeRepository commentLikeRepository;
    private final ReCommentLikeRepository reCommentLikeRepository;

  private final MemberRepository memberRepository;

    // 댓글 생성
    @Transactional
    public ResponseDto<?> createComment(CommentRequestDto requestDto, HttpServletRequest request) {
        if (null == request.getHeader("Refresh-Token")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        if (null == request.getHeader("Authorization")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        Member member = validateMember(request);
        if (null == member) {
            return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
        }

        Post post = postService.isPresentPost(requestDto.getPostId());
        if (null == post) {
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
        }

        Comment comment = Comment.builder()
                .member(member)
                .post(post)
                .content(requestDto.getContent())
                .build();
        commentRepository.save(comment);
        return ResponseDto.success(
                CommentResponseDto.builder()
                        .postId(comment.getPost().getId())
                        .commentId(comment.getId())
                        .author(comment.getMember().getNickname())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .modifiedAt(comment.getModifiedAt())
                        .build()
        );
    }

    // 댓글 조회
    @Transactional(readOnly = true)
    public ResponseDto<?> getAllCommentsByPost(Long postId) {
        Post post = postService.isPresentPost(postId);
        if (null == post) {
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
        }

        List<Comment> commentList = commentRepository.findAllByPost(post);
        List<CommentResponseDto> commentResponseDtoList = new ArrayList<>();


        for (Comment comment : commentList) {
            List<ReComment> reCommentList = reCommentRepository.findAllByComment(comment);
            List<ReCommentResponseDto> reCommentResponseDtoList = new ArrayList<>();
            List<CommentLike> commentLikeList = commentLikeRepository.findByComment(comment);
            for (ReComment reComment : reCommentList) {
                List<ReCommentLike> reCommentLikeList = reCommentLikeRepository.findByReComment(reComment);
                reCommentResponseDtoList.add(
                        ReCommentResponseDto.builder()
                                .commentId(reComment.getComment().getId())
                                .reCommentId(reComment.getId())
                                .author(reComment.getMember().getNickname())
                                .content(reComment.getContent())
                                .reCommentLike((long) reCommentLikeList.size())
                                .createdAt(reComment.getCreatedAt())
                                .modifiedAt(reComment.getModifiedAt())
                                .build()
                );
            }

            commentResponseDtoList.add(
                    CommentResponseDto.builder()
                            .postId(comment.getPost().getId())
                            .commentId(comment.getId())
                            .author(comment.getMember().getNickname())
                            .content(comment.getContent())
                            .commentLike((long) commentLikeList.size())
                            .createdAt(comment.getCreatedAt())
                            .modifiedAt(comment.getModifiedAt())
                            .reCommentResponseDtoList(reCommentResponseDtoList)
                            .build()
            );
        }


    return ResponseDto.success(commentResponseDtoList);
  }

  @Transactional
  public ResponseDto<?> updateComment(Long id, CommentRequestDto requestDto, HttpServletRequest request) {
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    Post post = postService.isPresentPost(requestDto.getPostId());
    if (null == post) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
    }

    Comment comment = isPresentComment(id);
    if (null == comment) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 댓글 id 입니다.");
    }

    if (comment.validateMember(member)) {
      return ResponseDto.fail("BAD_REQUEST", "작성자만 수정할 수 있습니다.");
    }

    comment.update(requestDto);
    return ResponseDto.success(
        CommentResponseDto.builder()
            .postId(comment.getPost().getId())
            .author(comment.getMember().getNickname())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .modifiedAt(comment.getModifiedAt())
            .build()
    );
  }

  @Transactional
  public ResponseDto<?> deleteComment(Long id, HttpServletRequest request) {
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    Comment comment = isPresentComment(id);
    if (null == comment) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 댓글 id 입니다.");
    }

    if (comment.validateMember(member)) {
      return ResponseDto.fail("BAD_REQUEST", "작성자만 수정할 수 있습니다.");
    }

    commentRepository.delete(comment);
    return ResponseDto.success("success");
  }

  @Transactional(readOnly = true)
  public Comment isPresentComment(Long id) {
    Optional<Comment> optionalComment = commentRepository.findById(id);
    return optionalComment.orElse(null);
  }

  @Transactional
  public Member validateMember(HttpServletRequest request) {
    if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
      return null;
    }
    return tokenProvider.getMemberFromAuthentication();
  }

  public void commentLike(Long commentId, String nickname) {
    Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("aa"));
    Member member = memberRepository.findByNickname(nickname).orElseThrow(() -> new RuntimeException("aaa"));
    CommentLike b = commentLikeRepository.findByCommentAndMember(comment,member).orElse(null);
    if (b == null) {
      CommentLike commentLike = new CommentLike(comment, member);
      commentLikeRepository.save(commentLike);
    } else {
      commentLikeRepository.delete(b);
    }
  }




  private Comment getCommentInService(Long commentId) {
    return null;
  }


}
