package ru.practicum.ewm.comment;

import org.springframework.stereotype.Component;

@Component
public class CommentMapper {
    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .eventId(comment.getEvent().getId())
                .userId(comment.getAuthor().getId())
                .comment(comment.getText())
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .build();
    }
}
