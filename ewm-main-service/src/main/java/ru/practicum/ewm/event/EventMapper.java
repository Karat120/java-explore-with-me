package ru.practicum.ewm.event;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.category.CategoryMapper;

@Component
public class EventMapper {
    private final CategoryMapper categoryMapper;

    public EventMapper(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public EventShortDto toShortDto(Event event, long views) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .initiatorId(event.getInitiator().getId())
                .eventDate(event.getEventDate())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public EventFullDto toFullDto(Event event, long views) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiatorId(event.getInitiator().getId())
                .paid(event.isPaid())
                .participantLimit(event.getParticipantLimit())
                .title(event.getTitle())
                .state(event.getState())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .views(views)
                .build();
    }
}
