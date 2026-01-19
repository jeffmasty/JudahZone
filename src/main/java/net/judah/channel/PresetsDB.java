package net.judah.channel;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.Getter;

public class PresetsDB {
	public static final String DEFAULT = "Freebird";
	private static Preset standard;

	private static ArrayList<Preset> db = new ArrayList<>();
	@Getter private static boolean initialized = false;
	private static final PresetsDB instance = new PresetsDB();

	// public static final Comparator<Preset> Alphabetical = (o1, o2) -> o1.name.compareTo(o2.name);
	// public static final Comparator<Preset> ByDate = (o1, o2) -> Integer.compare(o1.index, o2.index);

    private PresetsDB() {
    	if (!initialized)
    		init(Folders.getPresetsFile());
    }

    public static void init(File presetFile) {
    	initialized = false;
    	db.clear();
        if (presetFile == null || !presetFile.isFile()) {
            System.err.println("no preset file " + presetFile);
            return;
        }
        ArrayList<String> current = new ArrayList<>();
        String name = null;
        try {
            Scanner scanner = new Scanner(presetFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("[") && line.endsWith("]")) {
                    if (!current.isEmpty()) db.add(new Preset(name, current));
                    current.clear();
                    name = line.substring(1, line.length() -1);
                }
                else
                    current.add(line);
            }
            scanner.close();
            if (!current.isEmpty()) db.add(new Preset(name, current));

        } catch (Throwable e) {
            RTLogger.warn(instance, presetFile.getName() + " " + e.getMessage());
        }
        initialized = true;
    }

    public static void save() {
    	Threads.writeToFile(Folders.getPresetsFile(), PresetsDB.print());
    }

    public static String print() {
        StringBuffer result = new StringBuffer();
        for (Preset p : db)
            result.append(p.toFile());
        return result.toString();
    }

	public static Preset getDefault() {
		if (standard == null) {
			standard = byName(DEFAULT);
			if (standard == null)
				standard = new Preset("null", new ArrayList<String>());
		}
		return standard;
	}

	public static ArrayList<String> getList() {
		ArrayList<String> result = new ArrayList<>();
		for (Preset p : db)
			result.add(p.getName());
		return result;
	}

	public static Preset byName(String string) {
		for (Preset p : db)
			if (p.getName().equals(string))
				return p;
		return null;
	}

	public static String[] getPatches() {
		String[] result = new String[db.size()];
		for (int i = 0; i < db.size(); i++)
			result[i] = db.get(i).getName();
		return result;
	}


	public static Preset[] array() {
		return db.toArray(new Preset[db.size()]);
	}

	public static void replace(Channel channel) {
		int idx = db.indexOf(channel.getPreset());
		if (idx < 0) {
			RTLogger.warn(instance, "Unknown Preset on " + channel.getName()
				+ ": " + channel.getPreset() == null ? "null" : channel.getPreset().getName());
			return;
		}
		Preset p = channel.toPreset(channel.getPreset().getName());
        db.set(idx, p);
        save();
        feedback("update", p, channel);
	}



	public static void add(Channel ch, String name) {
		Preset p = ch.toPreset(name);

		for (Preset previous : db)
			if (previous.getName().equals(name)) {
				// replace
				db.set(db.indexOf(previous), p);
				save();
				return;
			}

		db.add(p);
        // sort(Alphabetical);
        save();
        ch.getGui().getPresets().refill(array(), ch.getPreset());
        feedback("new", p, ch);
	}

	private static void feedback(String mode, Preset p, Channel ch) {
        RTLogger.debug(instance, mode + " " + p.getName() + " from " + ch.getName() +
                " with " + p.size() + " FX");
	}

	public static int size() {
		return db.size();
	}

	public static int indexOf(Object selectedItem) {
		if (selectedItem instanceof Preset) {
			return db.indexOf(selectedItem);
		}
		return -1;
	}

	public static Preset get(int next) {
		return db.get(next);
	}


	public static void add(Preset preset) {
		db.add(preset);
	}

	public static void set(int idx, Preset replace) {
		db.set(idx, replace);
	}

	public static ArrayList<Preset> getDb() {
		return db;
	}

	public static void remove(int idx) {
		db.remove(idx);
	}


}