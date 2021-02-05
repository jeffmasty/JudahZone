package net.judah.midi;
import static net.judah.settings.MidiSetup.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackMidi.Event;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahClock;
import net.judah.JudahZone;
import net.judah.StageCommands;
import net.judah.api.BasicClient;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.api.Service;
import net.judah.api.Status;
import net.judah.beatbox.BeatBox;
import net.judah.fluid.FluidSynth;
import net.judah.plugin.BeatBuddy;
import net.judah.plugin.MPK;
import net.judah.plugin.Pedal;
import net.judah.sequencer.Sequencer;
import net.judah.settings.MidiSetup.IN;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.RTLogger;

/** handle MIDI for the project */
@Log4j
public class JudahMidi extends BasicClient implements Service, MidiQueue {

    public static final String JACKCLIENT_NAME = "JudahMidi";
    private long frame = 0;
    private String[] sources, destinations; // for GUI

    @Getter private static JudahMidi instance;
    /** current Jack Frame */
    public static long getCurrent() {
        return instance.scheduler.getCurrent();
    }

    @Getter private ArrayList<JackPort> inPorts = new ArrayList<>();  // Keyboard, Pedal, MidiIn
    @Getter private JackPort keyboard;
    @Getter private JackPort pedal;
    @Getter private JackPort drumsIn;
    @Getter private JackPort auxIn1;
    @Getter private JackPort auxIn2;
    @Getter private JackPort auxIn3;

    @Getter private ArrayList<JackPort> outPorts = new ArrayList<>(); // Synth, Effects, MidiOut
    @Getter private JackPort synthOut;
    @Getter private JackPort drumsOut;
    @Getter private JackPort auxOut1;
    @Getter private JackPort auxOut2;
    @Getter private JackPort auxOut3;
    @Getter private final String[] NAMES;
    /** name -> midiChannel */
    @Getter private final HashMap<String, Integer> midiChannelNames = new HashMap<>();
    /** midiChannel -> defaultJackPort */
    @Getter private final HashMap<Integer, JackPort> defaultMidiChannelOut = new HashMap<>();

    @Getter private final MidiScheduler scheduler = new MidiScheduler(this);
    @Getter private final Router router = new Router();
    @Getter private final ConcurrentLinkedQueue<ShortMessage> queue = new ConcurrentLinkedQueue<>();
    @Getter private final BeatBuddy clock;

    @Getter MidiCommands commands = new MidiCommands(this);
    private final StageCommands stage = new StageCommands();

    // for process()
    private Event midiEvent = new JackMidi.Event();
    private byte[] data = null;
    private int index, eventCount, size;
    private ShortMessage poll;
    private Midi midi;


    public JudahMidi(String name, BeatBuddy drumMachine) throws JackException {

        super(name);
        instance = this;
        this.clock = drumMachine;
        start();
        NAMES = channelMap();
    }

    private String[] channelMap() {
        String [] result = new String[] {
                "DRUMS", "NOTES", "CHORD", "DRUM2", "SDRUM", "BUDDY",
                "NOTE2", "NOTE3", "NOTE4", "NOTE5", "NOTE6",
                "NOTE7", "NOTE8", "CHRD2", "CHRD3", "CHRD4"
        };
        // Melody channels
        midiChannelNames.put("NOTES", 0);
        defaultMidiChannelOut.put(0, synthOut);

        midiChannelNames.put("NOTE2", 1);
        defaultMidiChannelOut.put(1, synthOut);

        midiChannelNames.put("NOTE3", 2);
        defaultMidiChannelOut.put(2, synthOut);

        midiChannelNames.put("NOTE4", 3);
        defaultMidiChannelOut.put(3, synthOut);

        midiChannelNames.put("NOTE5", 4);
        defaultMidiChannelOut.put(4, auxOut3);

        midiChannelNames.put("NOTE6", 5);
        defaultMidiChannelOut.put(5, auxOut3);

        midiChannelNames.put("NOTE7", 6);
        defaultMidiChannelOut.put(6, auxOut1);

        midiChannelNames.put("NOTE8", 7);
        defaultMidiChannelOut.put(7, auxOut2);

        // Drum channels
        midiChannelNames.put("BUDDY", 8);
        defaultMidiChannelOut.put(8, drumsOut);
        midiChannelNames.put("SDRUM", 9);
        defaultMidiChannelOut.put(9, synthOut);
        midiChannelNames.put("DRUM2", 10);
        defaultMidiChannelOut.put(10, auxOut3);
        midiChannelNames.put("DRUMS", 11);
        defaultMidiChannelOut.put(11, auxOut3);

        // Chords channels
        midiChannelNames.put("CHORD", 12);
        defaultMidiChannelOut.put(12, synthOut);
        midiChannelNames.put("CHRD2", 13);
        defaultMidiChannelOut.put(13, synthOut);
        midiChannelNames.put("CHRD3", 14);
        defaultMidiChannelOut.put(14, auxOut3);
        midiChannelNames.put("CHRD4", 15);
        defaultMidiChannelOut.put(15, auxOut1);
        return result;
    }

