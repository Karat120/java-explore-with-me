package ru.practicum.ewm.compilation;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import lombok.Data;

@Data
public class NewCompilationDto {
    private Set<Long> events;
    private boolean pinned;
    @NotBlank
    private String title;
}
