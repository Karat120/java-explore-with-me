package ru.practicum.ewm.comment;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
public class CommentUserService {
    private final CommentRepository repository;
    private final CommentMapper mapper;
    private final UserService userService;
    private final EventService eventService;

    @Transactional
    public CommentDto create(long userId, long eventId, CommentRequestDto dto) {
        User author = userService.getByIdOrThrow(userId);
        Event event = eventService.getByIdOrThrow(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("comments are allowed only for published events");
        }
        LocalDateTime now = LocalDateTime.now();
        Comment comment = repository.save(Comment.builder()
                .event(event)
                .author(author)
                .text(dto.getComment())
                .created(now)
                .updated(now)
                .build());
        return mapper.toDto(comment);
    }

    @Transactional
    public CommentDto updateByUser(long userId, long commentId, CommentRequestDto dto) {
        Comment comment = getByIdOrThrow(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("comment not found");
        }
        comment.setText(dto.getComment());
        comment.setUpdated(LocalDateTime.now());
        return mapper.toDto(repository.save(comment));
    }

    @Transactional
    public void deleteByUser(long userId, long commentId) {
        Comment comment = getByIdOrThrow(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("comment not found");
        }
        repository.deleteById(commentId);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getByUser(long userId, int from, int size) {
        userService.getByIdOrThrow(userId);
        int page = from / size;
        return repository.findByAuthorIdOrderByCreatedDesc(userId, PageRequest.of(page, size))
                .map(mapper::toDto)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getPublicByEvent(long eventId, int from, int size) {
        eventService.getByIdOrThrow(eventId);
        int page = from / size;
        return repository.findByEventIdAndEventStateOrderByCreatedAsc(eventId, EventState.PUBLISHED, PageRequest.of(page, size))
                .map(mapper::toDto)
                .getContent();
    }

    private Comment getByIdOrThrow(long commentId) {
        return repository.findById(commentId).orElseThrow(() -> new NotFoundException("comment not found"));
    }
}
