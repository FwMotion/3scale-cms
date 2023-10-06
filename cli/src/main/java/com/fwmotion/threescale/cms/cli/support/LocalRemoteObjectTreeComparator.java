package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.CmsLayout;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.fwmotion.threescale.cms.model.CmsSection;
import com.fwmotion.threescale.cms.model.ThreescaleObjectType;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class LocalRemoteObjectTreeComparator {

    @Inject
    CmsObjectPathKeyGenerator cmsObjectPathKeyGenerator;

    @Inject
    LocalFileCmsObjectGenerator localFileCmsObjectGenerator;

    @Nonnull
    public LocalRemoteTreeComparisonDetails compareLocalAndRemoteCmsObjectTrees(
        @Nonnull Stream<CmsObject> remoteObjects,
        @Nonnull File localRoot,
        boolean defaultLayoutCanBeRemoteOnly) throws Exception {
        LocalRemoteTreeComparisonDetails details = new LocalRemoteTreeComparisonDetails();

        calculateRemoteObjects(details, remoteObjects);
        calculateLocalObjects(details, localRoot);
        calculateDiffs(details);
        calculateDefaultLayout(details, defaultLayoutCanBeRemoteOnly);

        return details;
    }

    private void calculateRemoteObjects(
        @Nonnull LocalRemoteTreeComparisonDetails details,
        @Nonnull Stream<CmsObject> remoteObjects) {

        Map<String, Boolean> implicitSectionsMap = new HashMap<>();

        details.setRemoteObjectsByCmsPath(
            remoteObjects
                .map(cmsObject -> Pair.of(cmsObjectPathKeyGenerator.generatePathKeyForObject(cmsObject), cmsObject))
                .peek(pair -> {
                    String path = pair.getLeft();
                    if (pair.getRight() instanceof CmsSection) {
                        implicitSectionsMap.put(path, false);
                        path = StringUtils.left(path, StringUtils.lastIndexOf(path, '/'));
                    }

                    while (StringUtils.countMatches(path, '/') > 1) {
                        path = StringUtils.left(path, StringUtils.lastIndexOf(path, '/'));
                        implicitSectionsMap.computeIfAbsent(path + "/", k -> true);
                    }
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));

        details.setImplicitSectionPaths(implicitSectionsMap.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet()));
    }

    private void calculateLocalObjects(
        @Nonnull LocalRemoteTreeComparisonDetails details,
        @Nonnull File localRoot
    ) throws IOException {

        Path rootPath = localRoot.toPath();
        Set<String> implicitSectionPaths = details.getImplicitSectionPaths();

        Set<String> localPathIgnored = new HashSet<>();
        Map<String, Pair<CmsObject, File>> localObjectsByCmsPath = new HashMap<>();

        Files.walkFileTree(
            rootPath,
            Set.of(FileVisitOption.FOLLOW_LINKS),
            Integer.MAX_VALUE,
            new LocalFileVisitor(localFileCmsObjectGenerator,
                cmsObjectPathKeyGenerator,
                rootPath,
                implicitSectionPaths,
                localPathIgnored::add,
                localObjectsByCmsPath::put));

        details.setLocalPathsIgnored(localPathIgnored);
        details.setLocalObjectsByCmsPath(localObjectsByCmsPath);
    }

    private void calculateDiffs(@Nonnull LocalRemoteTreeComparisonDetails details) {
        Set<String> remoteObjectsNewerThanLocal = new HashSet<>();
        Set<String> localObjectsNewerThanLocal = new HashSet<>();
        Set<String> remotePathsMissingInLocal = new HashSet<>();

        Map<String, Pair<CmsObject, File>> localObjects = new HashMap<>(details.getLocalObjectsByCmsPath());

        for (Map.Entry<String, CmsObject> remoteEntry : details.getRemoteObjectsByCmsPath().entrySet()) {
            String path = remoteEntry.getKey();
            Pair<CmsObject, File> localFilePair = localObjects.remove(path);

            if (localFilePair == null) {
                if (remoteEntry.getValue().getType() != ThreescaleObjectType.SECTION) {
                    remotePathsMissingInLocal.add(path);
                }
            } else {
                if (remoteEntry.getValue().getType() == localFilePair.getLeft().getType()
                    && remoteEntry.getValue().getType() == ThreescaleObjectType.SECTION) {
                    continue;
                }

                OffsetDateTime remoteUpdatedAt = remoteEntry.getValue().getUpdatedAt()
                    .truncatedTo(ChronoUnit.SECONDS);
                OffsetDateTime localUpdatedAt = localFilePair.getLeft().getUpdatedAt()
                    .truncatedTo(ChronoUnit.SECONDS);

                int comparisonResult = remoteUpdatedAt.compareTo(localUpdatedAt);

                if (comparisonResult < 0) {
                    localObjectsNewerThanLocal.add(path);
                } else if (comparisonResult > 0) {
                    remoteObjectsNewerThanLocal.add(path);
                }
            }
        }

        details.setRemoteObjectsNewerThanLocal(remoteObjectsNewerThanLocal);
        details.setLocalObjectsNewerThanRemote(localObjectsNewerThanLocal);
        details.setRemotePathsMissingInLocal(remotePathsMissingInLocal);
        details.setLocalPathsMissingInRemote(new HashSet<>(localObjects.keySet()));
    }

    private void calculateDefaultLayout(LocalRemoteTreeComparisonDetails details, boolean defaultLayoutCanBeRemoteOnly) {
        Stream<CmsObject> remoteObjectStream;
        if (defaultLayoutCanBeRemoteOnly) {
            remoteObjectStream = details.getRemoteObjectsByCmsPath()
                .values()
                .stream();
        } else {
            Set<String> remoteOnlyPaths = details.getRemotePathsMissingInLocal();
            remoteObjectStream = details.getRemoteObjectsByCmsPath()
                .entrySet()
                .stream()
                .filter(entry -> !remoteOnlyPaths.contains(entry.getKey()))
                .map(Map.Entry::getValue);
        }

        details.setDefaultLayout(
            Stream.concat(
                    remoteObjectStream,
                    details.getLocalObjectsByCmsPath()
                        .values()
                        .stream()
                        .map(Pair::getLeft)
                )
                .filter(o -> o instanceof CmsLayout)
                .map(o -> (CmsLayout) o)
                .findFirst()
                .orElse(null)
        );

    }

}
