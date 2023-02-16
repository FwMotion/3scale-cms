package com.fwmotion.threescale.cms.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.OffsetDateTime;
import java.util.Set;

public class CmsFile implements CmsObject {

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long id;
    private Long sectionId;
    private String path;
    private Set<String> tags;
    private Boolean downloadable;

    @Override
    public ThreescaleObjectType getType() {
        return ThreescaleObjectType.FILE;
    }

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

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Boolean getDownloadable() {
        return downloadable;
    }

    public void setDownloadable(Boolean downloadable) {
        this.downloadable = downloadable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof CmsFile)) return false;

        CmsFile cmsFile = (CmsFile) o;

        return new EqualsBuilder().append(getId(), cmsFile.getId()).isEquals();
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
            .append("sectionId", sectionId)
            .append("path", path)
            .append("tags", tags)
            .append("downloadable", downloadable)
            .toString();
    }
}
