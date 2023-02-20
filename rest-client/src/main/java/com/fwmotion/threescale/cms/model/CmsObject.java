package com.fwmotion.threescale.cms.model;

import java.time.OffsetDateTime;

public interface CmsObject {

    ThreescaleObjectType getType();

    Long getId();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();

}
