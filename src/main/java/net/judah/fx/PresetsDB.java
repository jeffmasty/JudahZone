package net.judah.fx;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import net.judah.JudahZone;
import net.judah.mixer.Channel;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class PresetsDB extends ArrayList<Preset> {
	public static final String DEFAULT = "Freeverb";
	private Preset standard;
	
	
    public PresetsDB() {
        this(Folders.getPresetsFile());
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

    public void save() {
    	Constants.writeToFile(Folders.getPresetsFile(), toString());
    }

    @Override public String toString() {
        StringBuffer result = new StringBuffer();
        for (Preset p : this)
            result.append(p.toFile());
        return result.toString();
    }

	public Preset getDefault() {
		if (standard == null) {
			standard = byName(DEFAULT);
			if (standard == null) 
				standard = JudahZone.getMains().toPreset(DEFAULT);
		}
		return standard;
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

	public Preset[] array() {
		return toArray(new Preset[size()]);
	}

	public void replace(Channel channel) {
		int idx = indexOf(channel.getPreset());
		if (idx < 0) {
			RTLogger.warn(this, "Unknown Preset on " + channel.getName() 
				+ ": " + channel.getPreset() == null ? "null" : channel.getPreset().getName());
			return;
		}
		Preset p = channel.toPreset(channel.getPreset().getName());
        set(idx, p);
        save();
        RTLogger.log(this, "saved " + p.getName() + " from " + channel.getName() +
                " with " + p.size() + " FX");
	}
    
}

