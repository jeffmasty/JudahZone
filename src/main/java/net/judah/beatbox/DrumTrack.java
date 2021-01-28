package net.judah.beatbox;

import java.util.ArrayList;

import javax.swing.JToggleButton;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import net.judah.util.Constants;

@Data
public class DrumTrack {

    private float velocity = 1;
    private GMDrum drum;
    private final ArrayList<Beat> beats = new ArrayList<>();

    @JsonIgnore /* gui element */
    final transient ArrayList<JToggleButton> ticks = new ArrayList<>();

    public DrumTrack(GMDrum drum) {
        this.drum = drum;
    }

    public DrumTrack(String saved) {
        String[] split = saved.split("[/]");
        String[] track = split[0].split("[,]");
        this.drum = GMDrum.values()[Integer.parseInt(track[0])];
        this.velocity = Float.parseFloat(track[1]);
        if (split.length > 1) {
            String[] steps = split[1].split("[,]");
            for (String step : steps)
                beats.add((new Beat(Integer.parseInt(step))));
        }
    }

    public String forSave() {
        StringBuffer sb = new StringBuffer(drum.ordinal() + ",");
        sb.append(velocity);
        sb.append("/");
        int last = beats.size() -1;
        for (int i = 0 ; i < beats.size(); i++) {
            sb.append(beats.get(i).step);
            if (i != last) sb.append(",");
        }
        return sb.append(Constants.NL).toString();
    }

    public boolean hasStep(int x) {
        for (Beat b : beats)
            if (b.step == x) return true;
        return false;
    }

}
