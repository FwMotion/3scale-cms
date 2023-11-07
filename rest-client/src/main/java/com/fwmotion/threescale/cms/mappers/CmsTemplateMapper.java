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

    @Mapping(target = "draftContent", source = "draft")
    @Mapping(target = "publishedContent", source = "published")
    CmsLayout fromRestLayout(Layout layout);

    @Mapping(target = "type", constant = "LAYOUT")
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "layoutName", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    TemplateCreationRequest toRestLayoutCreation(CmsLayout layout);

    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "layoutName", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    TemplateUpdatableFields toRestLayoutUpdate(CmsLayout layout);

    @Mapping(target = "sectionId", ignore = true)
    CmsPage fromRestPage(Page page);

    @Mapping(target = "type", constant = "PAGE")
    @Mapping(target = "layoutName", source = "layout")
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
    @Mapping(target = "systemName", ignore = true)
    TemplateCreationRequest toRestPageCreation(CmsPage page);

    @Mapping(target = "layoutName", source = "layout")
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "layoutId", ignore = true)
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

    default CmsTemplate fromRest(Template template) {
        /* When upgraded to JDK21:
        return switch (template) {
            case null -> null;

            case BuiltinPage builtinPage -> fromRestBuiltinPage(builtinPage);
            case BuiltinPartial builtinPartial -> fromRestBuiltinPartial(builtinPartial);
            case Layout layout -> fromRestLayout(layout);
            case Page page -> fromRestPage(page);
            case Partial partial -> fromRestPartial(partial);

            default -> throw new UnsupportedOperationException("Unknown template type: " + template.getClass().getName());
        };
        */
        if (template == null) {
            return null;
        } else if (template instanceof BuiltinPage builtinPage) {
            return fromRestBuiltinPage(builtinPage);
        } else if (template instanceof BuiltinPartial builtinPartial) {
            return fromRestBuiltinPartial(builtinPartial);
        } else if (template instanceof Layout layout) {
            return fromRestLayout(layout);
        } else if (template instanceof Page page) {
            return fromRestPage(page);
        } else if (template instanceof Partial partial) {
            return fromRestPartial(partial);
        } else {
            throw new UnsupportedOperationException("Unknown template type: " + template.getClass().getName());
        }
    }

    default String mapHandlerFromRest(EnumHandler handler) {
        if (handler == null
            || handler == EnumHandler.UNKNOWN_DEFAULT_OPEN_API) {
            return null;
        }

        return handler.getValue();
    }

    default EnumHandler mapHandlerToRest(String handlerName) {
        EnumHandler enumHandler = EnumHandler.fromValue(handlerName);

        if (enumHandler == EnumHandler.UNKNOWN_DEFAULT_OPEN_API) {
            return null;
        }

        return enumHandler;
    }

}
