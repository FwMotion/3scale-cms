package com.fwmotion.threescale.cms.mixins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.redhat.threescale.rest.cms.model.BuiltinSection;
import com.redhat.threescale.rest.cms.model.Section;
import com.redhat.threescale.rest.cms.model.SectionList;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;

/**
 * This is used for deserialization to facilitate interlaced unwrapped lists.
 * Per
 * <a href="https://github.com/FasterXML/jackson-dataformat-xml/issues/275">jackson-dataformat-xml Issue #275</a>,
 * interlaced unwrapped lists is not supported, so this class will be used
 * as a workaround to merge the lists as they're "set" into the object.
 *
 * @see SectionListMixIn
 * @see SectionList
 */
public class SectionMergingList extends SectionList {

    @JsonProperty(SectionListMixIn.ELEMENT_NAME_BUILTIN_SECTION)
    @XmlElement(name = SectionListMixIn.ELEMENT_NAME_BUILTIN_SECTION)
    @JacksonXmlElementWrapper(useWrapping = false, localName = SectionListMixIn.ELEMENT_NAME_BUILTIN_SECTION)
    @Override
    public void setBuiltinSections(List<BuiltinSection> builtinSections) {
        builtinSections.forEach(super::addBuiltinSectionsItem);
    }

    @JsonProperty(SectionListMixIn.ELEMENT_NAME_BUILTIN_SECTION)
    @XmlElement(name = SectionListMixIn.ELEMENT_NAME_BUILTIN_SECTION)
    @JacksonXmlElementWrapper(useWrapping = false, localName = SectionListMixIn.ELEMENT_NAME_BUILTIN_SECTION)
    @Override
    public void setSections(List<Section> sections) {
        sections.forEach(super::addSectionsItem);
    }

}
