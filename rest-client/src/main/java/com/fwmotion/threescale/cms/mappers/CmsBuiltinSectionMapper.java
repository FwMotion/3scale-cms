package com.fwmotion.threescale.cms.mappers;

import com.fwmotion.threescale.cms.model.CmsBuiltinSection;
import com.redhat.threescale.rest.cms.model.BuiltinSection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CmsBuiltinSectionMapper {

    @Mapping(target = "path", source = "partialPath")
    CmsBuiltinSection fromRest(BuiltinSection builtinSection);

}
