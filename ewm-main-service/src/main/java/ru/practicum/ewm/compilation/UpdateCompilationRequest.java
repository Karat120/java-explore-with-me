package ru.practicum.ewm.compilation;

import java.util.Set;
import lombok.Data;

@Data
public class UpdateCompilationRequest {
    private Set<Long> events;
    private Boolean pinned;
    private String title;
}
