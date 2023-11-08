package com.fwmotion.threescale.cms.model;

public interface CmsBuiltinTemplate extends CmsTemplate {

    @Override
    default boolean isBuiltin() {
        return true;
    }

}
