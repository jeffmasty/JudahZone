package net.judah.plugin;

import java.util.ArrayList;

public class Plugins extends ArrayList<Plugin> {

    public Plugin byName(String name) {
        for (Plugin p : this) if (p.getName().equals(name)) return p;
        return null;
    }

}
