package com.fwmotion.threescale.cms.model;

import jakarta.annotation.Nonnull;

public interface CmsTemplate extends CmsObject {

    @Nonnull
    @Override
    default ThreescaleObjectType getType() {
        return ThreescaleObjectType.TEMPLATE;
    }

}
