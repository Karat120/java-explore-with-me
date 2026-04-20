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
