package ru.practicum.stats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {
    /** Hit identifier. */
    private Long id;

    /** Application name that produced the hit. */
    @NotBlank
    private String app;

    /** Requested URI. */
    @NotBlank
    private String uri;

    /** Client IP address. */
    @NotBlank
    private String ip;

    /** Request timestamp. */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
