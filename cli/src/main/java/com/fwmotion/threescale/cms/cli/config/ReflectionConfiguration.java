package com.fwmotion.threescale.cms.cli.config;

import com.fwmotion.threescale.cms.mixins.EnumHandlerMixIn;
import com.redhat.threescale.rest.cms.model.*;
import io.quarkus.runtime.annotations.RegisterForReflection;

@SuppressWarnings("unused")
@RegisterForReflection(targets = {
    EnumHandlerMixIn.class,

    BuiltinPage.class,
    BuiltinPartial.class,
    EnumHandler.class,
    EnumTemplateType.class,
    ErrorHash.class,
    FileCreationRequest.class,
    FileList.class,
    FileUpdatableFields.class,
    Layout.class,
    ListPaginationMetadata.class,
    ModelFile.class,
    Page.class,
    Partial.class,
    ProviderAccount.class,
    Section.class,
    SectionCreationRequest.class,
    SectionList.class,
    SectionUpdatableFields.class,
    Template.class,
    TemplateCreationRequest.class,
    TemplateList.class,
    TemplateUpdatableFields.class,
    WrappedProviderAccount.class
})
public class ReflectionConfiguration {
}