    // To be revived
    //public void routeChannel(HashMap<String, Object> props) {
    //    try {
    //        boolean active = Boolean.parseBoolean("" + props.get("Active"));
    //        int from = Integer.parseInt("" + props.get("from"));
    //        int to = Integer.parseInt("" + props.get("to"));
    //        Route route = new Route(from, to);
    //        if (active)
    //            getRouter().add(route);
    //        else
    //            getRouter().remove(route);
    //    } catch (NumberFormatException e) {
    //        log.error(e.getMessage() + " " + Constants.prettyPrint(props));
    //    }
    //}

    @Override
    protected void initialize() throws JackException {

        for (OUT port : OUT.values())
            outPorts.add(jackclient.registerPort(port.name, MIDI, JackPortIsOutput));

        for (IN port : IN.values())
            inPorts.add(jackclient.registerPort(port.name, MIDI, JackPortIsInput));

        int sz = inPorts.size();

        if (sz > IN.KEYBOARD.ordinal()) keyboard = inPorts.get(IN.KEYBOARD.ordinal());
        if (sz > IN.PEDAL.ordinal()) pedal = inPorts.get(IN.PEDAL.ordinal());
        if (sz > IN.DRUMS_IN.ordinal()) drumsIn = inPorts.get(IN.DRUMS_IN.ordinal());
        if (sz > IN.AUX1_IN.ordinal()) auxIn1 = inPorts.get(IN.AUX1_IN.ordinal());
        if (sz > IN.AUX2_IN.ordinal()) auxIn2 = inPorts.get(IN.AUX2_IN.ordinal());
        if (sz > IN.AUX3_IN.ordinal()) auxIn3= inPorts.get(IN.AUX3_IN.ordinal());

        sz = outPorts.size();
        if (sz > OUT.SYNTH_OUT.ordinal()) synthOut = outPorts.get(OUT.SYNTH_OUT.ordinal());
        if (sz > OUT.DRUMS_OUT.ordinal()) drumsOut = outPorts.get(OUT.DRUMS_OUT.ordinal());
        if (sz > OUT.AUX1_OUT.ordinal()) auxOut1 = outPorts.get(OUT.AUX1_OUT.ordinal());
        if (sz > OUT.AUX2_OUT.ordinal()) auxOut2 = outPorts.get(OUT.AUX2_OUT.ordinal());
        if (sz > OUT.AUX2_OUT.ordinal()) auxOut3 = outPorts.get(OUT.AUX3_OUT.ordinal());
        //	jackclient.setTimebaseCallback(this, false);
        new Thread(scheduler).start();

        clock.setOut(drumsOut);// wire-up drum machine midi messages

    }

