package com.github.ksgfk.dawnfoundation.api.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.common.versioning.ComparableVersion;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 检查Github Release更新
 *
 * @author KSGFK create in 2019/11/2
 */
public class GithubReleaseUpdateCheck {
    private static GithubReleaseUpdateCheck instance = new GithubReleaseUpdateCheck();

    private ExecutorService pool;
    private ConcurrentHashMap<String, UpdateInfo> info = new ConcurrentHashMap<>();

    private GithubReleaseUpdateCheck() {
        pool = new ThreadPoolExecutor(2,
                2,
                6000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> new Thread(r, "UpdateCheck"));
    }

    public static GithubReleaseUpdateCheck getInstance() {
        return instance;
    }

    /**
     * 开始检查
     *
     * @param modId            调用开始检查的modId
     * @param githubReleaseURL URL
     * @param nowVersion       目前版本
     * @param preCanUpdate     发现更高版本的Pre-release是否作为更新
     * @param logger           Logger
     * @param callback         检查完成后的回调
     */
    public void startCheck(String modId, String githubReleaseURL, String nowVersion, boolean preCanUpdate, Logger logger, @Nullable Consumer<UpdateInfo> callback) {
        pool.execute(new Check(modId, githubReleaseURL, nowVersion, preCanUpdate, logger, callback));
    }

    /**
     * 获取更新信息
     *
     * @return 可能返回null
     */
    @Nullable
    public UpdateInfo getInfo(String modId) {
        return info.getOrDefault(modId, null);
    }

    public static class UpdateInfo {
        private boolean canUpdate;
        private String latest;
        private String latestPre;

        public boolean canUpdate() {
            return canUpdate;
        }

        public String getLatest() {
            return latest;
        }

        public String getLatestPre() {
            return latestPre;
        }
    }

    static class Check implements Runnable {
        private final Logger logger;
        private String modId;
        private String githubReleaseURL;
        private String nowVersion;
        private Consumer<UpdateInfo> callback;
        private boolean preCanUpdate;

        Check(String modId, String githubReleaseURL, String nowVersion, boolean preCanUpdate, Logger logger, Consumer<UpdateInfo> callback) {
            this.modId = modId;
            this.githubReleaseURL = githubReleaseURL;
            this.nowVersion = nowVersion;
            this.preCanUpdate = preCanUpdate;
            this.logger = logger;
            this.callback = callback;
        }

        private void getUpdateJson(String modId, String githubReleaseURL, String nowVersion) {
            StringBuilder result = new StringBuilder();
            try {
                URL apiUrl = new URL(githubReleaseURL);
                HttpsURLConnection connection = (HttpsURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.getResponseCode();
                InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader buffer = new BufferedReader(reader);
                String line;
                while ((line = buffer.readLine()) != null) {
                    result.append(line);
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                return;
            }
            UpdateInfo i = new UpdateInfo();
            JsonArray elements = new JsonParser().parse(result.toString()).getAsJsonArray();
            for (JsonElement e : elements) {
                JsonObject jsonObject = e.getAsJsonObject();
                String versionString = jsonObject.get("tag_name").getAsString();
                boolean prerelease = jsonObject.get("prerelease").getAsBoolean();
                ComparableVersion latest = new ComparableVersion(versionString);
                ComparableVersion now = new ComparableVersion(nowVersion);
                if (prerelease) {
                    i.latestPre = versionString;
                } else {
                    i.latest = versionString;
                }
                if (preCanUpdate && prerelease) {
                    if (latest.compareTo(now) > 0) {
                        i.canUpdate = true;
                    }
                } else if (!prerelease) {
                    if (latest.compareTo(now) > 0) {
                        i.canUpdate = true;
                    }
                }
                break;
            }
            String r = "Latest:" + i.latest + "\tPre:" + i.latestPre;
            if (i.canUpdate) {
                logger.warn("Find update! " + r);
            } else {
                logger.info(r);
            }
            instance.info.put(modId, i);
            if (callback != null) {
                callback.accept(i);
            }
        }

        @Override
        public void run() {
            getUpdateJson(modId, githubReleaseURL, nowVersion);
        }
    }
}
