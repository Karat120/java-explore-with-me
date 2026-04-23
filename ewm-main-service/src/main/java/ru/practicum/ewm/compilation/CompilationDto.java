package ru.practicum.ewm.compilation;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.event.EventShortDto;

@Data
@Builder
public class CompilationDto {
    private Long id;
    private List<EventShortDto> events;
    private boolean pinned;
    private String title;
}
