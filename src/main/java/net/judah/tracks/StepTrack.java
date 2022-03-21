package net.judah.tracks;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.judah.api.Midi;
import net.judah.api.TimeProvider;
import net.judah.beatbox.Beat;
import net.judah.beatbox.BeatBox;
import net.judah.beatbox.GMDrum;
import net.judah.beatbox.MidiBase;
import net.judah.beatbox.Sequence;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Console;
import net.judah.util.RTLogger;

@Data @EqualsAndHashCode(callSuper = true)
public class StepTrack extends Track implements Runnable {

    private static final String MUTE = "MUTE";
    private static final String NONE = "none";

    private final JLabel filename;
   
	private byte data1 = 0;

	private ArrayList<ArrayList<Sequence>> beatbox = new ArrayList<>();
	ArrayList<Sequence> current = new ArrayList<>();
    private FluidInstrument instrument;
	
    
	public StepTrack(TimeProvider clock, String name, Type type, int channel, byte data1, OUT port) {
		super(clock, name, type, channel, port);
		this.data1 = data1;
		current.add(new Sequence(new MidiBase(GMDrum.lookup(data1))));
		filename = new JLabel("()");
		add(filename);
	}
		
	public StepTrack(TimeProvider clock, String name, Type type, byte data1, OUT port) {
		this(clock, name, type, 9, data1, port);
	}

	public StepTrack(TimeProvider clock, String name, Type type, OUT port) {
		this(clock, name, type, 9, GMDrum.Cowbell.toByte(), port);
	}
	
	@Override
	public void setFile(File file) {
		if ((file == null && this.file == null) || (file != null && file.equals(this.file)))
			return;

		beatbox.clear();
        if (file == null || !file.isFile()) {
            Console.info("no pattern file " + file);
            return;
        }
        this.file = file;
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
                    if (!split[1].equals(NONE))
                        setInstrument(split[1]);
//TODO                    clock.setSteps(Integer.parseInt(split[2]));
//                    clock.setSubdivision(Integer.parseInt(split[3]));
                    first = false;
                }
                else if (line.startsWith("!")) {
                    if (onDeck != null)
                        beatbox.add(onDeck);
                    onDeck = new ArrayList<>();
//                    if (line.contains(MUTE)) {
//                    	
                    	// setMute(true);
//                    	if (type == Type.Drums) continue;
//                        line = line.substring(line.indexOf(MUTE) + MUTE.length());
//                        if (line.length() < 10) continue;
//                        Chord chord = Chord.fromFile(line);
//                        onDeck.setScale(chord.getScale());
//                        onDeck.setKey(chord.getKey());
//                        onDeck.setOctave(chord.getOctave());
//                    }
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
        RTLogger.log(this, "loaded " + file.getName());
		filename.setText(file == null ? "" : file.getName());
		repaint();

//        if (BeatsView.getInstance() != null && this == BeatsView.getSequencer())
//            BeatsView.getInstance().updateKit(this, current.createTracks());

    }

	/*----- Player Interface ------*/

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.STEP)
			step((int)value);
	}

	public void step(int step) {
        for (Sequence seq : current) {
            // if (seq.isMute()) continue;
            for (Beat note : seq)
                if (note.getStep() == step) {
//                    int volume = Math.round(note.getVelocity() *
//							seq.getVelocity() /** (current.getVolume() * 1.27f) */);
                	int volume = 100;
                    Midi msg = null;
                    Beat.Type type = note.getType();
                    if (Beat.Type.NoteOn == type) {
                        msg = Midi.create(Midi.NOTE_ON, ch > 9 ? 9 : ch,
                                seq.getReference().getData1(), volume);
                    }
                    else if (Beat.Type.NoteOff == type) {
                        msg = Midi.create(Midi.NOTE_OFF, ch > 9 ? 9 : ch,
                                seq.getReference().getData1(), volume);
                    }
                    // TODO internal commands/CC and chords
                    if (msg != null) 
                    	JudahMidi.queue(msg, getMidiOut());
                    	
                }
        }
    }

	
	@Override
	public JPanel subGui() {
		JPanel result = new JPanel();
		if (data1 != 0)
			result.add(new JLabel("" + GMDrum.lookup(data1)));
			
		result.add(new JLabel("todo"));
		result.setBackground(Color.CYAN);
		return result;
	}

	
	    public void setInstrument(int index) {
        if (index < 0) return;
        ArrayList<FluidInstrument> list = (type == Type.STEP_DRUM)
                ? FluidSynth.getInstruments().getDrumkits()
                : FluidSynth.getInstruments().getInstruments();
        FluidInstrument next = list.get(index);
        if (next.equals(getInstrument())) return;
        noteOff();
        setInstrument(list.get(index));

    }

    public void setInstrument(String name) {
        ArrayList<FluidInstrument> pack = (type == Type.STEP_DRUM)
                ? FluidSynth.getInstruments().getDrumkits()
                : FluidSynth.getInstruments().getInstruments();
        for (FluidInstrument instrument : pack)
            if (instrument.name.equals(name))
                setInstrument(instrument);
    }

    public void setInstrument(FluidInstrument patch) {
        instrument = patch;
        try {
            Midi midi = new Midi(ShortMessage.PROGRAM_CHANGE, ch, instrument.index);
            JudahMidi.queue(midi, getMidiOut());
        } catch (InvalidMidiDataException e) { Console.warn(e); }
    }


	
    /** clock end, send any note-offs found in the sequencer */
    public void noteOff() {
        for (Sequence seq : current)
            for (Beat note : seq)
                if (note.getType() == Beat.Type.NoteOff) {
                    JudahMidi.queue(Midi.create(Midi.NOTE_OFF, ch,
                            seq.getReference().getData1(), 127), getMidiOut());
                    continue;
                }
    }

}
