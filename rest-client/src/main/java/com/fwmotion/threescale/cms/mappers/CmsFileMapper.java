package com.fwmotion.threescale.cms.mappers;

import com.fwmotion.threescale.cms.model.CmsFile;
import com.redhat.threescale.rest.cms.model.ModelFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Mapper
public interface CmsFileMapper {

    @Mapping(target = "tags", source = "tagList")
    CmsFile fromRest(ModelFile file);

    @Mapping(target = "tagList", source = "tags")
    // title is only the filename without path... not useful
    @Mapping(target = "title", ignore = true)
    // url is the S3 or local filesystem location for 3scale... not useful
    @Mapping(target = "url", ignore = true)
    // attachment (aka, file content) is not held in CmsFile
    @Mapping(target = "attachment", ignore = true)
    ModelFile toRest(CmsFile file);

    default Set<String> tagsFromRest(String tagList) {
        return new HashSet<>(Arrays.asList(tagList.split("\\s*,\\s*")));
    }

    default String tagsToRest(Set<String> tags) {
        return String.join(",", tags);
    }

}
