package ru.practicum.ewm.category;

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
@RequestMapping("/categories")
@Validated
@RequiredArgsConstructor
public class PublicCategoryController {
    private final CategoryService service;

    @GetMapping
    public List<CategoryDto> getAll(@RequestParam(defaultValue = "0") @Min(0) int from,
                                    @RequestParam(defaultValue = "10") @Min(1) int size) {
        return service.getAll(from, size);
    }

    @GetMapping("/{catId}")
    public CategoryDto getById(@PathVariable long catId) {
        return service.getById(catId);
    }
}
