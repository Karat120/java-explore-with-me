package ru.practicum.ewm.category;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.NotFoundException;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    @Transactional
    public CategoryDto create(CategoryDto dto) {
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    @Transactional
    public CategoryDto update(long catId, CategoryDto dto) {
        Category category = repository.findById(catId).orElseThrow(() -> new NotFoundException("category not found"));
        category.setName(dto.getName());
        return mapper.toDto(repository.save(category));
    }

    @Transactional
    public void delete(long catId) {
        if (!repository.existsById(catId)) {
            throw new NotFoundException("category not found");
        }
        repository.deleteById(catId);
    }

    @Transactional(readOnly = true)
    public Category getByIdOrThrow(long catId) {
        return repository.findById(catId).orElseThrow(() -> new NotFoundException("category not found"));
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(long catId) {
        return mapper.toDto(getByIdOrThrow(catId));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(int from, int size) {
        int page = from / size;
        return repository.findAll(PageRequest.of(page, size)).stream().map(mapper::toDto).toList();
    }
}
