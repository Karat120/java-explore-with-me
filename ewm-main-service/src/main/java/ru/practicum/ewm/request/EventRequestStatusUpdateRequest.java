package ru.practicum.ewm.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class EventRequestStatusUpdateRequest {
    @NotEmpty
    private List<Long> requestIds;
    @NotNull
    private RequestStatus status;
}
