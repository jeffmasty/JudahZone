package net.judah.effects.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import net.judah.util.Console;

public class PresetsHandler extends ArrayList<Preset> {

    public PresetsHandler() {

        this(new File(System.getProperty("user.dir"), "presets.zone"));
    }

    public PresetsHandler(File presetFile) {
        if (presetFile == null || !presetFile.isFile()) {
            Console.info("no preset file " + presetFile);
            return;
        }
        ArrayList<String> current = new ArrayList<>();
        String name = null;
        try {
            Scanner scanner = new Scanner(presetFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Console.info(line);
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

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        if (isEmpty()) Console.info("No Presets loaded.");
        for (Preset p : this) {
            Console.info(p.toString());
        }

    }

    public void applyPreset(String name, List<Effect> channel) {
        for (Preset p : this)
            if (p.getName().equals(name))
                p.applyPreset(channel);
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (Preset p : this)
            result.append(p);
        return result.toString();
    }

}
