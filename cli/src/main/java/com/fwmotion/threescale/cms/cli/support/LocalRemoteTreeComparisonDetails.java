package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.CmsObject;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class LocalRemoteTreeComparisonDetails {

    private Set<String> implicitSectionPaths;

    private Map<String, Pair<CmsObject, File>> localObjectsByCmsPath;
    private Map<String, CmsObject> remoteObjectsByCmsPath;

    private Set<String> localPathsIgnored;

    private Set<String> remotePathsMissingInLocal;
    private Set<String> localPathsMissingInRemote;

    private Set<String> remoteObjectsNewerThanLocal;
    private Set<String> localObjectsNewerThanRemote;

    public Set<String> getImplicitSectionPaths() {
        return implicitSectionPaths;
    }

    public void setImplicitSectionPaths(Set<String> implicitSectionPaths) {
        this.implicitSectionPaths = implicitSectionPaths;
    }

    public Map<String, Pair<CmsObject, File>> getLocalObjectsByCmsPath() {
        return localObjectsByCmsPath;
    }

    public void setLocalObjectsByCmsPath(Map<String, Pair<CmsObject, File>> localObjectsByCmsPath) {
        this.localObjectsByCmsPath = localObjectsByCmsPath;
    }

    public Map<String, CmsObject> getRemoteObjectsByCmsPath() {
        return remoteObjectsByCmsPath;
    }

    public void setRemoteObjectsByCmsPath(Map<String, CmsObject> remoteObjectsByCmsPath) {
        this.remoteObjectsByCmsPath = remoteObjectsByCmsPath;
    }

    public Set<String> getLocalPathsIgnored() {
        return localPathsIgnored;
    }

    public void setLocalPathsIgnored(Set<String> localPathsIgnored) {
        this.localPathsIgnored = localPathsIgnored;
    }

    public Set<String> getRemotePathsMissingInLocal() {
        return remotePathsMissingInLocal;
    }

    public void setRemotePathsMissingInLocal(Set<String> remotePathsMissingInLocal) {
        this.remotePathsMissingInLocal = remotePathsMissingInLocal;
    }

    public Set<String> getLocalPathsMissingInRemote() {
        return localPathsMissingInRemote;
    }

    public void setLocalPathsMissingInRemote(Set<String> localPathsMissingInRemote) {
        this.localPathsMissingInRemote = localPathsMissingInRemote;
    }

    public Set<String> getRemoteObjectsNewerThanLocal() {
        return remoteObjectsNewerThanLocal;
    }

    public void setRemoteObjectsNewerThanLocal(Set<String> remoteObjectsNewerThanLocal) {
        this.remoteObjectsNewerThanLocal = remoteObjectsNewerThanLocal;
    }

    public Set<String> getLocalObjectsNewerThanRemote() {
        return localObjectsNewerThanRemote;
    }

    public void setLocalObjectsNewerThanRemote(Set<String> localObjectsNewerThanRemote) {
        this.localObjectsNewerThanRemote = localObjectsNewerThanRemote;
    }
}
