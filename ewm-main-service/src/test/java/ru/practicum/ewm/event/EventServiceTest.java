package ru.practicum.ewm.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.category.CategoryService;
import ru.practicum.ewm.common.ConflictException;
import ru.practicum.ewm.request.ParticipationRequestRepository;
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

        assertThrows(ConflictException.class, () -> eventService.createByUser(1L, dto));
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

        assertThrows(ConflictException.class, () -> eventService.updateByAdmin(10L, dto));
    }
}
