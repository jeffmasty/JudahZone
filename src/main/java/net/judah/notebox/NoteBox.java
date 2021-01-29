package net.judah.notebox;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahClock;
import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.settings.Channels;
import net.judah.util.Console;
import net.judah.util.Constants;


public class NoteBox extends ArrayList<NoteTrack> {
    public static final File FOLDER = new File(Constants.ROOT, "sequences");

    @Getter private static final ConcurrentLinkedQueue<ShortMessage>
        queue = new ConcurrentLinkedQueue<>();

    private final JudahClock clock;
    @Setter @Getter private File file;

    @Getter int channel = 1;
    @Getter Scale scale = Scale.Major;
    @Getter Key key = Key.C;
    int root = 48;

    @Getter FluidInstrument instrument = FluidSynth.getInstruments().get(0);

    @Setter @Getter private static JackPort midiOut = JudahMidi.getInstance().getSynthOut();

    NoteBoxGui gui;

    public NoteBox() {
        clock = JudahClock.getInstance();
        assert clock != null;
        initialize();
    }

    public void initialize() {
        file = null;
        clear();
        for (int i = 20; i >= 0; i--)
            add(new NoteTrack(i));
        if (gui != null) gui.initialize();
    }

    public void process(int step) {
        try {
            for (NoteTrack track : this) {
                if (track.isMute()) continue;
                for (Note b : track.getBeats())
                    if (b.step == step) {
                        int volume = Math.round(
                                track.getVelocity() * b.getVelocity() * 127f);
                        Midi msg = Midi.create(
                                Midi.NOTE_ON, channel, track.getMidi() + root, volume);
                        queue.add(msg);
                    }
            }
        } catch (Throwable t) {
            Console.warn(step + ": " + t.getMessage(), t);
        }
    }

    public void setInstrument(FluidInstrument instrument) {
        this.instrument = instrument;
        getQueue().add(Midi.create(Midi.PROGRAM_CHANGE, 9, instrument.index, 0));
    }

    public NoteBoxGui getGui() {
        if (gui == null)
            gui = new NoteBoxGui(this);
        return gui;
    }

    public boolean load(File file) {
        if (file == null || !file.isFile()) {
            Console.info("no pattern file " + file);
            return false;
        }
        ArrayList<String> raw = new ArrayList<>();
        Scanner scanner = null;
        try {
            boolean first = true;
            scanner = new Scanner(file);
            clear();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //Console.info(new String(line));
                if (first) {
                    String[] split = line.split("[/]");
                    setInstrument(FluidSynth.getInstruments().get(Integer.parseInt(split[0])));
                    clock.setSteps(Integer.parseInt(split[1]));
                    clock.setSubdivision(Integer.parseInt(split[2]));
                    first = false;
                }
                else
                    add(new NoteTrack(line));
            }
            this.file = file;
        } catch (Throwable t) {
            Console.warn(t); return false;
        } finally {
            if (scanner != null) scanner.close();
        }
        for (String saved: raw)
            add(new NoteTrack(saved));
        if (gui != null) gui.initialize();
        Console.info("Loaded notebox " + file.getName());
        return true;
    }

    public void write(File f) {
        StringBuffer raw = new StringBuffer();
        raw.append(instrument.index).append("/").append(clock.getSteps());
        raw.append("/").append(clock.getSubdivision()).append(Constants.NL);
        for (NoteTrack t : this)
            raw.append(t.forSave());
        try {
            Constants.writeToFile(f, raw.toString());
        } catch (Exception e) { Console.warn(e); }
    }

    public static String volumeTarget() {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getAuxOut1())
            return channels.getAux1().getName();
        else if (midiOut == midi.getAuxOut2())
            return channels.getAux2().getName();
        else if (midiOut == midi.getAuxOut3())
            return channels.getDrums().getName();
        else if (midiOut == midi.getDrumsOut())
            return channels.getDrums().getName();
        else if (midiOut == midi.getSynthOut())
            return channels.getSynth().getName();
        return "?";
    }

    public static void setVolume(int vol) {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getAuxOut1())
            channels.getAux1().setVolume(vol);
        else if (midiOut == midi.getAuxOut2())
            channels.getAux2().setVolume(vol);
        else if (midiOut == midi.getAuxOut3())
            channels.getDrums().setVolume(vol);
        else if (midiOut == midi.getDrumsOut())
            channels.getDrums().setVolume(vol);
        else if (midiOut == midi.getSynthOut())
            channels.getSynth().setVolume(vol);
    }

    public static int getVolume() {
        JudahMidi midi = JudahMidi.getInstance();
        Channels channels = JudahZone.getChannels();
        if (midiOut == midi.getAuxOut1())
            return channels.getAux1().getVolume();
        else if (midiOut == midi.getAuxOut2())
            return channels.getAux2().getVolume();
        else if (midiOut == midi.getAuxOut3())
            return channels.getDrums().getVolume();
        else if (midiOut == midi.getDrumsOut())
            return channels.getDrums().getVolume();
        else if (midiOut == midi.getSynthOut())
            return channels.getSynth().getVolume();
        return 0;
    }


}
