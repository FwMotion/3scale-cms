package com.fwmotion.threescale.cms.mappers;

import com.fwmotion.threescale.cms.model.CmsSection;
import com.redhat.threescale.rest.cms.model.Section;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CmsSectionMapper {

    @Mapping(target = "path", source = "partialPath")
    CmsSection fromRest(Section section);

    @Mapping(target = "partialPath", source = "path")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "_public", ignore = true)
    Section toRest(CmsSection section);

}
