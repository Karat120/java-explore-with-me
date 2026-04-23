package ru.practicum.ewm.request;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequesterId(Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("""
            select r.event.id, count(r.id)
            from ParticipationRequest r
            where r.event.id in :eventIds and r.status = :status
            group by r.event.id
            """)
    List<Object[]> countByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
                                            @Param("status") RequestStatus status);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<ParticipationRequest> findByEventIdAndStatus(Long eventId, RequestStatus status);
}
