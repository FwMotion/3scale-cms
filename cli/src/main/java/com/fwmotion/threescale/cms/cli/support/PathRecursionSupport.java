package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.CmsFile;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.fwmotion.threescale.cms.model.CmsSection;
import com.fwmotion.threescale.cms.model.ThreescaleObjectType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class PathRecursionSupport {

    private static final Map<Class<? extends CmsObject>, Function<? super CmsObject, Integer>> GET_PARENT_ID_FUNCTIONS =
        Map.of(
            // TODO: See if there's a way to get section ID / parent ID from
            //       other object types
            CmsFile.class, file -> ((CmsFile) file).getSectionId(),
            CmsSection.class, section -> ((CmsSection) section).getParentId()
        );

    private static void addChildObjectsToList(LinkedList<Pair<String, CmsObject>> recursingList, Map<String, CmsObject> objectsByPath) {
        ListIterator<Pair<String, CmsObject>> treeWalker = recursingList.listIterator();

        while (treeWalker.hasNext()) {
            Pair<String, CmsObject> currentPair = treeWalker.next();
            CmsObject currentObject = currentPair.getRight();

            if (currentObject.getType() != ThreescaleObjectType.SECTION) {
                continue;
            }

            Integer parentId = currentObject.getId();

            int addedChildren = Math.toIntExact(
                objectsByPath.entrySet()
                    .stream()
                    .filter(childEntry -> {
                        CmsObject childObject = childEntry.getValue();
                        Integer childParentId = GET_PARENT_ID_FUNCTIONS.getOrDefault(childObject.getClass(), o -> Integer.MIN_VALUE)
                            .apply(childObject);

                        return parentId.equals(childParentId);
                    })
                    .peek(e -> treeWalker.add(Pair.of(e)))
                    .count());

            for (int i = 0; i < addedChildren; i++) {
                treeWalker.previous();
            }
        }
    }

    private static void validatePaths(Collection<String> specifiedPaths, Map<String, CmsObject> objectsByPath) {
        Set<String> nonMatchingPaths = specifiedPaths.stream()
            .filter(Predicate.not(objectsByPath::containsKey))
            .collect(Collectors.toSet());

        if (!nonMatchingPaths.isEmpty()) {
            throw new IllegalArgumentException("Paths do not exist: " + String.join(", ", nonMatchingPaths));
        }
    }

    public Set<String> calculateSpecifiedPaths(@Nonnull Collection<String> specifiedPaths,
                                               @Nonnull RecursionOption recurseBy,
                                               @Nonnull Map<String, CmsObject> objectsByPath) {

        validatePaths(specifiedPaths, objectsByPath);

        switch (recurseBy) {
            case NONE:
                return new HashSet<>(specifiedPaths);

            case PATH_PREFIX:
                return specifiedPaths.stream()
                    .flatMap(pathKey -> {
                        if (objectsByPath.get(pathKey).getType() == ThreescaleObjectType.SECTION) {
                            return objectsByPath.keySet().stream()
                                .filter(subKey -> StringUtils.startsWith(subKey, pathKey));
                        }

                        return Stream.of(pathKey);
                    })
                    .collect(Collectors.toSet());

            case PARENT_ID:
                return specifiedPaths.stream()
                    .flatMap(pathKey -> {
                        CmsObject parentObject = objectsByPath.get(pathKey);

                        LinkedList<Pair<String, CmsObject>> recursingList = new LinkedList<>();
                        recursingList.add(Pair.of(pathKey, parentObject));

                        addChildObjectsToList(recursingList, objectsByPath);

                        return recursingList.stream()
                            .map(Pair::getKey);
                    })
                    .collect(Collectors.toSet());

            default:
                throw new UnsupportedOperationException("Unknown recursion style: " + recurseBy);
        }
    }

    public enum RecursionOption {
        PARENT_ID,
        PATH_PREFIX,
        NONE
    }
}
