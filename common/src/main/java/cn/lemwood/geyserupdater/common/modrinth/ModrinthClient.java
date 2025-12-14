package cn.lemwood.geyserupdater.common.modrinth;

import cn.lemwood.geyserupdater.common.api.UpdateClient;
import cn.lemwood.geyserupdater.common.config.ConfigManager;
import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ModrinthClient implements UpdateClient {
    private static final String BASE_URL = "https://api.modrinth.com/v2"; 
    private final HttpClient httpClient;
    private final Gson gson;
    private final PlatformAdapter platform;
    private final ConfigManager config;

    public ModrinthClient(PlatformAdapter platform, ConfigManager config) {
        this.platform = platform;
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<UpdateVersion> getLatestVersion(String projectId) {
        String loader = platform.getModrinthLoader();
        String gameVersion = "1.21"; 
        
        try {
            String loadersParam = String.format("[\"%s\"]", loader);
            String gameVersionsParam = String.format("[\"%s\"]", gameVersion);
            
            String url = String.format("%s/project/%s/version?loaders=%s&game_versions=%s", 
                BASE_URL, projectId, 
                URLEncoder.encode(loadersParam, StandardCharsets.UTF_8),
                URLEncoder.encode(gameVersionsParam, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "lemwood/GeyserUpdater/1.0.0 (leemwood@example.com)")
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            platform.warn("Failed to fetch updates for " + projectId + ": " + response.statusCode());
                            return null;
                        }
                        
                        try {
                            JsonArray versions = gson.fromJson(response.body(), JsonArray.class);
                            for (JsonElement verElem : versions) {
                                JsonObject verObj = verElem.getAsJsonObject();
                                String versionType = verObj.get("version_type").getAsString();
                                
                                if (!config.isAllowAlpha() && "alpha".equalsIgnoreCase(versionType)) continue;
                                if (!config.isAllowBeta() && "beta".equalsIgnoreCase(versionType)) continue;
                                
                                UpdateVersion version = new UpdateVersion();
                                version.versionNumber = verObj.get("version_number").getAsString();
                                
                                JsonArray files = verObj.getAsJsonArray("files");
                                if (files.size() > 0) {
                                    JsonObject fileObj = files.get(0).getAsJsonObject();
                                    for (JsonElement f : files) {
                                         if (f.getAsJsonObject().has("primary") && f.getAsJsonObject().get("primary").getAsBoolean()) {
                                             fileObj = f.getAsJsonObject();
                                             break;
                                         }
                                    }
                                    version.downloadUrl = fileObj.get("url").getAsString();
                                    version.filename = fileObj.get("filename").getAsString();
                                    
                                    if (fileObj.has("hashes")) {
                                        JsonObject hashes = fileObj.getAsJsonObject("hashes");
                                        if (hashes.has("sha256")) {
                                            version.sha256 = hashes.get("sha256").getAsString();
                                        }
                                    }
                                    
                                    return version;
                                }
                            }
                        } catch (Exception e) {
                            platform.error("Error parsing Modrinth response", e);
                        }
                        return null;
                    });
        } catch (Exception e) {
            CompletableFuture<UpdateVersion> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }
}
