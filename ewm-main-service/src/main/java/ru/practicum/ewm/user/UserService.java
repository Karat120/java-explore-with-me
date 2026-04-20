package ru.practicum.ewm.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.NotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;

    @Transactional
    public UserDto create(UserDto userDto) {
        return mapper.toDto(repository.save(mapper.toEntity(userDto)));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAll(int from, int size) {
        int page = from / size;
        return repository.findAll(PageRequest.of(page, size)).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllByIds(List<Long> ids, int from, int size) {
        if (ids == null || ids.isEmpty()) {
            return getAll(from, size);
        }
        int start = Math.min(from, ids.size());
        int end = Math.min(from + size, ids.size());
        List<Long> pageIds = ids.subList(start, end);
        return repository.findByIdIn(pageIds).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public void delete(long userId) {
        if (!repository.existsById(userId)) {
            throw new NotFoundException("user not found");
        }
        repository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public User getByIdOrThrow(long userId) {
        return repository.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
    }
}
