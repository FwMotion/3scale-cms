package com.fwmotion.threescale.cms.cli.config;

import com.fwmotion.threescale.cms.mixins.*;
import com.redhat.threescale.rest.cms.model.*;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
    EnumHandlerMixIn.class,
    FileListMixIn.class,
    SectionListMixIn.class,
    TemplateListMixIn.class,
    TemplateMergingList.class,

    BuiltinPage.class,
    BuiltinPartial.class,
    EnumHandler.class,
    EnumTemplateType.class,
    ErrorHash.class,
    FileCreationRequest.class,
    FileList.class,
    FileUpdatableFields.class,
    Layout.class,
    ListPaginationAttributes.class,
    ModelFile.class,
    Page.class,
    Partial.class,
    ProviderAccount.class,
    ProviderPlan.class,
    ProviderUser.class,
    Section.class,
    SectionCreationRequest.class,
    SectionList.class,
    SectionUpdatableFields.class,
    Template.class,
    TemplateCommon.class,
    TemplateCreationRequest.class,
    TemplateList.class,
    TemplateList.class,
    TemplateUpdatableFields.class,
})
public class ReflectionConfiguration {
}

