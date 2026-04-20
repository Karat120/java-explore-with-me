package ru.practicum.stats.server.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.repository.EndpointHitRepository;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final EndpointHitRepository repository;
    private final StatsMapper mapper;

    @Transactional
    public void saveHit(EndpointHitDto endpointHit) {
        repository.save(mapper.toEntity(endpointHit));
    }

    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
        boolean hasUris = uris != null && !uris.isEmpty();
        if (unique && hasUris) {
            return repository.findUniqueStatsByUris(start, end, uris).stream().map(mapper::toDto).toList();
        }
        if (unique) {
            return repository.findUniqueStats(start, end).stream().map(mapper::toDto).toList();
        }
        if (hasUris) {
            return repository.findStatsByUris(start, end, uris).stream().map(mapper::toDto).toList();
        }
        return repository.findStats(start, end).stream().map(mapper::toDto).toList();
    }
}
