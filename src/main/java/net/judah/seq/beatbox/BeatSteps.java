package net.judah.seq.beatbox;

import java.awt.Rectangle;
import java.util.ArrayList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.Pastels;
import net.judah.seq.MidiTrack;
import net.judah.seq.Steps;

@RequiredArgsConstructor
public class BeatSteps extends Steps {

	private final MidiTrack track;
    private ArrayList<BeatLabel> all = new ArrayList<>();
    private BeatLabel previous;
    private final int width;
    @Getter private float unit;
    @Getter private float total;

    
    public BeatSteps(Rectangle r, MidiTrack track) {
    	this.track = track;
    	this.width = r.width;
    	setLayout(null);
    	setBounds(r);
    	createLabels();
    	addLbls();
    }
    
    public ArrayList<BeatLabel> createLabels() {
        all.clear();
        total =  2 * track.getClock().getSteps();
        int stepsPerBeat = track.getClock().getSteps() / track.getClock().getMeasure();
        int beat = 1;
        for (int x = 0; x < total; x++) {
            if (x % stepsPerBeat == 0) {
                BeatLabel b = new BeatLabel(track, "  " + beat++, Pastels.BLUE);
                all.add(b);
            }
            else if ((x + 2) % stepsPerBeat == 0 && stepsPerBeat == 4)
                all.add(new BeatLabel(track, "  +"));
            else 
            	all.add(new BeatLabel(track, "  "));
        }
        for (BeatLabel l : all)
        	l.setActive(false);
        return all;
    }

    // TODO fewer notes if div > 5
    public void setActive(int step) {
        if (previous != null) previous.setActive(false);
        previous = all.get(step);
        previous.setActive(true);
    }

	@Override
	public void setStart(int num) {
	}

	private void addLbls() {
		unit = width / (2 * track.getClock().getSteps());
        for (int i = 0; i < all.size(); i++) {
            BeatLabel lbl = all.get(i);
            lbl.setBounds((int)(i * unit) + 3 + BeatBox.X_OFF, 1, 26, 26);
            add(lbl);
        }
	}

	public void timeSig() { // TODO test time signature
		createLabels();
		addLbls();
		invalidate();
	}
	


}
