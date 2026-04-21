package ru.practicum.ewm.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.category.CategoryService;
import ru.practicum.ewm.request.ParticipationRequestRepository;
import ru.practicum.ewm.request.RequestStatus;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserService;
import ru.practicum.stats.client.StatsClient;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private UserService userService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private ParticipationRequestRepository requestRepository;
    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private EventService eventService;

    @Test
    void createByUserShouldThrowWhenEventDateTooEarly() {
        NewEventDto dto = new NewEventDto();
        dto.setAnnotation("valid annotation text with enough length");
        dto.setDescription("valid description text with enough length");
        dto.setCategory(1L);
        dto.setTitle("Title ok");
        dto.setLocation(new Location(10.0f, 10.0f));
        dto.setEventDate(LocalDateTime.now().plusMinutes(30));

        assertThrows(IllegalArgumentException.class, () -> eventService.createByUser(1L, dto));
    }

    @Test
    void updateByAdminShouldThrowWhenPublishingEventTooEarly() {
        Event event = Event.builder()
                .id(10L)
                .state(EventState.PENDING)
                .eventDate(LocalDateTime.now().plusMinutes(30))
                .build();
        UpdateEventDto dto = new UpdateEventDto();
        dto.setStateAction("PUBLISH_EVENT");

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(IllegalArgumentException.class, () -> eventService.updateByAdmin(10L, dto));
    }

    @Test
    void updateByUserShouldThrowWhenNotInitiator() {
        Event event = Event.builder()
                .id(10L)
                .state(EventState.PENDING)
                .initiator(User.builder().id(2L).build())
                .build();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(ru.practicum.ewm.common.NotFoundException.class, () -> eventService.updateByUser(1L, 10L, new UpdateEventDto()));
    }

    @Test
    void updateByUserShouldThrowWhenPublished() {
        Event event = Event.builder()
                .id(10L)
                .state(EventState.PUBLISHED)
                .initiator(User.builder().id(1L).build())
                .build();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(ru.practicum.ewm.common.ConflictException.class, () -> eventService.updateByUser(1L, 10L, new UpdateEventDto()));
    }

    @Test
    void searchAdminShouldThrowWhenRangeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> eventService.searchAdmin(
                List.of(), List.of(), List.of(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now(),
                0, 10));
    }

    @Test
    void searchPublicShouldThrowWhenSortInvalid() {
        assertThrows(IllegalArgumentException.class, () -> eventService.searchPublic(
                null, List.of(), null, null, null, false, "BAD_SORT", 0, 10, null));
    }

    @Test
    void getPublishedByIdShouldThrowWhenStateNotPublished() {
        Event event = Event.builder().id(10L).state(EventState.PENDING).build();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(ru.practicum.ewm.common.NotFoundException.class, () -> eventService.getPublishedById(10L, null));
    }

    @Test
    void updateByAdminShouldThrowWhenPublishFromWrongState() {
        Event event = Event.builder()
                .id(10L)
                .state(EventState.CANCELED)
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        UpdateEventDto dto = new UpdateEventDto();
        dto.setStateAction("PUBLISH_EVENT");
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(ru.practicum.ewm.common.ConflictException.class, () -> eventService.updateByAdmin(10L, dto));
    }

    @Test
    void updateByAdminShouldRejectPublishedEvent() {
        Event event = Event.builder()
                .id(10L)
                .state(EventState.PUBLISHED)
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        UpdateEventDto dto = new UpdateEventDto();
        dto.setStateAction("REJECT_EVENT");
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(ru.practicum.ewm.common.ConflictException.class, () -> eventService.updateByAdmin(10L, dto));
    }

    @Test
    void updateByUserShouldCancelReviewAndSave() {
        Event event = Event.builder()
                .id(10L)
                .state(EventState.PENDING)
                .initiator(User.builder().id(1L).build())
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        UpdateEventDto dto = new UpdateEventDto();
        dto.setStateAction("CANCEL_REVIEW");

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toFullDto(any(Event.class), any(Long.class), any(Long.class))).thenReturn(EventFullDto.builder().id(10L).build());
        when(requestRepository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(0L);

        eventService.updateByUser(1L, 10L, dto);

        verify(eventRepository).save(any(Event.class));
    }
}
