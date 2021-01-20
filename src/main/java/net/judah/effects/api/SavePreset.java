package net.judah.effects.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.judah.util.Constants;

public class SavePreset extends ArrayList<String> {

    public SavePreset(String presetName, List<Effect> channel) {
        add("[" + presetName + "]");
        for (Effect e : channel) {
            if (!e.isActive()) continue;
            String effect = e.getClass().getSimpleName() + "(";
            int count = e.getParamCount();
            for (int i = 0; i < e.getParamCount(); i++) {
                effect += e.get(i);
                if (i < count -1)
                    effect += "/";
            }
            add(effect + ")");
        }
    }

    public static void write(File file, String content) throws IOException {
        Files.write(Paths.get(file.toURI()), content.getBytes());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String s : this)
            sb.append(s).append(Constants.NL);
        return sb.toString();
    }
}
