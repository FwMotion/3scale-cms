package com.fwmotion.threescale.cms.mappers;

import com.fwmotion.threescale.cms.model.*;
import com.redhat.threescale.rest.cms.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CmsTemplateMapper {

    CmsBuiltinPage fromRestBuiltinPage(BuiltinPage builtinPage);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "published", ignore = true)
    BuiltinPage toRestBuiltinPage(CmsBuiltinPage builtinPage);

    CmsBuiltinPartial fromRestBuiltinPartial(BuiltinPartial builtinPartial);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "published", ignore = true)
    BuiltinPartial toRestBuiltinPartial(CmsBuiltinPartial builtinPartial);

    CmsLayout fromRestLayout(Layout layout);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "published", ignore = true)
    Layout toRestLayout(CmsLayout layout);

    CmsPage fromRestPage(Page page);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "published", ignore = true)
    Page toRestPage(CmsPage page);

    CmsPartial fromRestPartial(Partial partial);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "published", ignore = true)
    Partial toRestPartial(CmsPartial partial);

}
