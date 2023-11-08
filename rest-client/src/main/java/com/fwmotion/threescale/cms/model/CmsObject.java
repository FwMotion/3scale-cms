package com.fwmotion.threescale.cms.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.OffsetDateTime;

public interface CmsObject {

    default boolean isBuiltin() {
        return false;
    }

    @Nonnull
    ThreescaleObjectType getType();

    @Nullable
    Long getId();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();

}
