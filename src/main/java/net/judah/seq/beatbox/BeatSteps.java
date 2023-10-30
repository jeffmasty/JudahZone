package net.judah.seq.beatbox;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.Signature;
import net.judah.gui.Pastels;
import net.judah.seq.Steps;
import net.judah.seq.track.MidiTrack;

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
    	createLabels(track.getClock().getTimeSig());
    	addLbls();
    }
    
    public ArrayList<BeatLabel> createLabels(Signature sig) {
        all.clear();
        total =  2 * sig.steps;
        unit = width / total;
        int stepsPerBeat = track.getClock().getSteps() / sig.beats;
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
        for (int i = 0; i < all.size(); i++) {
            BeatLabel lbl = all.get(i);
            lbl.setBounds((int)(i * unit) + 3 + BeatBox.X_OFF, 1, 26, 26);
            add(lbl);
        }
	}

	@Override
	public void timeSig(Signature sig) { // TODO test time signature
		createLabels(sig);
		addLbls();
		invalidate();
	}

	@Override
	public void highlight(Point p) {
		
	}
	


}
