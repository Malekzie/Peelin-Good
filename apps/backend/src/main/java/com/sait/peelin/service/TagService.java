package com.sait.peelin.service;

import com.sait.peelin.dto.v1.TagCreateRequest;
import com.sait.peelin.dto.v1.TagDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Tag;
import com.sait.peelin.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Cacheable(value = "tags", key = "'all'")
    public List<TagDto> list() {
        return tagRepository.findAll().stream().map(CatalogMapper::tag).toList();
    }

    @Cacheable(value = "tags", key = "#id")
    public TagDto get(Integer id) {
        Tag t = tagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        return CatalogMapper.tag(t);
    }

    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public TagDto create(TagCreateRequest req) {
        Tag t = new Tag();
        t.setTagName(req.getName().trim());
        t.setDietary(req.isDietary());
        return CatalogMapper.tag(tagRepository.save(t));
    }

    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public TagDto update(Integer id, TagCreateRequest req) {
        Tag t = tagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        t.setTagName(req.getName().trim());
        t.setDietary(req.isDietary());
        return CatalogMapper.tag(tagRepository.save(t));
    }

    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public void delete(Integer id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag not found");
        }
        tagRepository.deleteById(id);
    }
}
