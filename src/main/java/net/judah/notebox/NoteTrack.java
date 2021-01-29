package net.judah.notebox;

import java.util.ArrayList;

import javax.swing.JToggleButton;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import net.judah.util.Constants;

@Data
public class NoteTrack {

    private int midi;

    private float velocity = 1;
    private boolean mute = false;
    private final ArrayList<Note> beats = new ArrayList<>();

    @JsonIgnore /* gui element */
    final transient ArrayList<JToggleButton> ticks = new ArrayList<>();

    public NoteTrack(int midi) {
        this.midi = midi;
    }

    public NoteTrack(String saved) {
        if (saved.startsWith("MUTE")) {
            mute = true;
            saved = saved.replace("MUTE", "");
        }
        String[] split = saved.split("[/]");
        String[] track = split[0].split("[,]");
        this.midi = Integer.parseInt(track[0]);
        this.velocity = Float.parseFloat(track[1]);
        if (split.length > 1) {
            String[] steps = split[1].split("[,]");
            for (String step : steps)
                beats.add((new Note(Integer.parseInt(step))));
        }
    }

    public String forSave() {
        StringBuffer sb = new StringBuffer("");
        if (mute) sb.append("MUTE");
        sb.append(midi).append(",");
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
        for (Note b : beats)
            if (b.step == x) return true;
        return false;
    }

}
