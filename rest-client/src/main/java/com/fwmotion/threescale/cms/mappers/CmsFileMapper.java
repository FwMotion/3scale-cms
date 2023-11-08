package com.fwmotion.threescale.cms.mappers;

import com.fwmotion.threescale.cms.model.CmsFile;
import com.redhat.threescale.rest.cms.model.ModelFile;
import jakarta.annotation.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper
public interface CmsFileMapper {

    CmsFile fromRest(ModelFile file);

    // title is only the filename without path... not useful
    @Mapping(target = "title", ignore = true)
    // url is the S3 or local filesystem location for 3scale... not useful
    @Mapping(target = "url", ignore = true)
    // attachment (aka, file content) is not held in CmsFile
    @Mapping(target = "attachment", ignore = true)
    ModelFile toRest(CmsFile file);

    default Set<String> tagsFromRest(@Nullable String tagList) {
        if (tagList == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(tagList.split("\\s*,\\s*")));
    }

    default String tagsToRest(@Nullable Set<String> tags) {
        if (tags == null) {
            return "";
        }

        // Sort tags so they're in consistent order
        return tags.stream()
            .sorted()
            .collect(Collectors.joining(","));
    }

}
