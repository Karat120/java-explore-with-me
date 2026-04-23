package ru.practicum.ewm.event;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByInitiatorId(Long initiatorId, Pageable pageable);

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:usersEmpty = true OR e.initiator.id IN :users)
              AND (:statesEmpty = true OR cast(e.state as string) IN :states)
              AND (:catsEmpty = true OR e.category.id IN :categories)
              AND e.eventDate BETWEEN :rangeStart AND :rangeEnd
            """)
    Page<Event> findAdminEvents(@Param("users") List<Long> users,
                                @Param("usersEmpty") boolean usersEmpty,
                                @Param("states") List<String> states,
                                @Param("statesEmpty") boolean statesEmpty,
                                @Param("categories") List<Long> categories,
                                @Param("catsEmpty") boolean catsEmpty,
                                @Param("rangeStart") LocalDateTime rangeStart,
                                @Param("rangeEnd") LocalDateTime rangeEnd,
                                Pageable pageable);

    @Query("""
            SELECT e
            FROM Event e
            WHERE e.state = ru.practicum.ewm.event.EventState.PUBLISHED
              AND (:text = '' OR lower(e.annotation) LIKE lower(concat('%', :text, '%'))
                   OR lower(e.description) LIKE lower(concat('%', :text, '%')))
              AND (:categoriesEmpty = true OR e.category.id IN :categories)
              AND (:paid IS NULL OR e.paid = :paid)
              AND e.eventDate BETWEEN :rangeStart AND :rangeEnd
            """)
    Page<Event> findPublished(@Param("text") String text,
                              @Param("categories") List<Long> categories,
                              @Param("categoriesEmpty") boolean categoriesEmpty,
                              @Param("paid") Boolean paid,
                              @Param("rangeStart") LocalDateTime rangeStart,
                              @Param("rangeEnd") LocalDateTime rangeEnd,
                              Pageable pageable);
}
