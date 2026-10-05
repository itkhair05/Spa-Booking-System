package com.example.spabooking.article.controller;

import com.example.spabooking.article.dto.ArticleResponse;
import com.example.spabooking.article.dto.CreateArticleRequest;
import com.example.spabooking.article.dto.UpdateArticleRequest;
import com.example.spabooking.article.entity.Article;
import com.example.spabooking.article.service.ArticleService;
import com.example.spabooking.common.storage.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final FileStorageService fileStorageService;

    @Autowired
    public ArticleController(ArticleService articleService, FileStorageService fileStorageService) {
        this.articleService = articleService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<ArticleResponse>> getAll() {
        List<Article> articles = articleService.findAll();
        List<ArticleResponse> response = articles.stream()
                .map(ArticleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ArticleResponse> getById(@PathVariable Long id) {
        Article article = articleService.findById(id);
        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody CreateArticleRequest request) {
        Article created = articleService.create(request);
        return new ResponseEntity<>(ArticleResponse.fromEntity(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ArticleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateArticleRequest request) {
        Article updated = articleService.update(id, request);
        return ResponseEntity.ok(ArticleResponse.fromEntity(updated));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ArticleResponse> publish(@PathVariable Long id) {
        Article published = articleService.publish(id);
        return ResponseEntity.ok(ArticleResponse.fromEntity(published));
    }

    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ArticleResponse> unpublish(@PathVariable Long id) {
        Article unpublished = articleService.unpublish(id);
        return ResponseEntity.ok(ArticleResponse.fromEntity(unpublished));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cover")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ArticleResponse> uploadCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file, "articles");
        Article article = articleService.findById(id);
        article.setCoverImage(fileUrl);
        UpdateArticleRequest req = new UpdateArticleRequest();
        req.setTitle(article.getTitle());
        req.setSlug(article.getSlug());
        req.setCategory(article.getCategory());
        req.setReadTime(article.getReadTime());
        req.setExcerpt(article.getExcerpt());
        req.setContent(article.getContent());
        req.setCoverImage(fileUrl);
        req.setStatus(article.getStatus());
        Article updated = articleService.update(id, req);
        return ResponseEntity.ok(ArticleResponse.fromEntity(updated));
    }
}
