package com.fwmotion.threescale.cms.model;

public class CmsSection implements CmsObject{

    private final Integer id;

    public CmsSection(Integer id) {
        this.id = id;
    }

    @Override
    public ThreescaleObjectType getType() {
        return ThreescaleObjectType.SECTION;
    }

    @Override
    public Integer getId() {
        return id;
    }
}
