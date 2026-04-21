package ru.practicum.ewm.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.common.ConflictException;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventService;
import ru.practicum.ewm.event.EventState;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserService;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private ParticipationRequestRepository requestRepository;
    @Mock
    private RequestMapper requestMapper;
    @Mock
    private UserService userService;
    @Mock
    private EventService eventService;

    @InjectMocks
    private RequestService requestService;

    @Test
    void createShouldThrowWhenDuplicateExists() {
        User user = User.builder().id(1L).email("a@a.a").name("Name").build();
        Event event = Event.builder().id(2L).state(EventState.PUBLISHED).initiator(User.builder().id(3L).build()).build();

        when(userService.getByIdOrThrow(1L)).thenReturn(user);
        when(eventService.getByIdOrThrow(2L)).thenReturn(event);
        when(requestRepository.existsByEventIdAndRequesterId(2L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> requestService.create(1L, 2L));
    }

    @Test
    void updateStatusesShouldThrowWhenRequestIdsEmpty() {
        Event event = Event.builder().id(10L).initiator(User.builder().id(1L).build()).build();
        EventRequestStatusUpdateRequest req = new EventRequestStatusUpdateRequest();
        req.setRequestIds(List.of());
        req.setStatus(RequestStatus.CONFIRMED);

        when(eventService.getByIdOrThrow(10L)).thenReturn(event);

        assertThrows(ConflictException.class, () -> requestService.updateStatuses(1L, 10L, req));
    }

    @Test
    void createShouldAutoConfirmWhenModerationDisabled() {
        User requester = User.builder().id(1L).email("a@a.a").name("Name").build();
        Event event = Event.builder()
                .id(2L)
                .state(EventState.PUBLISHED)
                .initiator(User.builder().id(3L).build())
                .participantLimit(0)
                .requestModeration(false)
                .build();
        ParticipationRequest saved = ParticipationRequest.builder()
                .id(11L)
                .event(event)
                .requester(requester)
                .status(RequestStatus.CONFIRMED)
                .created(LocalDateTime.now())
                .build();
        ParticipationRequestDto dto = ParticipationRequestDto.builder().id(11L).status(RequestStatus.CONFIRMED).build();

        when(userService.getByIdOrThrow(1L)).thenReturn(requester);
        when(eventService.getByIdOrThrow(2L)).thenReturn(event);
        when(requestRepository.existsByEventIdAndRequesterId(2L, 1L)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(2L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(saved);
        when(requestMapper.toDto(saved)).thenReturn(dto);

        ParticipationRequestDto created = requestService.create(1L, 2L);

        assertEquals(RequestStatus.CONFIRMED, created.getStatus());
    }

    @Test
    void createShouldThrowWhenRequesterIsInitiator() {
        User sameUser = User.builder().id(1L).email("a@a.a").name("Name").build();
        Event event = Event.builder().id(2L).state(EventState.PUBLISHED).initiator(sameUser).build();

        when(userService.getByIdOrThrow(1L)).thenReturn(sameUser);
        when(eventService.getByIdOrThrow(2L)).thenReturn(event);

        assertThrows(ConflictException.class, () -> requestService.create(1L, 2L));
    }

    @Test
    void createShouldThrowWhenEventUnpublished() {
        User requester = User.builder().id(1L).email("a@a.a").name("Name").build();
        Event event = Event.builder().id(2L).state(EventState.PENDING).initiator(User.builder().id(3L).build()).build();

        when(userService.getByIdOrThrow(1L)).thenReturn(requester);
        when(eventService.getByIdOrThrow(2L)).thenReturn(event);

        assertThrows(ConflictException.class, () -> requestService.create(1L, 2L));
    }

    @Test
    void cancelShouldThrowWhenRequestDoesNotBelongToUser() {
        ParticipationRequest request = ParticipationRequest.builder()
                .id(10L)
                .requester(User.builder().id(2L).build())
                .build();
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(ru.practicum.ewm.common.NotFoundException.class, () -> requestService.cancel(1L, 10L));
    }
}
