package com.example.spabooking.article.dto;

import com.example.spabooking.article.entity.Article;
import java.time.LocalDateTime;

public class ArticleResponse {

    private Long id;
    private String title;
    private String slug;
    private String category;
    private String readTime;
    private String excerpt;
    private String content;
    private String coverImage;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArticleResponse fromEntity(Article article) {
        ArticleResponse resp = new ArticleResponse();
        resp.setId(article.getId());
        resp.setTitle(article.getTitle());
        resp.setSlug(article.getSlug());
        resp.setCategory(article.getCategory());
        resp.setReadTime(article.getReadTime());
        resp.setExcerpt(article.getExcerpt());
        resp.setContent(article.getContent());
        resp.setCoverImage(article.getCoverImage());
        resp.setStatus(article.getStatus());
        resp.setPublishedAt(article.getPublishedAt());
        resp.setCreatedAt(article.getCreatedAt());
        resp.setUpdatedAt(article.getUpdatedAt());
        return resp;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReadTime() { return readTime; }
    public void setReadTime(String readTime) { this.readTime = readTime; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
