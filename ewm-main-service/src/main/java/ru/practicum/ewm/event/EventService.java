package ru.practicum.ewm.event;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.Category;
import ru.practicum.ewm.category.CategoryService;
import ru.practicum.ewm.common.ConflictException;
import ru.practicum.ewm.common.NotFoundException;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository repository;
    private final EventMapper mapper;
    private final UserService userService;
    private final CategoryService categoryService;
    private final StatsClient statsClient;

    @Transactional
    public EventFullDto createByUser(long userId, NewEventDto dto) {
        User initiator = userService.getByIdOrThrow(userId);
        Category category = categoryService.getByIdOrThrow(dto.getCategory());
        Event event = Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .initiator(initiator)
                .paid(dto.isPaid())
                .participantLimit(dto.getParticipantLimit())
                .title(dto.getTitle())
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
        return mapper.toFullDto(repository.save(event), 0L);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getByUser(long userId, int from, int size) {
        int page = from / size;
        userService.getByIdOrThrow(userId);
        return repository.findByInitiatorId(userId, PageRequest.of(page, size))
                .stream()
                .map(e -> mapper.toShortDto(e, 0L))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventFullDto getByUserAndId(long userId, long eventId) {
        Event event = getByIdOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("event not found");
        }
        return mapper.toFullDto(event, 0L);
    }

    @Transactional
    public EventFullDto updateByUser(long userId, long eventId, UpdateEventDto dto) {
        Event event = getByIdOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("event not found");
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("published event cannot be updated by user");
        }
        patch(event, dto);
        return mapper.toFullDto(repository.save(event), 0L);
    }

    @Transactional
    public EventFullDto publish(long eventId) {
        Event event = getByIdOrThrow(eventId);
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException("only pending event can be published");
        }
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());
        return mapper.toFullDto(repository.save(event), getViews(List.of(event.getId())).getOrDefault(event.getId(), 0L));
    }

    @Transactional
    public EventFullDto reject(long eventId) {
        Event event = getByIdOrThrow(eventId);
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("published event cannot be rejected");
        }
        event.setState(EventState.CANCELED);
        return mapper.toFullDto(repository.save(event), 0L);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            int from, int size, HttpServletRequest request) {
        int page = from / size;
        LocalDateTime start = rangeStart == null ? LocalDateTime.now().minusYears(10) : rangeStart;
        LocalDateTime end = rangeEnd == null ? LocalDateTime.now().plusYears(10) : rangeEnd;
        List<Long> cats = categories == null ? List.of() : categories;
        List<Event> events = repository.findPublished(text, cats, cats.isEmpty(), paid, start, end,
                PageRequest.of(page, size, Sort.by("eventDate").descending())).getContent();
        Map<Long, Long> views = getViews(events.stream().map(Event::getId).toList());
        saveHit(request);
        return events.stream().map(e -> mapper.toShortDto(e, views.getOrDefault(e.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public EventFullDto getPublishedById(long eventId, HttpServletRequest request) {
        Event event = getByIdOrThrow(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("event not found");
        }
        saveHit(request);
        long views = getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        return mapper.toFullDto(event, views);
    }

    @Transactional(readOnly = true)
    public Event getByIdOrThrow(long eventId) {
        return repository.findById(eventId).orElseThrow(() -> new NotFoundException("event not found"));
    }

    private void patch(Event event, UpdateEventDto dto) {
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getCategory() != null) {
            event.setCategory(categoryService.getByIdOrThrow(dto.getCategory()));
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
    }

    private void saveHit(HttpServletRequest request) {
        statsClient.hit(EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private Map<Long, Long> getViews(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> uris = eventIds.stream().map(id -> "/events/" + id).toList();
        List<ViewStatsDto> stats = statsClient.getStats(LocalDateTime.now().minusYears(10), LocalDateTime.now().plusYears(1), uris, true);
        return stats.stream()
                .filter(s -> s.getUri() != null && s.getHits() != null)
                .collect(Collectors.toMap(s -> Long.parseLong(s.getUri().substring(s.getUri().lastIndexOf('/') + 1)),
                        ViewStatsDto::getHits,
                        Long::sum));
    }
}
