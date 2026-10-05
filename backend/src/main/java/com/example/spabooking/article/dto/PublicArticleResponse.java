package com.example.spabooking.article.dto;

import com.example.spabooking.article.entity.Article;
import java.time.LocalDateTime;

public class PublicArticleResponse {

    private Long id;
    private String title;
    private String slug;
    private String category;
    private String readTime;
    private String excerpt;
    private String content;
    private String coverImage;
    private LocalDateTime publishedAt;

    public static PublicArticleResponse fromEntity(Article article) {
        PublicArticleResponse resp = new PublicArticleResponse();
        resp.setId(article.getId());
        resp.setTitle(article.getTitle());
        resp.setSlug(article.getSlug());
        resp.setCategory(article.getCategory());
        resp.setReadTime(article.getReadTime());
        resp.setExcerpt(article.getExcerpt());
        resp.setContent(article.getContent());
        resp.setCoverImage(article.getCoverImage());
        resp.setPublishedAt(article.getPublishedAt());
        return resp;
    }

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

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
