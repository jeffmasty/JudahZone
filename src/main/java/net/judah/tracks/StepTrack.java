package net.judah.tracks;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.api.Notification;
import net.judah.beatbox.Beat;
import net.judah.beatbox.BeatBox;
import net.judah.beatbox.JudahKit;
import net.judah.beatbox.MidiBase;
import net.judah.beatbox.Sequence;
import net.judah.clock.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Data @EqualsAndHashCode(callSuper = true)
public class StepTrack extends Track implements Runnable {

    float vol2 = 1f;
    
	@Getter private Box beatbox = new Box();
	private StepFeedback display;
	private Cycle cycle = new Cycle();
	
    public StepTrack(JudahClock clock, StepDrum sequencer, File folder) {
    	super(clock, sequencer.getClass().getSimpleName(), Type.STEP_DRUM, OUT.CALF_OUT, folder);
    	beatbox = sequencer.getBeatBox();
    	beatbox.setCurrent(beatbox.get(0));
    	display = new StepFeedback(beatbox);
    	display.refresh();
    	feedback.add(display); 
    }
    
    public StepTrack(JudahClock clock, File file, File folder) {
    	super(clock, file.getName(), Type.STEP_DRUM, 9, OUT.CALF_OUT, folder);
    	display = new StepFeedback(beatbox);
    	feedback.add(display);
    	setFile(file);
    }
    
	@Override
	public void setFile(File file) {
		beatbox.clear();
		this.file = file;
        if (file != null && file.isFile())
        	new Thread(this).start();
	}
	
	@Override
    public void run() {
        Scanner scanner = null;
        try {
            ArrayList<Sequence> onDeck = null;
            boolean first = true;
            scanner = new Scanner(file);
            beatbox.clear();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (first) 
                    first = false;
                else if (line.startsWith("!")) {
                    if (onDeck != null)
                        beatbox.add(onDeck);
                    onDeck = new ArrayList<>();
                }
                else {
                    onDeck.add(new Sequence(line, BeatBox.Type.Drums));
                }
            }
            beatbox.add(onDeck);
        } catch (Throwable t) {
            Console.warn(t); return;
        } finally {
            if (scanner != null) scanner.close();
        }
        beatbox.setCurrent(beatbox.get(0));
        RTLogger.log(this, "loaded step track " + file.getName());
        display.refresh();
        MainFrame.update(this);
    }

	/*----- Player Interface ------*/

	@Override
	public void update(Notification.Property prop, Object value) {
		if (prop == Notification.Property.STEP) {
			step((int)value);
			display.step((int)value);
		}
		else if (prop == Notification.Property.BARS) {
			ArrayList<Sequence> next = cycle.cycle(beatbox);
			if (next != beatbox.getCurrent()) {
				beatbox.setCurrent(next);
				MainFrame.update(this);
			}
		}

	}

	public void step(int step) {
		boolean firstSequence = true;
        for (Sequence seq : beatbox.getCurrent()) {
        	float trackVol = firstSequence ? gain : vol2;
        	firstSequence = false;
            // if (seq.isMute()) continue;
            for (Beat note : seq)
                if (note.getStep() == step) {
                    Midi msg = null;
                    Beat.Type type = note.getType();
                    if (Beat.Type.NoteOn == type) {
                        msg = Midi.create(Midi.NOTE_ON, ch > 9 ? 9 : ch,
                                seq.getReference().getData1(), (int)(note.getVelocity() * trackVol));
                    }
                    else if (Beat.Type.NoteOff == type) {
                        msg = Midi.create(Midi.NOTE_OFF, ch > 9 ? 9 : ch,
                                seq.getReference().getData1(), (int)(note.getVelocity() * trackVol));
                    }
                    if (msg != null) 
                    	JudahMidi.queue(msg, getMidiOut());
                    	
                }
        }
    }

    /** clock end, send any note-offs found in the sequencer */
    public void noteOff() {
        for (Sequence seq : beatbox.getCurrent())
            for (Beat note : seq)
                if (note.getType() == Beat.Type.NoteOff) {
                    JudahMidi.queue(Midi.create(Midi.NOTE_OFF, ch,
                            seq.getReference().getData1(), 127), getMidiOut());
                    continue;
                }
    }

	public void changePattern(boolean forward) {
		
        int idx = beatbox.indexOf(beatbox.getCurrent());
        ArrayList<Sequence> current;
        if (forward) {
            if (idx == beatbox.size() - 1)
                current = beatbox.get(0);
            else
                current = beatbox.get(idx + 1);
        }
        else {
            if (idx == 0) 
            	current = beatbox.get(beatbox.size() - 1);
            else
            	current = beatbox.get(idx - 1);
        }
        beatbox.setCurrent(current);
        display.refresh();
	}

	@Override
	public boolean process(int knob, int data2) {
		switch (knob) {
			case 4: // pattern
				beatbox.setCurrent(beatbox.get(Constants.ratio(data2 -1, beatbox.size())));
				MainFrame.update(this);
				return true;
			case 5: // cycle
				cycle.setSelectedIndex(Constants.ratio(data2 - 1, Cycle.CYCLES.length));
				return true;
			case 6: // GMDRUM 2
				setInstrument(1, data2);
				return true;
			case 7: // vol2
				setVol2(data2 * 0.01f); 
				return true;
		}
		return false;
	}

	public void setInstrument(int seqNum, int data2) {
		int idx = Constants.ratio(data2, JudahKit.values().length - 1);
		int midi = JudahKit.values()[idx].getMidi();
		// iterate patterns, changing track beats to new midi instrument
		for (ArrayList<Sequence> pattern : getBeatbox()) 
			pattern.get(seqNum).setReference(new MidiBase(midi));
		MainFrame.update(this);
	}
	
}
