package com.example.bookiibookii.domain.comment.repository;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.comment.entity.Comment;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ImportAutoConfiguration(exclude = DataJpaRepositoriesAutoConfiguration.class)
@EnableJpaRepositories(basePackageClasses = CommentRepository.class)
@ActiveProfiles("test")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    private User host;
    private User author;
    private User parentWriter;
    private User otherMember;
    private User outsider;
    private Groups group;
    private Comment publicRoot;
    private Comment secretRoot;
    private Comment secretReply;

    @BeforeEach
    void setUp() {
        host = persistUser("host");
        author = persistUser("author");
        parentWriter = persistUser("parent-writer");
        otherMember = persistUser("other-member");
        outsider = persistUser("outsider");

        Book book = Book.builder()
                .isbn13("9780000000301")
                .title("댓글 테스트 책")
                .author("저자")
                .publisher("출판사")
                .image("image")
                .totalPages(100)
                .link("link")
                .category(CustomCategory.KOREAN_NOVEL)
                .build();
        entityManager.persist(book);

        group = Groups.builder()
                .book(book)
                .host(host)
                .maxCapacity(2)
                .readingPeriod(14)
                .groupStatus(GroupStatus.RECRUITING)
                .tradeType(TradeType.DIRECT)
                .groupName("댓글 테스트 그룹")
                .build();
        entityManager.persist(group);

        publicRoot = persistComment(parentWriter, null, false, null, "공개 댓글");
        secretRoot = persistComment(author, null, true, host.getId(), "비밀 댓글");
        secretReply = persistComment(author, publicRoot, true, parentWriter.getId(), "비밀 답글");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void secretRootIsVisibleOnlyToAuthorAndHost() {
        assertVisible(author, publicRoot, secretRoot, secretReply);
        assertVisible(host, publicRoot, secretRoot, secretReply);
        assertVisible(otherMember, publicRoot);
        assertVisible(outsider, publicRoot);
    }

    @Test
    void secretReplyIsVisibleToAuthorParentWriterAndHost() {
        assertVisible(author, publicRoot, secretRoot, secretReply);
        assertVisible(parentWriter, publicRoot, secretReply);
        assertVisible(host, publicRoot, secretRoot, secretReply);
        assertVisible(otherMember, publicRoot);
        assertVisible(outsider, publicRoot);
    }

    @Test
    void hostSeesPublicParentAndSelfTargetedSecretReplyInTreeQuery() {
        Comment selfReply = persistComment(author, null, false, null, "A의 공개 댓글");
        Comment secretSelfReply = persistComment(author, selfReply, true, author.getId(), "A의 비밀 답글");
        entityManager.flush();
        entityManager.clear();

        List<Comment> visible = commentRepository.findVisibleTree(group.getId(), host.getId(), null);

        assertThat(visible).extracting(Comment::getId)
                .contains(selfReply.getId(), secretSelfReply.getId());
    }

    private void assertVisible(User viewer, Comment... expected) {
        List<Comment> visible = commentRepository.findVisibleTree(group.getId(), viewer.getId(), null);
        assertThat(visible).extracting(Comment::getId)
                .containsExactlyElementsOf(List.of(expected).stream().map(Comment::getId).toList());
    }

    private User persistUser(String socialId) {
        User user = User.builder()
                .nickName(socialId)
                .socialType(SocialType.KAKAO)
                .socialId(socialId)
                .build();
        entityManager.persist(user);
        return user;
    }

    private Comment persistComment(
            User writer,
            Comment parent,
            boolean secret,
            Long secretTargetUserId,
            String content
    ) {
        Comment comment = Comment.builder()
                .group(group)
                .user(writer)
                .parent(parent)
                .secret(secret)
                .secretTargetUserId(secretTargetUserId)
                .content(content)
                .build();
        entityManager.persist(comment);
        return comment;
    }
}
