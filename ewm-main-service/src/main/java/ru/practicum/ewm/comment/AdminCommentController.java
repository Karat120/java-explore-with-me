package ru.practicum.ewm.comment;

import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/comments")
@Validated
@RequiredArgsConstructor
public class AdminCommentController {
    private final CommentService service;

    @GetMapping
    public List<CommentDto> getAll(@RequestParam(required = false) Long userId,
                                   @RequestParam(required = false) Long eventId,
                                   @RequestParam(defaultValue = "0") @Min(0) int from,
                                   @RequestParam(defaultValue = "10") @Min(1) int size) {
        return service.getAdmin(userId, eventId, from, size);
    }

    @GetMapping("/{commentId}")
    public CommentDto getById(@PathVariable long commentId) {
        return service.getById(commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long commentId) {
        service.deleteByAdmin(commentId);
    }
}
