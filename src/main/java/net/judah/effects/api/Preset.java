package net.judah.effects.api;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Console;
import net.judah.util.Constants;

public class Preset extends ArrayList<Setting> {

    @Getter private final String name;
    @Getter @Setter private boolean active;

    public Preset(String name, List<String> raw) {
        this.name = name;
        for (String s : raw) {
            String[] first = s.split("[(]");
            String raw2 = first[1].substring(0, first[1].length() - 1);
            String[] values = raw2.split("[/]");
            Setting setting = new Setting(first[0]);
            for (int i = 0; i < values.length; i++)
                setting.add(Float.parseFloat(values[i]));
            add(setting);

        }

    }

    public Preset(String name, ArrayList<Setting> settings) {
        this.name = name;
        addAll(settings);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[").append(name).append("]").append(Constants.NL);
        for (Setting s : this)
            sb.append(s).append(Constants.NL);
        return sb.toString();
    }

    public void applyPreset(List<Effect> channel) {
        setting:
        for (Setting s : this) {
            for (Effect e : channel) {
                if (e.getClass().getSimpleName().equals(s.getEffect().getSimpleName())) {
                    for (int i = 0; i < s.size(); i++)
                        e.set(i, s.get(i));
                    e.setActive(true);
                    continue setting;
                }
            }
            Console.info("Preset Error. not found: " + s.getEffect().getClass().getCanonicalName());
        }
        Console.info(name + " preset applied.");
    }


}
