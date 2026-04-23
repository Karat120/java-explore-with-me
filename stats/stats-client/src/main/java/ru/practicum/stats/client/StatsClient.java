package ru.practicum.stats.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

/**
 * HTTP client for the stats-service API.
 */
public final class StatsClient {
    /** Shared date-time format accepted by stats-service. */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** Rest client used to call stats-service. */
    private final RestClient restClient;

    /**
     * Creates a new client for the given stats-service base URL.
     *
     * @param serverUrl base URL of stats-service
     */
    public StatsClient(final String serverUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    /**
     * Sends hit information to stats-service.
     *
     * @param endpointHit hit payload
     */
    public void hit(final EndpointHitDto endpointHit) {
        Objects.requireNonNull(endpointHit, "endpointHit must not be null");
        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(endpointHit)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Requests aggregated view stats from stats-service.
     *
     * @param start range start
     * @param end range end
     * @param uris optional list of URIs
     * @param unique unique hits only flag
     * @return list of aggregated stats
     */
    public List<ViewStatsDto> getStats(final LocalDateTime start,
                                             final LocalDateTime end,
                                             final List<String> uris,
                                             final boolean unique) {
        Optional<List<String>> urisParam =
                (uris == null || uris.isEmpty())
                        ? Optional.empty()
                        : Optional.of(uris);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/stats")
                        .queryParam("start", start.format(FORMATTER))
                        .queryParam("end", end.format(FORMATTER))
                        .queryParamIfPresent("uris", urisParam)
                        .queryParam("unique", unique)
                        .build())
                .retrieve()
                .body(new org.springframework.core
                        .ParameterizedTypeReference<>() {
                });
    }
}
