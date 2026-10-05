package com.example.spabooking.article.service;

import com.example.spabooking.article.dto.CreateArticleRequest;
import com.example.spabooking.article.dto.UpdateArticleRequest;
import com.example.spabooking.article.entity.Article;
import com.example.spabooking.article.repository.ArticleRepository;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ArticleService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final ArticleRepository articleRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public ArticleService(ArticleRepository articleRepository, TenantRepository tenantRepository) {
        this.articleRepository = articleRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<Article> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return articleRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<Article> findAllPublished() {
        Long tenantId = TenantContext.requireTenantId();
        return articleRepository.findAllByTenantIdAndStatusOrderByPublishedAtDesc(tenantId, "PUBLISHED");
    }

    public Article findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return articleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
    }

    public Article findPublishedBySlugOrId(String slugOrId) {
        Long tenantId = TenantContext.requireTenantId();
        try {
            Long id = Long.parseLong(slugOrId);
            Article article = articleRepository.findByIdAndTenantId(id, tenantId).orElse(null);
            if (article != null && "PUBLISHED".equals(article.getStatus())) {
                return article;
            }
        } catch (NumberFormatException ignored) {
        }

        return articleRepository.findByTenantIdAndSlug(tenantId, slugOrId)
                .filter(a -> "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Published article not found: " + slugOrId));
    }

    @Transactional
    public Article create(CreateArticleRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.getTitle());
        } else {
            slug = toSlug(slug);
        }

        // Ensure unique slug per tenant
        String originalSlug = slug;
        int count = 1;
        while (articleRepository.existsByTenantIdAndSlug(tenantId, slug)) {
            slug = originalSlug + "-" + count++;
        }

        Article article = new Article();
        article.setTenant(tenant);
        article.setTitle(request.getTitle().trim());
        article.setSlug(slug);
        article.setCategory(request.getCategory());
        article.setReadTime(request.getReadTime());
        article.setExcerpt(request.getExcerpt());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());

        String status = "PUBLISHED".equalsIgnoreCase(request.getStatus()) ? "PUBLISHED" : "DRAFT";
        article.setStatus(status);
        if ("PUBLISHED".equals(status)) {
            article.setPublishedAt(LocalDateTime.now());
        }

        return articleRepository.save(article);
    }

    @Transactional
    public Article update(Long id, UpdateArticleRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Article existing = articleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));

        existing.setTitle(request.getTitle().trim());
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = toSlug(request.getSlug());
            if (!newSlug.equals(existing.getSlug())) {
                if (articleRepository.existsByTenantIdAndSlug(tenantId, newSlug)) {
                    throw new IllegalArgumentException("Slug already in use: " + newSlug);
                }
                existing.setSlug(newSlug);
            }
        }
        existing.setCategory(request.getCategory());
        existing.setReadTime(request.getReadTime());
        existing.setExcerpt(request.getExcerpt());
        existing.setContent(request.getContent());
        if (request.getCoverImage() != null) {
            existing.setCoverImage(request.getCoverImage());
        }

        if (request.getStatus() != null) {
            String newStatus = "PUBLISHED".equalsIgnoreCase(request.getStatus()) ? "PUBLISHED" : "DRAFT";
            if ("PUBLISHED".equals(newStatus) && !"PUBLISHED".equals(existing.getStatus())) {
                existing.setPublishedAt(LocalDateTime.now());
            }
            existing.setStatus(newStatus);
        }

        return articleRepository.save(existing);
    }

    @Transactional
    public Article publish(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Article article = articleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        article.setStatus("PUBLISHED");
        if (article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        return articleRepository.save(article);
    }

    @Transactional
    public Article unpublish(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Article article = articleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        article.setStatus("DRAFT");
        return articleRepository.save(article);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Article article = articleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        articleRepository.delete(article);
    }

    public static String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
