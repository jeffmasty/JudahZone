package net.judah.tracks;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.api.Notification;
import net.judah.beatbox.Beat;
import net.judah.beatbox.BeatBox;
import net.judah.beatbox.Sequence;
import net.judah.clock.JudahClock;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class KitTrack extends Track implements Runnable {

	private final Box beatbox = new Box();
	ArrayList<Sequence> current = new ArrayList<>();
	private StepFeedback display;
	
	private void setCurrent(ArrayList<Sequence> pattern) {
		current = pattern;
	}
	
	public KitTrack(JudahClock clock, File file, File folder) {
		super(clock, "DrumKit", Type.DRUM_KIT, 9, OUT.DRUMS_OUT, folder);
		setFile(file);
		display = new StepFeedback(beatbox);
		feedback.add(display);
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
                if (first) {
                    String[] split = line.split("[/]");
                    //setMidiOut(JudahMidi.getByName(split[0]));
                    if (!split[1].equals("none"))
                        setInstrument(split[1]);
//TODO                    clock.setSteps(Integer.parseInt(split[2]));
//                    clock.setSubdivision(Integer.parseInt(split[3]));
                    first = false;
                }
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
        current = beatbox.get(0);
        RTLogger.log(this, "KIT loaded " + file.getName());
    }

	public void setInstrument(String name) {
        ArrayList<FluidInstrument> pack = (type == Type.STEP_DRUM)
                ? FluidSynth.getInstruments().getDrumkits()
                : FluidSynth.getInstruments().getInstruments();
        for (FluidInstrument instrument : pack)
            if (instrument.name.equals(name))
            	Instrument.set(this, instrument.index);
    }

	/*----- Player Interface ------*/
	@Override
	public void update(Notification.Property prop, Object value) {
		if (prop == Notification.Property.STEP) {
			step((int)value);
			display.step((int)value);
		}
	}

	public void step(int step) {
        for (Sequence seq : current) {
            for (Beat note : seq)
                if (note.getStep() == step) {
                    Midi msg = null;
                    Beat.Type type = note.getType();
                    if (Beat.Type.NoteOn == type) {
                        msg = Midi.create(Midi.NOTE_ON, 9,
                                seq.getReference().getData1(), (int)(note.getVelocity() * gain));
                    }
                    else if (Beat.Type.NoteOff == type) {
                        msg = Midi.create(Midi.NOTE_OFF, 9,
                                seq.getReference().getData1());
                    }
                    if (msg != null) 
                    	JudahMidi.queue(msg, getMidiOut());
                }
        }
    }

	@Override
	public boolean process(int knob, int data2) {
		switch (knob) {
			case 2: // instrument, overrides Tracker.knob()
				TrackView view = MainFrame.get().getTracker().getView(this);
				int idx = Constants.ratio(data2, view.getInstruments().getItemCount() - 1);
				Instrument.set(this, idx);
				view.getInstruments().setSelectedIndex(idx);
				return true;
			case 4: // Pattern
				
				return true; 
			case 7: // TODO
				
				return true;
		}
		return false;
	}

	public void changePattern(boolean up) {
		setCurrent(Box.next(up, beatbox, current));
	}

	
}
