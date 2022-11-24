package net.judah.effects.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import net.judah.util.RTLogger;

public class PresetsDB extends ArrayList<Preset> {

    public enum Raw {
        StdCrunch, Chorus2, DrumsVerb, Tremelo, Freeverb
    }

	public static final File FILE = new File(System.getProperty("user.dir"), "presets.zone");

    public PresetsDB() {
        this(FILE);
    }

    public PresetsDB(File presetFile) {
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
                    if (!current.isEmpty()) add(new Preset(name, current));
                    current.clear();
                    name = line.substring(1, line.length() -1);
                }
                else
                    current.add(line);
            }
            scanner.close();
            if (!current.isEmpty()) add(new Preset(name, current));

        } catch (Throwable e) {
            RTLogger.warn(this, presetFile.getName() + " " + e.getMessage());
        }

    }
    
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (Preset p : this)
            result.append(p.toFile());
        return result.toString();
    }

    public Preset get(Raw raw) {
        for (Preset p : this)
            if (p.getName().equals(raw.name()))
                return p;
        return null;
    }

	public Preset getFirst() {
		if (isEmpty()) 
			return null;
		return get(0);
	}
	
	public ArrayList<String> getList() {
		ArrayList<String> result = new ArrayList<>();
		for (Preset p : this)
			result.add(p.getName());
		return result;
	}

	public Preset byName(String string) {
		for (Preset p : this)
			if (p.getName().equals(string))
				return p;
		return null;
	}

    
}

