package permagears.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public final class AdvancementConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("PermaGears/AdvancementConfig");
    private static final String NAME = "permagears_advancements.json";

    public static final class Data {
        public Set<String> sigil_advancements = new HashSet<>();
        public int reload_interval_ticks = 100;
    }

    private static Path file;
    private static Data data = new Data();
    private static long lastMTime = 0L;
    private static int reloadTicker = 0;

    private AdvancementConfigManager() {}

    public static void init(Path configDir) {
        try {
            Files.createDirectories(configDir);
            file = configDir.resolve(NAME);
            if (!Files.exists(file)) saveDefaults();
            load();
        } catch (Throwable t) {
            LOGGER.error("Advancement config init failed; using empty.", t);
            data = new Data();
        }
    }

    public static boolean isEnabled() {
        return data != null && !data.sigil_advancements.isEmpty();
    }

    public static boolean isSigilAdvancement(String id) {
        return data != null && data.sigil_advancements.contains(id);
    }

    public static void maybeReload() {
        if (file == null) return;
        try {
            reloadTicker++;
            int every = Math.max(1, data.reload_interval_ticks);
            if (reloadTicker % every != 0) return;

            long m = Files.getLastModifiedTime(file).toMillis();
            if (m != lastMTime) {
                load();
                LOGGER.info("PermaGears advancement config reloaded.");
            }
        } catch (Throwable t) {
            LOGGER.error("Advancement config reload failed; keeping previous.", t);
        }
    }

    private static void load() {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();

            Data nd = new Data();

            if (obj.has("sigil_advancements") && obj.get("sigil_advancements").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("sigil_advancements");
                for (JsonElement el : arr) {
                    if (el.isJsonPrimitive()) {
                        nd.sigil_advancements.add(el.getAsString());
                    }
                }
            }

            if (obj.has("reload_interval_ticks")) {
                nd.reload_interval_ticks = Math.max(1, obj.get("reload_interval_ticks").getAsInt());
            }

            data = nd;
            lastMTime = Files.getLastModifiedTime(file).toMillis();
        } catch (Throwable t) {
            LOGGER.error("Failed to load advancement config; using empty.", t);
            data = new Data();
        }
    }

    private static void saveDefaults() {
        JsonObject obj = new JsonObject();
        obj.add("sigil_advancements", new JsonArray()); // empty by default (safe)
        obj.addProperty("reload_interval_ticks", 100);

        try (Writer w = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
        } catch (Throwable t) {
            LOGGER.error("Failed to write default advancement config", t);
        }
    }
}
