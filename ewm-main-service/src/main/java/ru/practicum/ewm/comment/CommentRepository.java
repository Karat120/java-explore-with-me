package ru.practicum.ewm.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByAuthorIdOrderByCreatedDesc(Long authorId, Pageable pageable);

    Page<Comment> findByEventIdAndEventStateOrderByCreatedAsc(Long eventId, ru.practicum.ewm.event.EventState state, Pageable pageable);

    @Query("""
            select c
            from Comment c
            where (:userId is null or c.author.id = :userId)
              and (:eventId is null or c.event.id = :eventId)
            order by c.created desc
            """)
    Page<Comment> findAdmin(@Param("userId") Long userId, @Param("eventId") Long eventId, Pageable pageable);
}
