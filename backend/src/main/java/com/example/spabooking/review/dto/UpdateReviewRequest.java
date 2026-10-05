package com.example.spabooking.review.dto;

public class UpdateReviewRequest {

    private Boolean isPublished;
    private Integer displayOrder;
    private String comment;

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
