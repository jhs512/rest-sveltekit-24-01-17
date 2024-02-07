package com.ll.rsv.domain.post.postComment.service;

import com.ll.rsv.domain.member.member.entity.Member;
import com.ll.rsv.domain.post.post.entity.Post;
import com.ll.rsv.domain.post.postComment.entity.PostComment;
import com.ll.rsv.domain.post.postComment.repository.PostCommentRepository;
import com.ll.rsv.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Transactional
@RequiredArgsConstructor
public class PostCommentService {
    private final PostCommentRepository postCommentRepository;


    @Transactional
    public void delete(Post post, PostComment postComment) {
        post.deleteComment(postComment);
    }

    @Transactional
    public PostComment write(Member author, Post post, String body, boolean published) {
        PostComment postComment = post.addComment(author, body, published);

        return postComment;
    }

    @Transactional
    public void edit(Post post, PostComment postComment, String body) {
        postComment.setBody(body);

        if (!postComment.isPublished()) {
            postComment.getPost().increaseCommentsCount();
        }

        postComment.setPublished(true);
    }

    public boolean canDelete(Member actor, PostComment postComment) {
        if (actor == null) return false;
        if (postComment == null) return false;
        if (actor.isAdmin()) return true;
        return actor.equals(postComment.getAuthor());
    }


    public boolean canEdit(Member actor, PostComment postComment) {
        if (actor == null) return false;
        if (postComment == null) return false;
        return actor.equals(postComment.getAuthor());
    }

    public RsData<PostComment> findTempOrMake(Member author, Post post) {
        AtomicBoolean isNew = new AtomicBoolean(false);

        PostComment postComment = postCommentRepository.findTop1ByPostAndAuthorAndPublishedAndBodyOrderByIdDesc(
                post,
                author,
                false,
                ""
        ).orElseGet(() -> {
            isNew.set(true);
            return write(author, post, "", false);
        });

        return RsData.of(
                isNew.get() ? "임시댓글이 생성되었습니다." : "%d번 임시댓글을 불러왔습니다.".formatted(postComment.getId()),
                postComment
        );
    }

    public List<PostComment> findByPostAndPublishedAndParentCommentOrderByIdDesc(Post post, boolean published, PostComment parentComment) {
        return postCommentRepository.findByPostAndPublishedAndParentCommentOrderByIdDesc(post, true, parentComment);
    }

    public Optional<PostComment> findById(long id) {
        return postCommentRepository.findById(id);
    }

    public boolean canRead(Member actor, PostComment postComment) {
        if (postComment == null) return false;

        if (postComment.isPublished()) return true; // 공개글이면 가능

        if (actor == null) return false;
        if (actor.isAdmin()) return true; // 관리자이면 가능

        return actor.equals(postComment.getAuthor()); // 그것도 아니라면 본인이 쓴 글이여야 함
    }
}
