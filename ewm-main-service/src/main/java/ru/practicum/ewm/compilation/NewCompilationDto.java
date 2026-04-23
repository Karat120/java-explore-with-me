package ru.practicum.ewm.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Data;

@Data
public class NewCompilationDto {
    private Set<Long> events;
    private boolean pinned;
    @NotBlank
    @Size(min = 1, max = 50)
    private String title;
}
