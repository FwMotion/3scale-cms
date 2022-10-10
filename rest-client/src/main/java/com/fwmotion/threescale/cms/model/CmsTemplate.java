package com.fwmotion.threescale.cms.model;

public interface CmsTemplate extends CmsObject {

    @Override
    default ThreescaleObjectType getType() {
        return ThreescaleObjectType.TEMPLATE;
    }

}
