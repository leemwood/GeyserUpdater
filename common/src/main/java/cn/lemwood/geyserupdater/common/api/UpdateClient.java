package cn.lemwood.geyserupdater.common.api;

import java.util.concurrent.CompletableFuture;

public interface UpdateClient {
    CompletableFuture<UpdateVersion> getLatestVersion(String projectId);

    class UpdateVersion {
        public String versionNumber;
        public String downloadUrl;
        public String filename;
    }
}
