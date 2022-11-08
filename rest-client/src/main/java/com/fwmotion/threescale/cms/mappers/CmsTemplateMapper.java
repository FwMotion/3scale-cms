package com.fwmotion.threescale.cms.mappers;

import com.fwmotion.threescale.cms.model.*;
import com.redhat.threescale.rest.cms.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CmsTemplateMapper {

    CmsBuiltinPage fromRestBuiltinPage(BuiltinPage builtinPage);

    @Mapping(target = "layoutName", source = "layout")
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    TemplateUpdatableFields toRestBuiltinPage(CmsBuiltinPage builtinPage);

    CmsBuiltinPartial fromRestBuiltinPartial(BuiltinPartial builtinPartial);

    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "layoutName", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "title", ignore = true)
    TemplateUpdatableFields toRestBuiltinPartial(CmsBuiltinPartial builtinPartial);

    CmsLayout fromRestLayout(Layout layout);

    @Mapping(target = "type", constant = "LAYOUT")
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "layoutName", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "title", ignore = true)
    TemplateCreationRequest toRestLayoutCreation(CmsLayout layout);

    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "layoutName", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "title", ignore = true)
    TemplateUpdatableFields toRestLayoutUpdate(CmsLayout layout);

    CmsPage fromRestPage(Page page);

    @Mapping(target = "type", constant = "PAGE")
    @Mapping(target = "layoutName", source = "layout")
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "systemName", ignore = true)
    TemplateCreationRequest toRestPageCreation(CmsPage page);

    @Mapping(target = "layoutName", source = "layout")
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "systemName", ignore = true)
    TemplateUpdatableFields toRestPageUpdate(CmsPage page);

    CmsPartial fromRestPartial(Partial partial);

    @Mapping(target = "type", constant = "PARTIAL")
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "layoutName", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "title", ignore = true)
    TemplateCreationRequest toRestPartialCreation(CmsPartial partial);

    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "layoutName", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "title", ignore = true)
    TemplateUpdatableFields toRestPartialUpdate(CmsPartial partial);

}
