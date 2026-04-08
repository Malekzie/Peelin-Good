package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.TagCreateRequest;
import com.sait.peelin.dto.v1.TagDto;
import com.sait.peelin.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Tags", description = "Product categorisation tags (e.g. Gluten-Free, Vegan). Create/update/delete require ADMIN role.")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "List tags", description = "Returns all product tags.")
    @ApiResponse(responseCode = "200", description = "List of tags returned")
    @GetMapping
    public List<TagDto> list() {
        return tagService.list();
    }

    @Operation(summary = "Get tag", description = "Returns a single tag by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag found"),
            @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content)
    })
    @GetMapping("/{id}")
    public TagDto get(@PathVariable Integer id) {
        return tagService.get(id);
    }

    @Operation(summary = "Create tag", description = "Create a new product tag. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tag created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TagDto create(@Valid @RequestBody TagCreateRequest req) {
        return tagService.create(req);
    }

    @Operation(summary = "Update tag", description = "Replace a tag's fields. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TagDto update(@PathVariable Integer id, @Valid @RequestBody TagCreateRequest req) {
        return tagService.update(id, req);
    }

    @Operation(summary = "Delete tag", description = "Permanently delete a tag. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag deleted"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        tagService.delete(id);
    }
}
