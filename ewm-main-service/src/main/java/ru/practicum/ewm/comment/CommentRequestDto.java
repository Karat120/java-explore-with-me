package ru.practicum.ewm.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDto {
    @NotBlank
    @Size(max = 2000)
    private String comment;
}
