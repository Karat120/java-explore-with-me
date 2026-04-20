package ru.practicum.stats.server.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.repository.ViewStatsView;

@Component
public class StatsMapper {
    public EndpointHit toEntity(EndpointHitDto dto) {
        return EndpointHit.builder()
                .id(dto.getId())
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }

    public ViewStatsDto toDto(ViewStatsView view) {
        return ViewStatsDto.builder()
                .app(view.getApp())
                .uri(view.getUri())
                .hits(view.getHits())
                .build();
    }
}
