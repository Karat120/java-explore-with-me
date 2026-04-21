package ru.practicum.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStatsDto {
    /** Application name that received hits. */
    private String app;
    /** Requested URI. */
    private String uri;
    /** Number of hits for the URI. */
    private Long hits;
}
