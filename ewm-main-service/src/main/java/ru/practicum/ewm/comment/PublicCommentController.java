package ru.practicum.ewm.comment;

import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events/{eventId}/comments")
@Validated
@RequiredArgsConstructor
public class PublicCommentController {
    private final CommentUserService service;

    @GetMapping
    public List<CommentDto> getByEvent(@PathVariable long eventId,
                                       @RequestParam(defaultValue = "0") @Min(0) int from,
                                       @RequestParam(defaultValue = "10") @Min(1) int size) {
        return service.getPublicByEvent(eventId, from, size);
    }
}
