package com.fwmotion.threescale.cms.mixins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.redhat.threescale.rest.cms.model.*;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;

/**
 * This is used for deserialization to facilitate interlaced unwrapped lists.
 * Per
 * <a href="https://github.com/FasterXML/jackson-dataformat-xml/issues/275">jackson-dataformat-xml Issue #275</a>,
 * interlaced unwrapped lists is not supported, so this class will be used
 * as a workaround to merge the lists as they're "set" into the object.
 *
 * @see TemplateListMixIn
 * @see TemplateList
 */
public class TemplateMergingList extends TemplateList {

    @JsonProperty(TemplateListMixIn.ELEMENT_NAME_BUILTIN_PAGE)
    @XmlElement(name = TemplateListMixIn.ELEMENT_NAME_BUILTIN_PAGE)
    @JacksonXmlElementWrapper(useWrapping = false, localName = TemplateListMixIn.ELEMENT_NAME_BUILTIN_PAGE)
    @Override
    public void setBuiltinPages(List<BuiltinPage> builtinPages) {
        builtinPages.forEach(super::addBuiltinPagesItem);
    }

    @JsonProperty(TemplateListMixIn.ELEMENT_NAME_BUILTIN_PARTIAL)
    @XmlElement(name = TemplateListMixIn.ELEMENT_NAME_BUILTIN_PARTIAL)
    @JacksonXmlElementWrapper(useWrapping = false, localName = TemplateListMixIn.ELEMENT_NAME_BUILTIN_PARTIAL)
    @Override
    public void setBuiltinPartials(List<BuiltinPartial> builtinPartials) {
        builtinPartials.forEach(super::addBuiltinPartialsItem);
    }

    @JsonProperty(TemplateListMixIn.ELEMENT_NAME_LAYOUT)
    @XmlElement(name = TemplateListMixIn.ELEMENT_NAME_LAYOUT)
    @JacksonXmlElementWrapper(useWrapping = false, localName = TemplateListMixIn.ELEMENT_NAME_LAYOUT)
    @Override
    public void setLayouts(List<Layout> layouts) {
        layouts.forEach(super::addLayoutsItem);
    }

    @JsonProperty(TemplateListMixIn.ELEMENT_NAME_PAGE)
    @XmlElement(name = TemplateListMixIn.ELEMENT_NAME_PAGE)
    @JacksonXmlElementWrapper(useWrapping = false, localName = TemplateListMixIn.ELEMENT_NAME_PAGE)
    @Override
    public void setPages(List<Page> pages) {
        pages.forEach(super::addPagesItem);
    }

    @JsonProperty(TemplateListMixIn.ELEMENT_NAME_PARTIAL)
    @XmlElement(name = TemplateListMixIn.ELEMENT_NAME_PARTIAL)
    @JacksonXmlElementWrapper(useWrapping = false, localName = TemplateListMixIn.ELEMENT_NAME_PARTIAL)
    @Override
    public void setPartials(List<Partial> partials) {
        partials.forEach(super::addPartialsItem);
    }
}
