package net.judah.beatbox;

import java.util.ArrayList;

import javax.swing.JToggleButton;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.judah.beatbox.BeatBox.Type;
import net.judah.util.Constants;

/** one line of a grid */
@Data @EqualsAndHashCode(callSuper = true)
public class Sequence extends ArrayList<Beat> {

    private MidiBase reference;
    private float velocity = 0.8f;
    private boolean mute = false;

        public Sequence(String saved, Type type) {
        if (saved.startsWith("MUTE")) {
            mute = true;
            saved = saved.replace("MUTE", "");
        }
        String[] split = saved.split("[/]");
        String[] track = split[0].split("[,]");

        reference = (type == Type.Drums)
            ? new MidiBase(GMDrum.values()[Integer.parseInt(track[0])])
            : new MidiBase(Integer.parseInt(track[0]));

        this.velocity = Float.parseFloat(track[1]);
        if (split.length > 1) {
            String[] steps = split[1].split("[,]");
            for (int i = 0; i < steps.length; i++) {
                String[] step = steps[i].split("[:]");
                add(new Beat(Integer.parseInt(step[0]), Beat.Type.valueOf(step[1])));
            }
        }
    }
    
    public Sequence(MidiBase reference) {
        this.reference = reference;
    }

    @JsonIgnore /* gui element */
    final transient ArrayList<JToggleButton> ticks = new ArrayList<>();

    public Beat getStep(int x) {
        for (Beat b : this)
            if (b.getStep() == x) return b;
        return null;
    }

    public boolean hasStep(int x) {
        return getStep(x) != null;
    }

    public String forSave(Type type) {
        StringBuffer sb = new StringBuffer("");
        if (mute) sb.append("MUTE");
        if (type == Type.Drums)
            sb.append(reference.getDrum().ordinal()).append(",");
        else
            sb.append(reference.getData1()).append(",");
        sb.append(velocity);
        sb.append("/");
        int last = size() -1;
        for (int i = 0 ; i < size(); i++) {
            sb.append(get(i).getStep());
            sb.append(":");
            sb.append(get(i).getType().name());
            if (i != last) sb.append(",");
        }
        return sb.append(Constants.NL).toString();
    }


}
