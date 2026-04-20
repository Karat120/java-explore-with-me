package ru.practicum.ewm.request;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.ConflictException;
import ru.practicum.ewm.common.NotFoundException;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventService;
import ru.practicum.ewm.event.EventState;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserService;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final ParticipationRequestRepository repository;
    private final RequestMapper mapper;
    private final UserService userService;
    private final EventService eventService;

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getByUser(long userId) {
        userService.getByIdOrThrow(userId);
        return repository.findByRequesterId(userId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public ParticipationRequestDto create(long userId, long eventId) {
        User requester = userService.getByIdOrThrow(userId);
        Event event = eventService.getByIdOrThrow(eventId);
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("initiator cannot request own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("cannot request unpublished event");
        }
        if (repository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("duplicate request");
        }
        long confirmed = repository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("participant limit reached");
        }
        RequestStatus status = (!event.isRequestModeration() || event.getParticipantLimit() == 0)
                ? RequestStatus.CONFIRMED : RequestStatus.PENDING;
        ParticipationRequest request = repository.save(ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .status(status)
                .created(LocalDateTime.now())
                .build());
        return mapper.toDto(request);
    }

    @Transactional
    public ParticipationRequestDto cancel(long userId, long requestId) {
        ParticipationRequest request = repository.findById(requestId).orElseThrow(() -> new NotFoundException("request not found"));
        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("request not found");
        }
        request.setStatus(RequestStatus.CANCELED);
        return mapper.toDto(repository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(long userId, long eventId) {
        Event event = eventService.getByIdOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("event not found");
        }
        return repository.findByEventId(eventId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public EventRequestStatusUpdateResult updateStatuses(long userId, long eventId, EventRequestStatusUpdateRequest req) {
        Event event = eventService.getByIdOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("event not found");
        }
        List<ParticipationRequest> requests = repository.findAllById(req.getRequestIds());
        List<ParticipationRequestDto> confirmed = new java.util.ArrayList<>();
        List<ParticipationRequestDto> rejected = new java.util.ArrayList<>();
        long confirmedCount = repository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        for (ParticipationRequest r : requests) {
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("only pending requests can be updated");
            }
            if (req.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
                    throw new ConflictException("participant limit reached");
                }
                r.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmed.add(mapper.toDto(repository.save(r)));
            } else {
                r.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toDto(repository.save(r)));
            }
        }
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            List<ParticipationRequest> pending = repository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (ParticipationRequest pendingRequest : pending) {
                pendingRequest.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toDto(repository.save(pendingRequest)));
            }
        }
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }
}
