package ru.practicum.ewm.event;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import ru.practicum.ewm.request.ParticipationRequestRepository;
import ru.practicum.ewm.request.RequestStatus;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final long USER_HOURS_BEFORE_EVENT = 2;
    private static final long ADMIN_HOURS_BEFORE_PUBLISH = 1;
    private final EventRepository repository;
    private final EventMapper mapper;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Transactional
    public EventFullDto createByUser(long userId, NewEventDto dto) {
        validateUserEventDate(dto.getEventDate());
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
                .requestModeration(dto.isRequestModeration())
                .location(dto.getLocation())
                .title(dto.getTitle())
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
        Event saved = repository.save(event);
        return mapper.toFullDto(saved, 0L, 0L);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getByUser(long userId, int from, int size) {
        int page = from / size;
        userService.getByIdOrThrow(userId);
        return repository.findByInitiatorId(userId, PageRequest.of(page, size))
                .stream()
                .map(e -> mapper.toShortDto(e, 0L, getConfirmedRequests(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventFullDto getByUserAndId(long userId, long eventId) {
        Event event = getByIdOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("event not found");
        }
        return mapper.toFullDto(event, 0L, getConfirmedRequests(eventId));
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
        if (dto.getEventDate() != null) {
            validateUserEventDate(dto.getEventDate());
        }
        applyStateActionByUser(event, dto.getStateAction());
        patch(event, dto);
        Event saved = repository.save(event);
        return mapper.toFullDto(saved, getViews(List.of(saved.getId())).getOrDefault(saved.getId(), 0L), getConfirmedRequests(saved.getId()));
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> searchAdmin(List<Long> users, List<String> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        int page = from / size;
        List<Long> userIds = users == null ? List.of() : users;
        List<String> stateValues = states == null ? List.of() : states;
        List<Long> categoryIds = categories == null ? List.of() : categories;
        LocalDateTime start = rangeStart == null ? LocalDateTime.now().minusYears(20) : rangeStart;
        LocalDateTime end = rangeEnd == null ? LocalDateTime.now().plusYears(20) : rangeEnd;
        List<Event> events = repository.findAdminEvents(userIds, userIds.isEmpty(), stateValues, stateValues.isEmpty(),
                        categoryIds, categoryIds.isEmpty(), start, end, PageRequest.of(page, size))
                .getContent();
        Map<Long, Long> views = getViews(events.stream().map(Event::getId).toList());
        return events.stream()
                .map(e -> mapper.toFullDto(e, views.getOrDefault(e.getId(), 0L), getConfirmedRequests(e.getId())))
                .toList();
    }

    @Transactional
    public EventFullDto updateByAdmin(long eventId, UpdateEventDto dto) {
        Event event = getByIdOrThrow(eventId);
        if (dto.getEventDate() != null) {
            validateAdminEventDate(dto.getEventDate());
        }
        applyStateActionByAdmin(event, dto.getStateAction());
        patch(event, dto);
        Event saved = repository.save(event);
        return mapper.toFullDto(saved, getViews(List.of(saved.getId())).getOrDefault(saved.getId(), 0L), getConfirmedRequests(saved.getId()));
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            boolean onlyAvailable, String sort, int from, int size, HttpServletRequest request) {
        int page = from / size;
        LocalDateTime start = rangeStart == null ? LocalDateTime.now().minusYears(10) : rangeStart;
        LocalDateTime end = rangeEnd == null ? LocalDateTime.now().plusYears(10) : rangeEnd;
        List<Long> cats = categories == null ? List.of() : categories;
        List<Event> events = repository.findPublished(text, cats, cats.isEmpty(), paid, start, end,
                PageRequest.of(page, size, Sort.by("eventDate").descending())).getContent();
        if (onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 || getConfirmedRequests(e.getId()) < e.getParticipantLimit())
                    .toList();
        }
        Map<Long, Long> views = getViews(events.stream().map(Event::getId).toList());
        if ("VIEWS".equalsIgnoreCase(sort)) {
            events = events.stream()
                    .sorted((a, b) -> Long.compare(views.getOrDefault(b.getId(), 0L), views.getOrDefault(a.getId(), 0L)))
                    .toList();
        } else {
            events = events.stream().sorted((a, b) -> b.getEventDate().compareTo(a.getEventDate())).toList();
        }
        saveHit(request);
        return events.stream()
                .map(e -> mapper.toShortDto(e, views.getOrDefault(e.getId(), 0L), getConfirmedRequests(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventFullDto getPublishedById(long eventId, HttpServletRequest request) {
        Event event = getByIdOrThrow(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("event not found");
        }
        saveHit(request);
        long views = getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        return mapper.toFullDto(event, views, getConfirmedRequests(eventId));
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
        if (dto.getLocation() != null) {
            event.setLocation(dto.getLocation());
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
    }

    private void applyStateActionByUser(Event event, String stateAction) {
        if (stateAction == null) {
            return;
        }
        if (Objects.equals(stateAction, "SEND_TO_REVIEW")) {
            if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
                throw new ConflictException("only pending or canceled events can be changed");
            }
            event.setState(EventState.PENDING);
            return;
        }
        if (Objects.equals(stateAction, "CANCEL_REVIEW")) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("published event cannot be canceled");
            }
            event.setState(EventState.CANCELED);
        }
    }

    private void applyStateActionByAdmin(Event event, String stateAction) {
        if (stateAction == null) {
            return;
        }
        if (Objects.equals(stateAction, "PUBLISH_EVENT")) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("only pending event can be published");
            }
            validateAdminEventDate(event.getEventDate());
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
            return;
        }
        if (Objects.equals(stateAction, "REJECT_EVENT")) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("published event cannot be rejected");
            }
            event.setState(EventState.CANCELED);
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

    private long getConfirmedRequests(long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private void validateUserEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(USER_HOURS_BEFORE_EVENT))) {
            throw new ConflictException("event date must be at least 2 hours from now");
        }
    }

    private void validateAdminEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(ADMIN_HOURS_BEFORE_PUBLISH))) {
            throw new ConflictException("event date must be at least 1 hour from publication");
        }
    }
}
