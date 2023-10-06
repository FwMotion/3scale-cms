package com.fwmotion.threescale.cms.mixins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.redhat.threescale.rest.cms.model.Section;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;

/**
 * @see com.redhat.threescale.rest.cms.model.SectionList
 */
public interface SectionListMixIn {

    String ELEMENT_NAME_SECTION = "section";

    @JsonProperty(ELEMENT_NAME_SECTION)
    @XmlElement(name = ELEMENT_NAME_SECTION)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_SECTION)
    List<Section> getSections();

    @JsonProperty(ELEMENT_NAME_SECTION)
    @XmlElement(name = ELEMENT_NAME_SECTION)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_SECTION)
    void setSections(List<Section> sections);

}
