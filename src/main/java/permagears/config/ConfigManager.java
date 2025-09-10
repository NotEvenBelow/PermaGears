package permagears.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("PermaGears/Config");

    public static class Data {
        public Set<String> unbreakable_tools = new HashSet<>();
        public int scan_interval_ticks = 40;
        public int reload_interval_ticks = 100;
        public boolean disable_soulbound = false;
        public boolean disable_drop_lock = false;

        public boolean give_sigil_on_first_join = false; 
    }

    private static final String NAME = "permagears.json";
    private static final String DEFAULT_SIGIL_ID = "permagears:fractured_sigil";

    private static Path file;
    private static Data data = new Data();
    private static long lastMTime = 0L;
    private static int reloadTicker = 0;

    public static void init(Path configDir) {
        try {
            Files.createDirectories(configDir);
            file = configDir.resolve(NAME);
            if (!Files.exists(file)) saveDefaults();
            load();
            ensureDefaultSigilPresent();
        } catch (IOException e) {
            LOGGER.error("Config init failed", e);
        }
    }

    public static void ensureLoadedDefault() {
        if (file == null) {
            init(FabricLoader.getInstance().getConfigDir());
        }
    }

    public static boolean isWhitelisted(String itemId) {
        return data.unbreakable_tools.contains(itemId);
    }

    public static boolean disableSoulbound() { return data.disable_soulbound; }

    public static boolean disableDropLock() { return data.disable_drop_lock; }

    // NEW: expose the toggle
    public static boolean giveSigilOnFirstJoin() { return data.give_sigil_on_first_join; }

    public static int getScanInterval() { return Math.max(1, data.scan_interval_ticks); }

    public static void maybeReload() {
        reloadTicker++;
        if (reloadTicker % Math.max(1, data.reload_interval_ticks) != 0) return;
        try {
            long m = Files.getLastModifiedTime(file).toMillis();
            if (m != lastMTime) {
                load();
                LOGGER.info("PermaGears tool config reloaded.");
            }
        } catch (IOException ignored) {}
    }

    private static void load() {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            Data nd = new Data();

            if (obj.has("unbreakable_tools") && obj.get("unbreakable_tools").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("unbreakable_tools")) {
                    nd.unbreakable_tools.add(el.getAsString());
                }
            }
            if (obj.has("scan_interval_ticks")) nd.scan_interval_ticks = obj.get("scan_interval_ticks").getAsInt();
            if (obj.has("reload_interval_ticks")) nd.reload_interval_ticks = obj.get("reload_interval_ticks").getAsInt();
            if (obj.has("disable_soulbound")) nd.disable_soulbound = obj.get("disable_soulbound").getAsBoolean();
            if (obj.has("disable_drop_lock")) nd.disable_drop_lock = obj.get("disable_drop_lock").getAsBoolean();

            if (obj.has("give_sigil_on_first_join"))
                nd.give_sigil_on_first_join = obj.get("give_sigil_on_first_join").getAsBoolean();

            data = nd;
            lastMTime = Files.getLastModifiedTime(file).toMillis();
        } catch (Exception e) {
            LOGGER.error("PermaGears failed to load tool config", e);
        }
    }

    private static void saveDefaults() {
        JsonObject obj = new JsonObject();
        JsonArray list = new JsonArray();
        list.add(DEFAULT_SIGIL_ID);
        obj.add("unbreakable_tools", list);
        obj.addProperty("scan_interval_ticks", 40); // Currently useless, maybe for later update 
        obj.addProperty("reload_interval_ticks", 100); // same as above
        obj.addProperty("disable_soulbound", false);
        obj.addProperty("disable_drop_lock", false);

        obj.addProperty("give_sigil_on_first_join", false);

        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
        } catch (IOException e) { LOGGER.error("Failed to write default tool config", e); }
    }

    private static void ensureDefaultSigilPresent() {
        if (!data.unbreakable_tools.contains(DEFAULT_SIGIL_ID)) {
            data.unbreakable_tools.add(DEFAULT_SIGIL_ID);
            saveCurrent();
            LOGGER.info("Added default '{}' to unbreakable_tools.", DEFAULT_SIGIL_ID);
        }
    }

    private static void saveCurrent() {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String id : data.unbreakable_tools) arr.add(id);
        obj.add("unbreakable_tools", arr);
        obj.addProperty("scan_interval_ticks", data.scan_interval_ticks);
        obj.addProperty("reload_interval_ticks", data.reload_interval_ticks);
        obj.addProperty("disable_soulbound", data.disable_soulbound);
        obj.addProperty("disable_drop_lock", data.disable_drop_lock);

        // NEW: persist the toggle
        obj.addProperty("give_sigil_on_first_join", data.give_sigil_on_first_join);

        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
            lastMTime = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) { LOGGER.error("Failed to write tool config", e); }
    }

    public static Data getData() { return data; }
}
