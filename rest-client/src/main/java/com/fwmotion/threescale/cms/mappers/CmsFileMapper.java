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
    CmsFile mapFromRest(ModelFile file);

    @Mapping(target = "tagList", source = "tags")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "url", ignore = true)
    // TODO: Map attachment
    @Mapping(target = "attachment", ignore = true)
    ModelFile mapToRest(CmsFile file);

    default Set<String> mapTagsFromRest(String tagList) {
        return new HashSet<>(Arrays.asList(tagList.split("\\s*,\\s*")));
    }

    default String mapTagsToRest(Set<String> tags) {
        return String.join(",", tags);
    }

}
