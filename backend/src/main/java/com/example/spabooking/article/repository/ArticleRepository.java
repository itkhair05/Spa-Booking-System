package com.example.spabooking.article.repository;

import com.example.spabooking.article.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findAllByTenantId(Long tenantId);

    List<Article> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<Article> findAllByTenantIdAndStatusOrderByPublishedAtDesc(Long tenantId, String status);

    Optional<Article> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Article> findByTenantIdAndSlug(Long tenantId, String slug);

    boolean existsByTenantIdAndSlug(Long tenantId, String slug);
}
