package ru.practicum.ewm.compilation;

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
@RequestMapping("/compilations")
@Validated
@RequiredArgsConstructor
public class PublicCompilationController {
    private final CompilationService service;

    @GetMapping
    public List<CompilationDto> getAll(@RequestParam(required = false) Boolean pinned,
                                       @RequestParam(defaultValue = "0") @Min(0) int from,
                                       @RequestParam(defaultValue = "10") @Min(1) int size) {
        return service.getAll(pinned, from, size);
    }

    @GetMapping("/{compId}")
    public CompilationDto getById(@PathVariable long compId) {
        return service.getById(compId);
    }
}
