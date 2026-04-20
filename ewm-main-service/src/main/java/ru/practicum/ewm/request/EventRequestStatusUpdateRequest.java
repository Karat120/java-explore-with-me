package ru.practicum.ewm.request;

import java.util.List;
import lombok.Data;

@Data
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;
    private RequestStatus status;
}
