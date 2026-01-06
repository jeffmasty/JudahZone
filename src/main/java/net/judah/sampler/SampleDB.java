package net.judah.sampler;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import judahzone.api.Asset;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.Recording;

/**
 * SampleDB: generic sample cache using normalized Path keys.
 */
public final class SampleDB {
	// TODO normalization
    private static final ConcurrentHashMap<Path, Recording> db = new ConcurrentHashMap<>();

    /* DEFAULT LOOP SAMPLES */
    static final String[] LOOPS = {"Creek", "Rain", "Birds", "Bicycle"};
    /* DEFAULT ONE-SHOT SAMPLES */
    static final String[] ONESHOTS = {"Satoshi", "Prrrrrr", "DropBass", "DJOutlaw"};
    /* STEP SAMPLES used by Sampler */
    static final String[] STEPS = {
        "Crickets","Block","Cowbell","Clap","Claves","Ride","Tambo","Shaker",
        "Disco","Hats16","Snares","4x4"
    };

    private static volatile boolean initialized = false;
    public static boolean isInitialized() { return initialized; }

    // Initialize standard samples (sampler + step samples)
    public static void init() {
        if (initialized) return;

        String[] standard = Stream.concat(java.util.Arrays.stream(LOOPS), java.util.Arrays.stream(ONESHOTS))
                .toArray(String[]::new);

        // register sampler assets (may attempt to warm load)
        for (String name : standard) {
            File file = new File(Folders.getSamples(), name + ".wav");
            loadAndRegister(name, file, Asset.Category.SAMPLER);
        }

        // register step-sample assets (no eager load required; keep recording null to save memory)
        for (String name : STEPS) {
            File file = new File(Folders.getSamples(), name + ".wav");
            loadAndRegister(name, file, Asset.Category.STEPSAMPLE);
        }
        initialized = true;

    }

    private static void loadAndRegister(String name, File file, Asset.Category cat) {
        try {
            Recording loaded = Recording.loadInternal(file);
            registerAsset(new Asset(name, file, loaded, loaded.size() * Constants.bufSize(), cat));
        } catch (Throwable t) {
            // conservative: register asset even if load failed so UI can show it
            registerAsset(new Asset(name, file, null, 0L, cat));
            RTLogger.warn(SampleDB.class, "Warm load failed: " + name + " : " + t.getMessage());
        }
    }

    private static final ConcurrentHashMap<Asset.Category, CopyOnWriteArrayList<Asset>> index = new ConcurrentHashMap<>();
    private static final ExecutorService bg = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SampleDB-loader");
        t.setDaemon(true);
        return t;
    });

    private SampleDB() {}

    public static Path keyOf(File f) {
        try {
            // Prefer real path to resolve symlinks/canonical locations so keys remain stable.
            return f.toPath().toRealPath().normalize();
        } catch (java.io.IOException e) {
            // Fall back to absolute+normalize if realPath fails (e.g., file not yet present)
            RTLogger.warn(SampleDB.class, "keyOf: toRealPath failed for " + f + " : " + e.getMessage());
            return f.toPath().toAbsolutePath().normalize();
        }
    }

    // Return the Asset registered for a category + simple name (case-insensitive match).
    public static Asset get(Asset.Category cat, String name) {
        var list = index.get(cat);
        if (list == null) return null;
        for (Asset a : list) {
            if (a.name().equalsIgnoreCase(name)) return a;
        }
        return null;
    }

    // Fast lookup. If not present, loads synchronously (caller must avoid calling from audio thread).
    public static Recording get(File file) throws Exception {
        Path key = keyOf(file);
        Recording r = db.get(key);
        if (r == null) {
            RTLogger.warn(SampleDB.class, "Lazy load sample: " + key);
            Recording loaded = Recording.loadInternal(file);
            Recording prev = db.putIfAbsent(key, loaded);
            return prev == null ? loaded : prev;
        }
        return r;
    }

    // Non-blocking: start background load and invoke callback when ready.
    public static void loadAsync(File file, java.util.function.Consumer<Recording> cb) {
        Path key = keyOf(file);
        if (db.containsKey(key)) {
            cb.accept(db.get(key));
            return;
        }
        bg.submit(() -> {
            try {
                Recording loaded = Recording.loadInternal(file);
                Recording prev = db.putIfAbsent(key, loaded);
                cb.accept(prev == null ? loaded : prev);
            } catch (Throwable t) {
                RTLogger.warn(SampleDB.class, "Async load failed: " + t.getMessage());
                cb.accept(null);
            }
        });
    }

    public static void registerAsset(Asset asset) {
        Path key = keyOf(asset.file());
        if (asset.recording() != null) {
            db.putIfAbsent(key, asset.recording());
        } else {
            // Log explicitly so warm-load failures are visible and don't silently cause lazy loads later.
            RTLogger.warn(SampleDB.class, "registerAsset: asset has no recording (will lazy-load): " + asset.file());
        }
        index.computeIfAbsent(asset.category(), k -> new CopyOnWriteArrayList<>()).add(asset);
    }

    // List assets by category for UI (cheap, snapshot-safe)
    public static List<Asset> listByCategory(Asset.Category cat) {
        var list = index.get(cat);
        return list == null ? List.of() : List.copyOf(list);
    }

    // Optional: clear for tests / reloads
    public static void clear() {
        db.clear();
        index.clear();
        initialized = false;
    }
}
