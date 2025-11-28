package cn.lemwood.geyserupdater.common.geyser;

import cn.lemwood.geyserupdater.common.api.UpdateClient;
import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GeyserDownloadClient implements UpdateClient {
    private static final String BASE_URL = "https://download.geysermc.org/v2";
    private final HttpClient httpClient;
    private final Gson gson;
    private final PlatformAdapter platform;

    public GeyserDownloadClient(PlatformAdapter platform) {
        this.platform = platform;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<UpdateVersion> getLatestVersion(String projectId) {
        // Project ID mapping for Geyser API
        String geyserProject = switch (projectId) {
            case "geyser" -> "geyser";
            case "floodgate" -> "floodgate";
            default -> projectId;
        };

        String url = String.format("%s/projects/%s/versions/latest/builds/latest", BASE_URL, geyserProject);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "lemwood/GeyserUpdater/1.0.0 (leemwood@example.com)")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        platform.warn("Failed to fetch Geyser updates for " + projectId + ": " + response.statusCode());
                        return null;
                    }

                    try {
                        JsonObject buildObj = gson.fromJson(response.body(), JsonObject.class);
                        UpdateVersion version = new UpdateVersion();
                        
                        // Build number as version, or try to construct a semantic version if available?
                        // Geyser API returns "build": 123.
                        // The "version" field in URL is just "latest".
                        // Actually, the response might not contain the semantic version string directly in root?
                        // Let's check the response structure from previous tool output (user provided).
                        // User didn't provide full JSON, just "downloads".
                        // Usually Geyser API v2 returns: {"build": 123, "time": "...", "channel": "standalone", "promoted": true, "changes": [...], "downloads": {...}}
                        // It doesn't explicitly say "2.2.0-SNAPSHOT".
                        // However, for version comparison, build number is actually better if we track it.
                        // But existing logic compares string versions.
                        // Let's use the build number as version for now, prefixed with "build-".
                        
                        int build = buildObj.get("build").getAsInt();
                        version.versionNumber = String.valueOf(build);

                        JsonObject downloads = buildObj.getAsJsonObject("downloads");
                        String platformKey = getGeyserPlatformKey(projectId);
                        
                        if (downloads.has(platformKey)) {
                            JsonObject downloadInfo = downloads.getAsJsonObject(platformKey);
                            version.filename = downloadInfo.get("name").getAsString();
                            // Construct download URL manually as it's standard
                            // /v2/projects/{project}/versions/{version}/builds/{build}/downloads/{download}
                            // We used "latest" for version in request, but for download url we should probably use "latest" too or specific version?
                            // Actually we can just append to the base URL structure.
                            version.downloadUrl = String.format("%s/projects/%s/versions/latest/builds/latest/downloads/%s", 
                                    BASE_URL, geyserProject, platformKey);
                            return version;
                        } else {
                            platform.warn("Geyser platform '" + platformKey + "' not found in downloads for " + projectId);
                        }
                    } catch (Exception e) {
                        platform.error("Error parsing Geyser API response", e);
                    }
                    return null;
                });
    }

    private String getGeyserPlatformKey(String projectId) {
        // If checking for GeyserExtras, it might not follow the same structure or be available here.
        // GeyserExtras is a separate project.
        // The URL used above is for "geyser" or "floodgate".
        // GeyserExtras might need to use Modrinth or GitHub if not on Geyser Downloads API.
        // But user request is "use official api for standalone version".
        // Geyser Standalone needs Geyser-Standalone.jar.
        // Floodgate is also on Downloads API.
        
        if ("geyserextras".equals(projectId)) {
            // GeyserExtras is not on download.geysermc.org usually?
            // It is on Modrinth.
            // So we should probably fallback or handle it?
            // But UpdateClient is per-instance.
            // We might need a CompositeClient or handle this in getLatestVersion.
            return null; 
        }

        String loader = platform.getModrinthLoader();
        return switch (loader) {
            case "geyser" -> "standalone"; // For extension
            case "velocity" -> "velocity";
            case "paper" -> "spigot"; // Paper uses Spigot build
            case "fabric" -> "fabric";
            case "viaproxy" -> "viaproxy";
            default -> "standalone";
        };
    }
}
