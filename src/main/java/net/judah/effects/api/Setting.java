package net.judah.effects.api;

import java.awt.geom.IllegalPathStateException;
import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.judah.effects.Chorus;
import net.judah.effects.Compression;
import net.judah.effects.CutFilter;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.Fader;
import net.judah.effects.Freeverb;
import net.judah.effects.LFO;
import net.judah.effects.Overdrive;

@Data @EqualsAndHashCode(callSuper=true)
public class Setting extends ArrayList<Float> {

    public Setting(String clazz) {
        if (clazz.equals(Chorus.class.getSimpleName()))
            effect = Chorus.class;
        else if (clazz.equals(Compression.class.getSimpleName()))
            effect = Compression.class;
        else if (clazz.equals(CutFilter.class.getSimpleName()))
            effect = CutFilter.class;
        else if (clazz.equals(Delay.class.getSimpleName()))
            effect = Delay.class;
        else if (clazz.equals(EQ.class.getSimpleName()))
            effect = EQ.class;
        else if (clazz.equals(Fader.class.getSimpleName()))
            effect = Fader.class;
        else if (clazz.equals(Freeverb.class.getSimpleName()))
            effect = Freeverb.class;
        else if (clazz.equals(LFO.class.getSimpleName()))
            effect = LFO.class;
        else if (clazz.equals(Overdrive.class.getSimpleName()))
            effect = Overdrive.class;
        else throw new IllegalPathStateException(clazz);
    }

    public Setting(Effect effect) {
        this.effect = effect.getClass();
        for (int i = 0; i < effect.getParamCount(); i++)
        add(Float.valueOf(effect.get(i).toString()));
    }

    private final Class<?> effect;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(effect.getSimpleName()).append("(");
        for (int i = 0; i < this.size(); i++) {
            sb.append(get(i));
            if (i < this.size() -1)
                sb.append("/");
        }
        sb.append(")");
        return sb.toString();
    }

}
