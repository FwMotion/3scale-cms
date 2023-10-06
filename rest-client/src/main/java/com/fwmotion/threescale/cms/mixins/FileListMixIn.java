package com.fwmotion.threescale.cms.mixins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.redhat.threescale.rest.cms.model.ModelFile;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;

/**
 * @see com.redhat.threescale.rest.cms.model.FileList
 */
public interface FileListMixIn {

    String ELEMENT_NAME_FILE = "file";

    @JsonProperty(ELEMENT_NAME_FILE)
    @XmlElement(name = ELEMENT_NAME_FILE)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_FILE)
    List<ModelFile> getFiles();

    @JsonProperty(ELEMENT_NAME_FILE)
    @XmlElement(name = ELEMENT_NAME_FILE)
    @JacksonXmlElementWrapper(useWrapping = false, localName = ELEMENT_NAME_FILE)
    void setFiles(List<ModelFile> sections);

}
