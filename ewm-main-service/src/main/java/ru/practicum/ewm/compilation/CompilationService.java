package ru.practicum.ewm.compilation;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.NotFoundException;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventMapper;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.EventShortDto;

@Service
@RequiredArgsConstructor
public class CompilationService {
    private final CompilationRepository repository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        Compilation comp = new Compilation();
        comp.setTitle(dto.getTitle());
        comp.setPinned(dto.isPinned());
        comp.setEvents(resolveEvents(dto.getEvents()));
        return toDto(repository.save(comp));
    }

    @Transactional
    public void delete(long compId) {
        if (!repository.existsById(compId)) {
            throw new NotFoundException("compilation not found");
        }
        repository.deleteById(compId);
    }

    @Transactional
    public CompilationDto update(long compId, UpdateCompilationRequest req) {
        Compilation comp = repository.findById(compId).orElseThrow(() -> new NotFoundException("compilation not found"));
        if (req.getTitle() != null) {
            comp.setTitle(req.getTitle());
        }
        if (req.getPinned() != null) {
            comp.setPinned(req.getPinned());
        }
        if (req.getEvents() != null) {
            comp.setEvents(resolveEvents(req.getEvents()));
        }
        return toDto(repository.save(comp));
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        int page = from / size;
        if (pinned == null) {
            return repository.findAll(PageRequest.of(page, size)).stream().map(this::toDto).toList();
        }
        return repository.findByPinned(pinned, PageRequest.of(page, size)).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CompilationDto getById(long compId) {
        return toDto(repository.findById(compId).orElseThrow(() -> new NotFoundException("compilation not found")));
    }

    private Set<Event> resolveEvents(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return new java.util.HashSet<>(eventRepository.findAllById(ids));
    }

    private CompilationDto toDto(Compilation comp) {
        List<EventShortDto> events = comp.getEvents().stream()
                .map(e -> eventMapper.toShortDto(e, 0L, 0L))
                .toList();
        return CompilationDto.builder()
                .id(comp.getId())
                .events(events)
                .pinned(comp.isPinned())
                .title(comp.getTitle())
                .build();
    }
}
