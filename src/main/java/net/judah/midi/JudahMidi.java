package net.judah.midi;
import static net.judah.settings.MidiSetup.DRUMS_CHANNEL;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.MIDI;

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
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.BasicClient;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.api.PortMessage;
import net.judah.api.Service;
import net.judah.beatbox.BeatBox;
import net.judah.clock.BeatBuddy;
import net.judah.clock.JudahClock;
import net.judah.controllers.ArduinoPedal;
import net.judah.controllers.Controller;
import net.judah.controllers.Crave;
import net.judah.controllers.KorgMixer;
import net.judah.controllers.KorgPads;
import net.judah.controllers.MPK;
import net.judah.controllers.MidiPedal;
import net.judah.fluid.FluidSynth;
import net.judah.sequencer.Sequencer;
import net.judah.settings.MidiSetup.IN;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Console;
import net.judah.util.RTLogger;

/** handle MIDI integration with Jack */
public class JudahMidi extends BasicClient implements Service, MidiQueue {

    public static final String JACKCLIENT_NAME = "JudahMidi";
    private long frame = 0;
    private String[] sources, destinations; // for GUI

    @Getter private static JudahMidi instance;
   
    /** current Jack Frame */
    public static long getCurrent() {
        return instance.scheduler.getCurrent();
    }
    
    private JudahClock clock;
    
    @Getter private ArrayList<JackPort> inPorts = new ArrayList<>();  // Keyboard, Pedal, MidiIn
    @Getter private JackPort keyboard;
    @Getter private JackPort pedal;
    @Getter private JackPort drumsIn;
    @Getter private JackPort pads;
    @Getter private JackPort mixer;
    @Getter private JackPort auxIn;
    @Getter private JackPort arduino;
    @Getter private JackPort craveIn;

    @Getter private ArrayList<JackPort> outPorts = new ArrayList<>(); // Synth, Effects, MidiOut
    @Setter @Getter private JackPort keyboardSynth;
    @Getter private JackPort synthOut;
    @Getter private JackPort drumsOut;
    @Getter private JackPort auxOut1;
    @Getter private JackPort craveOut;
    @Getter private JackPort clockOut;
    // @Getter private JackPort auxOut2;
    @Getter private JackPort calfOut;
    @Getter private final String[] NAMES;
    /** name -> midiChannel */
    @Getter private final HashMap<String, Integer> midiChannelNames = new HashMap<>();
    /** midiChannel -> defaultJackPort */
    @Getter private final HashMap<Integer, JackPort> defaultMidiChannelOut = new HashMap<>();

    @Getter private final MidiScheduler scheduler = new MidiScheduler(this);
    @Getter private final Router router = new Router();
    @Getter private static final ConcurrentLinkedQueue<PortMessage> queue = new ConcurrentLinkedQueue<>();

    @Getter MidiCommands commands = new MidiCommands(this);
    private final HashMap<JackPort, Controller> switchboard = new HashMap<>();
    private final Event midiEvent = new JackMidi.Event();

    public JudahMidi(String name) throws JackException {
        super(name);
        instance = this;
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
        midiChannelNames.put("Keys", 0);
        defaultMidiChannelOut.put(0, synthOut);

        midiChannelNames.put("bass", 1);
        defaultMidiChannelOut.put(1, synthOut);

        midiChannelNames.put("lead", 2);
        defaultMidiChannelOut.put(2, synthOut);

        midiChannelNames.put("chord", 3);
        defaultMidiChannelOut.put(3, synthOut);

        midiChannelNames.put("NOTE5", 4);
        defaultMidiChannelOut.put(4, calfOut);

        midiChannelNames.put("NOTE6", 5);
        defaultMidiChannelOut.put(5, calfOut);

        midiChannelNames.put("NOTE7", 6);
        defaultMidiChannelOut.put(6, auxOut1);

        midiChannelNames.put("NOTE8", 7);
        defaultMidiChannelOut.put(7, synthOut);

        // Drum channels
        midiChannelNames.put("BUDDY", 8);
        defaultMidiChannelOut.put(8, drumsOut);
        midiChannelNames.put("SDRUM", 9);
        defaultMidiChannelOut.put(9, synthOut);
        midiChannelNames.put("DRUM2", 10);
        defaultMidiChannelOut.put(10, calfOut);
        midiChannelNames.put("DRUMS", 11);
        defaultMidiChannelOut.put(11, calfOut);

        // Chords channels
        midiChannelNames.put("CHORD", 12);
        defaultMidiChannelOut.put(12, synthOut);
        midiChannelNames.put("CHRD2", 13);
        defaultMidiChannelOut.put(13, synthOut);
        midiChannelNames.put("CHRD3", 14);
        defaultMidiChannelOut.put(14, calfOut);
        midiChannelNames.put("CHRD4", 15);
        defaultMidiChannelOut.put(15, auxOut1);
        return result;
    }

