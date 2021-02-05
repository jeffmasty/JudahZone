package net.judah.beatbox;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.judah.JudahClock;
import net.judah.api.Midi;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.util.Console;
import net.judah.util.Constants;

@Getter @EqualsAndHashCode(callSuper=true)
public class BeatBox extends ArrayList<Grid> implements Runnable {
    public static enum Type { Drums, Melodic }
    private static final String MUTE = "MUTE";
    private static final String NONE = "none";

    @Setter @Getter private JackPort midiOut;
    @Getter private final ConcurrentLinkedQueue<ShortMessage>
            queue = new ConcurrentLinkedQueue<>();
    File file;
    private final JudahClock clock;
    private final int midiChannel;
    private final Type type;
    private FluidInstrument instrument;
    Grid current; // current grid pattern

    public BeatBox(int midiChannel) {
        clock = JudahClock.getInstance();
        this.midiChannel = midiChannel;
        this.type = midiChannel == 9 ? Type.Drums : Type.Melodic;
        midiOut = type == Type.Drums
                ? JudahMidi.getInstance().getAuxOut3()
                : JudahMidi.getInstance().getSynthOut();
        current = create();
    }

    public void process(int step) {
        if (current.isMute()) return;
        for (Sequence seq : current) {
            if (seq.isMute()) continue;
            for (Beat note : seq)
                if (note.getStep() == step) {
                    int volume = Math.round(note.getVelocity() *
                            seq.getVelocity() *  (current.getVolume() * 1.27f));
                    Midi msg = null;
                    Beat.Type type = note.getType();
                    if (Beat.Type.NoteOn == type) {
                        msg = Midi.create(Midi.NOTE_ON, midiChannel,
                                seq.getReference().getData1(), volume);
                    }
                    else if (Beat.Type.NoteOff == type) {
                        msg = Midi.create(Midi.NOTE_OFF, midiChannel,
                                seq.getReference().getData1(), volume);
                    }
                    // TODO internal commands/CC and chords
                    if (msg != null)
                        queue.add(msg);

                }
        }
    }

    /** clock end, send any note-offs found in the sequencer */
    public void noteOff() {
        for (Sequence seq : current)
            for (Beat note : seq)
                if (note.getType() == Beat.Type.NoteOff) {
                    queue.add(Midi.create(Midi.NOTE_OFF, midiChannel,
                            seq.getReference().getData1(), 127));
                    continue;
                }
    }

    private Grid create() {
        Grid g = new Grid(this);
        add(g);
        return g;
    }

    public void setCurrent(Grid g) {
        if (contains(g) == false) throw new InvalidParameterException();
        Console.info("ch:" + midiChannel + " to pattern " + indexOf(g) + (g.isMute() ? " MUTE" : ""));
        current = g;
    }

    public void setInstrument(int index) {
        if (index < 0) return;
        ArrayList<FluidInstrument> list = (type == Type.Drums)
                ? FluidSynth.getInstruments().getDrumkits()
                : FluidSynth.getInstruments().getInstruments();
        FluidInstrument next = list.get(index);
        if (next.equals(getInstrument())) return;
        noteOff();
        setInstrument(list.get(index));

    }

    public void setInstrument(String name) {
        ArrayList<FluidInstrument> pack = (type == Type.Drums)
                ? FluidSynth.getInstruments().getDrumkits()
                : FluidSynth.getInstruments().getInstruments();
        for (FluidInstrument instrument : pack)
            if (instrument.name.equals(name))
                setInstrument(instrument);
    }

    public void setInstrument(FluidInstrument patch) {
        instrument = patch;
        try {
            Midi midi = new Midi(ShortMessage.PROGRAM_CHANGE, midiChannel, instrument.index);
            queue.add(midi);
        } catch (InvalidMidiDataException e) { Console.warn(e); }
    }

    // TODO handle pattern mute/velocity, Instrument,
    public void load(File file) {
        clear();
        if (file == null || !file.isFile()) {
            Console.info("no pattern file " + file);
            return;
        }
        this.file = file;
        new Thread(this).start();
    }

    public void write(File f) {
        StringBuffer raw = new StringBuffer();
        raw.append(getMidiOut().getShortName()).append("/");
        raw.append(getInstrument() == null ? NONE : getInstrument().name);
        raw.append("/").append(clock.getSteps());
        raw.append("/").append(clock.getSubdivision()).append(Constants.NL);
        for (Grid grid : this) {
            raw.append("!");
            if (grid.isMute())
                raw.append("MUTE");

            raw.append(Constants.NL);

            for (Sequence t : grid)
                raw.append(t.forSave(getType()));
        }
        try {
            Constants.writeToFile(f, raw.toString());
        } catch (Exception e) { Console.warn(e); }
    }

    @Override
    public void run() {
        Scanner scanner = null;
        try {
            Grid onDeck = null;
            boolean first = true;
            scanner = new Scanner(file);
            clear();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (first) {
                    String[] split = line.split("[/]");
                    setMidiOut(JudahMidi.getByName(split[0]));
                    if (!split[1].equals(NONE))
                        setInstrument(split[1]);
                    clock.setSteps(Integer.parseInt(split[2]));
                    clock.setSubdivision(Integer.parseInt(split[3]));
                    first = false;
                }
                else if (line.startsWith("!")) {
                    if (onDeck != null)
                        add(onDeck);
                    onDeck = new Grid(this, true);
                    if (line.contains(MUTE)) {
                        onDeck.setMute(true);
                        if (type == Type.Drums) continue;
                        line = line.substring(line.indexOf(MUTE) + MUTE.length());
                        if (line.length() < 10) continue;
                        Chord chord = Chord.fromFile(line);
                        onDeck.setScale(chord.getScale());
                        onDeck.setKey(chord.getKey());
                        onDeck.setOctave(chord.getOctave());
                    }
                }
                else {
                    onDeck.add(new Sequence(line, type));
                }
            }
            add(onDeck);
        } catch (Throwable t) {
            Console.warn(t); return;
        } finally {
            if (scanner != null) scanner.close();
        }
        current = get(0);
        if (this == BeatsView.getInstance().getSequencer())
            BeatsView.getInstance().updateKit(this, current.createTracks());

    }

    public void next(boolean forward) {
        int idx = indexOf(current);
        if (forward) {
            if (idx == size() - 1)
                current = create();
            else
                current = get(idx + 1);
        }
        else {
            if (idx == 0) {
                if (size() == 1)
                    current = create();
                else current = get(size() - 1);
            }
            else current = get(idx - 1);
        }
        BeatsView.getInstance().updateKit(this, current.createTracks());


    }

}
