package ru.practicum.ewm.comment;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.NotFoundException;

@Service
@RequiredArgsConstructor
public class CommentAdminService {
    private final CommentRepository repository;
    private final CommentMapper mapper;

    @Transactional(readOnly = true)
    public List<CommentDto> getAdmin(Long userId, Long eventId, int from, int size) {
        int page = from / size;
        return repository.findAdmin(userId, eventId, PageRequest.of(page, size))
                .map(mapper::toDto)
                .getContent();
    }

    @Transactional(readOnly = true)
    public CommentDto getById(long commentId) {
        return mapper.toDto(getByIdOrThrow(commentId));
    }

    @Transactional
    public void deleteByAdmin(long commentId) {
        getByIdOrThrow(commentId);
        repository.deleteById(commentId);
    }

    private Comment getByIdOrThrow(long commentId) {
        return repository.findById(commentId).orElseThrow(() -> new NotFoundException("comment not found"));
    }
}
