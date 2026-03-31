package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.TagCreateRequest;
import com.sait.peelin.dto.v1.TagDto;
import com.sait.peelin.service.TagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Tag(name = "Tags")
public class TagController {

    private final TagService tagService;

    @GetMapping
    public List<TagDto> list() {
        return tagService.list();
    }

    @GetMapping("/{id}")
    public TagDto get(@PathVariable Integer id) {
        return tagService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TagDto create(@Valid @RequestBody TagCreateRequest req) {
        return tagService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TagDto update(@PathVariable Integer id, @Valid @RequestBody TagCreateRequest req) {
        return tagService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        tagService.delete(id);
    }
}
