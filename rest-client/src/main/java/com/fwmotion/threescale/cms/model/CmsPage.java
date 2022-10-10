package com.fwmotion.threescale.cms.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class CmsPage implements CmsTemplate {

    private Integer id;
    private String contentType;
    private String handler;
    private Boolean liquidEnabled;
    private String path;
    private Boolean hidden;
    private String layout;
    private String title;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof CmsPage)) return false;

        CmsPage cmsPage = (CmsPage) o;

        return new EqualsBuilder().append(getId(), cmsPage.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getId()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("contentType", contentType)
            .append("handler", handler)
            .append("liquidEnabled", liquidEnabled)
            .append("path", path)
            .append("hidden", hidden)
            .append("layout", layout)
            .append("title", title)
            .toString();
    }
}