    @SuppressWarnings("deprecation")
    @Override
    protected void makeConnections() throws JackException {

        for (String port : jack.getPorts(jackclient, Pedal.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
            log.debug("connecting foot pedal: " + port + " to " + pedal.getName());
            jack.connect(jackclient, port, pedal.getName());
        }
        for (String port : jack.getPorts(jackclient, MPK.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
            log.debug("connecting keyboard: " + port + " to " + keyboard.getName());
            jack.connect(jackclient, port, keyboard.getName());
        }

        String[] usbSource = jack.getPorts("MIDI2x2", MIDI, EnumSet.of(JackPortIsOutput));
        if (usbSource.length == 2) {
            for (String portname : usbSource) {
                if (portname.contains(" MIDI 1"))
                    jack.connect(jackclient, portname, drumsIn.getName());
                else if (portname.contains(" MIDI 2"))
                    jack.connect(jackclient, portname, auxIn1.getName());
            }
        }

        String[] usbSink = jack.getPorts("MIDI2x2", MIDI, EnumSet.of(JackPortIsInput));
        if (usbSink.length == 2) {
            for (String portname : usbSink) {
                if (portname.contains(" MIDI 1")) {
                    jack.connect(jackclient, drumsOut.getName(), portname);
                    log.debug("Connected Midi Clock");
                }
                else if (portname.contains(" MIDI 2"))
                    jack.connect(jackclient, auxOut1.getName(), portname);
            }
        }
        String [] kompleteMidi = jack.getPorts("Komplete", MIDI, EnumSet.of(JackPortIsInput));
        if (kompleteMidi.length == 1)
            jack.connect(jackclient, auxOut2.getName(), kompleteMidi[0]);
        jack.connect(jackclient, synthOut.getName(), FluidSynth.MIDI_PORT);
    }


    @Override
    public boolean process(JackClient client, int nframes) {
        if (JudahZone.getMasterTrack() == null || JudahZone.getMasterTrack().isOnMute()) return true;
        try {

            scheduler.offer(frame++);
            JudahClock.getInstance().process();
            for (JackPort port : outPorts)
                JackMidi.clearBuffer(port);

            for (JackPort port : inPorts) {
                eventCount = JackMidi.getEventCount(port);
                for (index = 0; index < eventCount; index++) {
                    if (JackMidi.getEventCount(port) != eventCount) {
                        RTLogger.warn(this, "eventCount found " +
                                JackMidi.getEventCount(port) + " expected " + eventCount);
                        return true;
                    }
                    JackMidi.eventGet(midiEvent, port, index);
                    size = midiEvent.size();
                    if (data == null || data.length != size) {
                        data = new byte[size];
                    }

                    midiEvent.read(data);
                    if (port == drumsIn) {
                        clock.processTime(data);
                        continue;
                    }

                    midi = router.process(new Midi(data, port.getShortName()));

                    if (Sequencer.getCurrent() == null) {
                        if (!stage.midiProcessed(midi))
                            write(midi, midiEvent.time());
                    }
                    else if (!Sequencer.getCurrent().midiProcessed(midi)) {
                        if (!stage.midiProcessed(midi))
                            write(midi, midiEvent.time());
                    }

                }
            }

            for (BeatBox b : JudahClock.getInstance().getSequencers()) {
                poll = b.getQueue().poll();
                while (poll != null) {
                    JackMidi.eventWrite(b.getMidiOut(),
                            0, poll.getMessage(), poll.getLength());
                    poll = b.getQueue().poll();
                }
            }

            poll = clock.getQueue().poll();
            while (poll != null) {
                JackMidi.eventWrite(drumsOut, 0, poll.getMessage(), poll.getLength());
                RTLogger.log(this, "to beat buddy: " + poll);
                poll = clock.getQueue().poll();
            }

            poll = queue.poll();
            while (poll != null) {
                write(poll, 0);
                poll = queue.poll();
            }

        } catch (Exception e) {

            RTLogger.warn(this, e);

            if (e.getMessage().equals("ENOBUF"))
                JudahZone.getInstance().recoverMidi(e);

            return false;
        }
        return state.get() == Status.ACTIVE;
    }

    private void write(ShortMessage midi, int time) throws JackException {
        switch (midi.getChannel()) {
        //	case KOMPLETE_CHANNEL:
        //		JackMidi.eventWrite(komplete, time, midi.getMessage(), midi.getLength());
        //		break;
        case DRUMS_CHANNEL: // sending drum notes to external drum machine
            JackMidi.eventWrite(drumsOut, time, midi.getMessage(), midi.getLength());
            break;
        case AUX_CHANNEL:
            JackMidi.eventWrite(auxOut1, time, midi.getMessage(), midi.getLength());
            break;
        default:
            JackMidi.eventWrite(synthOut, time, midi.getMessage(), midi.getLength());
        }
    }

    @Override
    public void queue(ShortMessage message) {
        // log.trace("queued " + message);
        queue.add(message);
    }

    @Override
    public void properties(HashMap<String, Object> props) {
        scheduler.clear();
    }

    public String[] getSources() {
        if (sources != null) return sources;
        String[] sources = new String[inPorts.size() + 1];
        sources[0] = ""; // drop-down option for no source port selected
        for (int i = 0; i < inPorts.size(); i++)
            sources[i + 1] = inPorts.get(i).getShortName();
        return sources;
    }

    public String[] getDestinations() {
        if (destinations != null) return destinations;
        destinations = new String[outPorts.size() + 1];
        destinations[0] = ""; // drop-down option for no destination port selected
        for (int i = 0; i < outPorts.size(); i++)
            destinations[i + 1] = outPorts.get(i).getShortName();
        return destinations;
    }

    public static void queueSynth(Midi msg) {


    }

    public static JackPort getByName(String name) {
        for (JackPort p : instance.outPorts)
            if (p.getShortName().equals(name))
                return p;
        for (JackPort p : instance.inPorts)
            if (p.getShortName().equals(name))
                return p;
        throw new NullPointerException(name);
    }


}

// jack.connect(jackclient, dr5Port.getName(), "a2j:Komplete Audio 6 [20] (playback): Komplete Audio 6 MIDI 1");
//public void disconnectDrums() {
//	try {
//    	for (String port : jack.getPorts(jackclient, drums.getShortName(), MIDI, EnumSet.of(JackPortIsOutput))) {
//    		log.debug("disconnecting " + port + " from " + FluidSynth.MIDI_PORT);
//    		jack.disconnect(jackclient, port, FluidSynth.MIDI_PORT);
//    	}
//	} catch (JackException e) { log.error("disconnecting drums error, " + e.getMessage());}}

