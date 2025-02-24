package net.judah.fx;

import static net.judah.synth.taco.SynthDB.*;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import net.judah.util.Constants;

public class Preset extends ArrayList<Setting> implements Comparable<Preset> {

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
    	return name;
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

	public Object toFile() {
        StringBuilder sb = new StringBuilder(OPEN).append(name).append(CLOSE).append(Constants.NL);
        for (Setting s : this)
            sb.append(s).append(Constants.NL);
        return sb.toString();
    }

	@Override
	public int compareTo(Preset preset) {
		return name.compareTo((preset).getName());
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Preset && name.equals(((Preset)o).name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