    @Override
    protected void initialize() throws JackException {

    	// init getters for out ports
        for (OUT port : OUT.values())
            outPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsOutput));
        int sz = outPorts.size();
        if (sz > OUT.SYNTH_OUT.ordinal()) synthOut = outPorts.get(OUT.SYNTH_OUT.ordinal());
        if (sz > OUT.DRUMS_OUT.ordinal()) drumsOut = outPorts.get(OUT.DRUMS_OUT.ordinal());
        if (sz > OUT.CALF_OUT.ordinal()) calfOut = outPorts.get(OUT.CALF_OUT.ordinal());
        if (sz > OUT.CRAVE_OUT.ordinal()) craveOut = outPorts.get(OUT.CRAVE_OUT.ordinal());
        if (sz > OUT.AUX1_OUT.ordinal()) auxOut1 = outPorts.get(OUT.AUX1_OUT.ordinal());
        if (sz > OUT.CLOCK_OUT.ordinal()) clockOut = outPorts.get(OUT.CLOCK_OUT.ordinal());
        keyboardSynth = craveOut;
        

        // connect midi controllers to software handlers
        for (IN port : IN.values())
            inPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsInput));
        sz = inPorts.size();
        if (sz > IN.KEYBOARD.ordinal()) {
        	keyboard = inPorts.get(IN.KEYBOARD.ordinal());
        	switchboard.put(keyboard, new MPK());
        }
        if (sz > IN.PEDAL.ordinal()) {
        	pedal = inPorts.get(IN.PEDAL.ordinal());
        	switchboard.put(pedal, new MidiPedal());
        }
        if (sz > IN.MIXER.ordinal()) {
        	mixer = inPorts.get(IN.MIXER.ordinal());
        	KorgMixer controller = new KorgMixer();
        	switchboard.put(mixer, controller);
	        if (sz > IN.PADS.ordinal()) {
	        	pads = inPorts.get(IN.PADS.ordinal());
	        	switchboard.put(pads, new KorgPads(controller, JudahZone.getLooper()));
	        }
        }
        if (sz > IN.ARDUINO.ordinal()) {
        	arduino = inPorts.get(IN.ARDUINO.ordinal());
        	switchboard.put(arduino, new ArduinoPedal());
        }
        if (sz > IN.CRAVE_IN.ordinal()) {
        	craveIn = inPorts.get(IN.CRAVE_IN.ordinal());
        	switchboard.put(craveIn, new Crave());
        }
        
        if (sz > IN.DRUMS_IN.ordinal()) {
        	drumsIn = inPorts.get(IN.DRUMS_IN.ordinal());
        	// TODO JudahClock as software controller?
        }
        if (sz > IN.AUX_IN.ordinal()) {
        	auxIn = inPorts.get(IN.AUX_IN.ordinal());
        }
        if (switchboard.isEmpty()) Console.warn(new NullPointerException("JudahMidi: no controllers connected"));
        
        Thread timePolling = new Thread(scheduler);
        timePolling.setPriority(8);
        timePolling.setName(scheduler.getClass().getSimpleName());
        timePolling.start();
        
        clock = JudahClock.getInstance();
        clock.setClockOut(clockOut);
        
    }

    @Override
    protected void makeConnections() throws JackException {

        for (String port : jack.getPorts(jackclient, MidiPedal.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
            RTLogger.log(this, "connecting foot pedal: " + port + " to " + pedal.getName());
            jack.connect(jackclient, port, pedal.getName());
        }
        for (String port : jack.getPorts(jackclient, MPK.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
            RTLogger.log(this, "connecting keyboard: " + port + " to " + keyboard.getName());
            jack.connect(jackclient, port, keyboard.getName());
        }
        for (String port : jack.getPorts(jackclient, KorgPads.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
            RTLogger.log(this, "connecting korg pads: " + port + " to " + pads.getName());
            jack.connect(jackclient, port, pads.getName());
        }
        for (String port : jack.getPorts(jackclient, KorgMixer.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
            RTLogger.log(this, "connecting korg mixer: " + port + " to " + mixer.getName());
            jack.connect(jackclient, port, mixer.getName());
        }
        for (String port : jack.getPorts(jackclient, Crave.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
            RTLogger.log(this, "connecting korg mixer: " + port + " to " + craveIn.getName());
            jack.connect(jackclient, port, craveIn.getName());
        }
        
		String[] usbSource = jack.getPorts(jackclient, "MIDI2x2", MIDI, EnumSet.of(JackPortIsOutput));
		for (String portname : usbSource) {
			if (portname.contains("Midi Out 1"))
				jack.connect(jackclient, portname, drumsIn.getName());
			else if (portname.contains("Midi Out 2"))
				jack.connect(jackclient, portname, arduino.getName());
		}
        
        String[] usbSink = jack.getPorts(jackclient, "MIDI2x2", MIDI, EnumSet.of(JackPortIsInput));
        if (usbSink.length == 2) {
            for (String portname : usbSink) {
                if (portname.contains(" Midi Out 1")) {
                    jack.connect(jackclient, drumsOut.getName(), portname);
                    RTLogger.log(this, "BeatBuddy connected");
                }
				else if (portname.contains(" Midi Out 2")) {
					jack.connect(jackclient, clockOut.getName(), portname);
					RTLogger.log(this, "Midi Clock Out connected");
				}
            }
            
        }
        jack.connect(jackclient, synthOut.getName(), FluidSynth.MIDI_PORT);
        
        for (String port : jack.getPorts(jackclient, "CRAVE MIDI 1", MIDI, EnumSet.of(JackPortIsInput))) {
        	RTLogger.log(this, "connecting CRAVE: " + craveOut.getName() + " to " + port);
            jack.connect(jackclient, craveOut.getName(), port);
        }

        //        String [] kompleteMidi = jack.getPorts("Komplete", MIDI, EnumSet.of(JackPortIsInput));
		//        if (kompleteMidi.length == 1)
		//            jack.connect(jackclient, auxOut2.getName(), kompleteMidi[0]);

    }
    
//    private byte[] data = null;
    private final byte[] DATA1 = new byte[1], DATA2 = new byte[2], DATA3 = new byte[3];
//    private int index, eventCount;
//    private ShortMessage poll;
//    private PortMessage route;
//    private Midi midi;
	    
    @Override
    public boolean process(JackClient client, int nframes) {
        if (JudahZone.getMasterTrack() == null || JudahZone.getMasterTrack().isOnMute()) return true;

        
        try {

            scheduler.offer(frame++);
            for (JackPort port : outPorts)
                JackMidi.clearBuffer(port);
            clock.process();
            
            ShortMessage poll = BeatBuddy.getQueue().poll();
        	while (poll != null) {
        		JackMidi.eventWrite(drumsOut, 0, poll.getMessage(), poll.getLength());
        		// RTLogger.log(this, "to beat buddy: " + poll);
        		poll = BeatBuddy.getQueue().poll();
        	}

        	// check for incoming midi
        	int eventCount;
        	Midi midi;
            for (JackPort port : inPorts) {
                eventCount = JackMidi.getEventCount(port);
                for (int index = 0; index < eventCount; index++) {
                    if (JackMidi.getEventCount(port) != eventCount) {
                        RTLogger.warn(this, "eventCount found " +
                                JackMidi.getEventCount(port) + " expected " + eventCount);
                        return true;
                    }
                    JackMidi.eventGet(midiEvent, port, index);
                    byte[] data;
                    switch (midiEvent.size()) {
	                    case 1: data = DATA1; break;
	                    case 2: data = DATA2; break;
	                    case 3: data = DATA3; break;
	                    default: data = new byte[midiEvent.size()];
                    }

                    midiEvent.read(data);
                    if (port == drumsIn) {
                    	clock.processTime(data);
                        continue;
                    }

                    midi = router.process(new Midi(data, port.getShortName()));

                    if (Sequencer.getCurrent() == null) {
                    	if (switchboard.get(port) != null) {
                    		if (switchboard.get(port).midiProcessed(midi))
                    			MainFrame.updateCurrent();
                    		else 
                    			write(midi, midiEvent.time());
                    	}
                    }
                    else if (Sequencer.getCurrent().midiProcessed(midi, port)) 
                    	MainFrame.updateTime();
                }
            }

            // check sequencers for output
            for (BeatBox b : clock.getSequencers()) {
                poll = b.getQueue().poll();
                while (poll != null) {
                    JackMidi.eventWrite(b.getMidiOut(),
                            0, poll.getMessage(), poll.getLength());
                	poll = b.getQueue().poll();
                }
            }

            PortMessage route = queue.poll();
            while(route != null) {
            	if (route.getPort() == null) 
            		write(route.getMidi(), 0);
            	else 
            		JackMidi.eventWrite(route.getPort(), 
            				0, route.getMidi().getMessage(), route.getMidi().getLength());
            	route = queue.poll();
            }

        } catch (Throwable e) {

            RTLogger.warn(this, e);

            if ("ENOBUF".equals(e.getMessage())) {
                JudahZone.getInstance().recoverMidi();
                return false;
            }
        }
        return true;
    }

    private void write(ShortMessage midi, int time) throws JackException {
        switch (midi.getChannel()) {
	        case DRUMS_CHANNEL: // sending drum notes to external drum machine
	            JackMidi.eventWrite(clock.getSequencer(9).getMidiOut(), time, midi.getMessage(), midi.getLength());
	            break;
			//	case AUX_CHANNEL:
			//	    JackMidi.eventWrite(auxOut1, time, midi.getMessage(), midi.getLength());
			//	    break;
	        default: JackMidi.eventWrite(keyboardSynth, time, midi.getMessage(), midi.getLength());
        }
    }

    public static void queue(ShortMessage midi, JackPort midiOut) {
    	queue.add(new PortMessage(midi, midiOut));
    }
    
    @Override // TODO Deprecated
    public void queue(ShortMessage message) {
        queue.add(new PortMessage(message, null));
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

    public static JackPort getByName(String name) {
        for (JackPort p : instance.outPorts)
            if (p.getShortName().equals(name))
                return p;
        for (JackPort p : instance.inPorts)
            if (p.getShortName().equals(name))
                return p;
        RTLogger.warn(instance, name + " midi port not found, using default out");
        return instance.outPorts.get(0);
    }


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

//	case KOMPLETE_CHANNEL:
//		JackMidi.eventWrite(komplete, time, midi.getMessage(), midi.getLength());
//		break;

// jack.connect(jackclient, dr5Port.getName(), "a2j:Komplete Audio 6 [20] (playback): Komplete Audio 6 MIDI 1");
//public void disconnectDrums() {
//	try {
//    	for (String port : jack.getPorts(jackclient, drums.getShortName(), MIDI, EnumSet.of(JackPortIsOutput))) {
//    		RTLogger.log(this, "disconnecting " + port + " from " + FluidSynth.MIDI_PORT);
//    		jack.disconnect(jackclient, port, FluidSynth.MIDI_PORT);
//    	}
//	} catch (JackException e) { log.error("disconnecting drums error, " + e.getMessage());}}

