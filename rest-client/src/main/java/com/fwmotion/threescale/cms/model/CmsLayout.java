package com.fwmotion.threescale.cms.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.OffsetDateTime;

public class CmsLayout implements CmsTemplate {

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long id;
    private String systemName;
    private String title;
    private String contentType;
    private String handler;
    private Boolean liquidEnabled;
    private String draftContent;
    private String publishedContent;

    @Override
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public Boolean getLiquidEnabled() {
        return liquidEnabled;
    }

    public void setLiquidEnabled(Boolean liquidEnabled) {
        this.liquidEnabled = liquidEnabled;
    }

    public String getDraftContent() {
        return draftContent;
    }

    public void setDraftContent(String draftContent) {
        this.draftContent = draftContent;
    }

    public String getPublishedContent() {
        return publishedContent;
    }

    public void setPublishedContent(String publishedContent) {
        this.publishedContent = publishedContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof CmsLayout cmsLayout)) return false;

        return new EqualsBuilder().append(getId(), cmsLayout.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getId()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("createdAt", createdAt)
            .append("updatedAt", updatedAt)
            .append("id", id)
            .append("systemName", systemName)
            .append("contentType", contentType)
            .append("handler", handler)
            .append("liquidEnabled", liquidEnabled)
            .append("draftContent", draftContent)
            .append("publishedContent", publishedContent)
            .toString();
    }
}
