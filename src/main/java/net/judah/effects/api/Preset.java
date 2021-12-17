package net.judah.effects.api;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.ControlPanel;
import net.judah.mixer.Channel;
import net.judah.util.Console;
import net.judah.util.Constants;

public class Preset extends ArrayList<Setting> {

    @Getter private final String name;

    public Preset(String name, List<String> raw) {
        this.name = name;
        for (String s : raw) {
            String[] first = s.split("[(]");
            String raw2 = first[1].substring(0, first[1].length() - 1);
            String[] values = raw2.split("[/]");
            Setting setting = new Setting(first[0]);
            for (int i = 0; i < values.length; i++)
                setting.add(Integer.parseInt(values[i]));
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

    /**@return comma separated list of used effects enclosed in parentheses*/
    public String condenseEffects() {
        String result = " (";
        for (int i = 0; i < size(); i++) {
            result += get(i).getEffectName();
            if (i < size() - 1)
                result += ",";
        }
        return result + ")";
    }

    public void applyPreset(Channel channel, boolean active) {
        setting:
        for (Setting s : this) {
            for (Effect e : channel) {
                if (e.getName().equals(s.getEffectName())) {
                    for (int i = 0; i < s.size(); i++)
                        e.set(i, s.get(i));
                    e.setActive(active);
                    continue setting;
                }
            }
            Console.info("Preset Error. not found: " + s.getEffectName());
        }
        channel.setPreset(this);
        channel.setPresetActive(active);
        if (ControlPanel.getInstance().getChannel().equals(channel))
            MainFrame.update(channel);
        Console.info(name + " preset applied to " + channel.getName());
    }

}
