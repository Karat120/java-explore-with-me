package ru.practicum.ewm.request;

import org.springframework.stereotype.Component;

@Component
public class RequestMapper {
    public ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus())
                .created(request.getCreated())
                .build();
    }
}
