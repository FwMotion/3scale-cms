package com.fwmotion.threescale.cms.mixins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.redhat.threescale.rest.cms.model.*;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * @see com.redhat.threescale.rest.cms.model.TemplateList
 */
@JsonDeserialize(as = TemplateMergingList.class)
public interface TemplateListMixIn {

    String ELEMENT_NAME_BUILTIN_PAGE = "builtin_page";
    String ELEMENT_NAME_BUILTIN_PARTIAL = "builtin_partial";
    String ELEMENT_NAME_LAYOUT = "layout";
    String ELEMENT_NAME_PAGE = "page";
    String ELEMENT_NAME_PARTIAL = "partial";

    @JsonProperty(ELEMENT_NAME_BUILTIN_PAGE)
    @XmlElement(name = ELEMENT_NAME_BUILTIN_PAGE)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_BUILTIN_PAGE)
    List<BuiltinPage> getBuiltinPages();

    @JsonProperty(ELEMENT_NAME_BUILTIN_PARTIAL)
    @XmlElement(name = ELEMENT_NAME_BUILTIN_PARTIAL)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_BUILTIN_PARTIAL)
    List<BuiltinPartial> getBuiltinPartials();

    @JsonProperty(ELEMENT_NAME_LAYOUT)
    @XmlElement(name = ELEMENT_NAME_LAYOUT)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_LAYOUT)
    List<Layout> getLayouts();

    @JsonProperty(ELEMENT_NAME_PAGE)
    @XmlElement(name = ELEMENT_NAME_PAGE)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_PAGE)
    List<Page> getPages();

    @JsonProperty(ELEMENT_NAME_PARTIAL)
    @XmlElement(name = ELEMENT_NAME_PARTIAL)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_PARTIAL)
    List<Partial> getPartials();

}
