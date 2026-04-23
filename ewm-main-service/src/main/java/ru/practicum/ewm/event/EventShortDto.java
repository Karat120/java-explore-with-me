package ru.practicum.ewm.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.category.CategoryDto;
import ru.practicum.ewm.user.UserShortDto;

@Data
@Builder
public class EventShortDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private UserShortDto initiator;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
    private Boolean paid;
    private String title;
    private Long views;
}
