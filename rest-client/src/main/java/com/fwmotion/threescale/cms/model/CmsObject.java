package com.fwmotion.threescale.cms.model;

import java.time.OffsetDateTime;

public interface CmsObject {

    ThreescaleObjectType getType();

    Integer getId();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();

}
