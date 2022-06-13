package net.judah.midi;
import static net.judah.settings.MidiSetup.DRUMS_CHANNEL;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
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
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.BasicClient;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.api.PortMessage;
import net.judah.api.Service;
import net.judah.beatbox.BeatBox;
import net.judah.clock.JudahClock;
import net.judah.clock.JudahClock.Mode;
import net.judah.controllers.*;
import net.judah.sequencer.Sequencer;
import net.judah.settings.MidiSetup.IN;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Console;
import net.judah.util.RTLogger;

/** handle MIDI integration with Jack */
public class JudahMidi extends BasicClient implements Service, MidiQueue {

    @Getter private static JudahMidi instance;
	public static final String JACKCLIENT_NAME = "JudahMidi";
    @Getter private static final Router router = new Router();
    @Getter private static final ConcurrentLinkedQueue<PortMessage> queue = new ConcurrentLinkedQueue<>();

	private long frame = 0;
    private String[] sources, destinations; // for GUI
    private final byte[] DATA1 = new byte[1], DATA2 = new byte[2], DATA3 = new byte[3]; // for process()
    private final Event midiEvent = new JackMidi.Event(); // for process()

    private JudahClock clock;
    @Getter private ArrayList<JackPort> inPorts = new ArrayList<>();  // Keyboard, Pedal, MidiIn
    @Getter private JackPort keyboard;
    @Getter private JackPort pedal;
    @Getter private JackPort circuitIn;
    @Getter private JackPort pads;
    @Getter private JackPort mixer;
    @Getter private JackPort auxIn;
    @Getter private JackPort arduino;
    @Getter private JackPort line6;
    //@Getter private JackPort jamstikIn;
    //@Getter private JackPort craveIn;
    
    @Getter private final ArrayList<JackPort> outPorts = new ArrayList<>(); 
    @Getter private JackPort fluidOut;
    @Getter private JackPort auxOut1;
    @Getter private JackPort unoOut;
    @Getter private JackPort circuitOut;
    @Getter private JackPort craveOut;
    @Getter private JackPort clockOut;
    @Getter private JackPort calfOut;
    @Getter private final ArrayList<JackPort> sync = new ArrayList<>();
    
    private JackPort keyboardSynth;

    
    @Getter private final MidiScheduler scheduler = new MidiScheduler(this);
    
    @Getter MidiCommands commands = new MidiCommands(this);
    private final HashMap<JackPort, Controller> switchboard = new HashMap<>();

    public JudahMidi(String name) throws JackException {
        super(name);
        instance = this;
        start();
    }

    /** current Jack Frame */
    public static long getCurrent() {
        return instance.scheduler.getCurrent();
    }

    @Override
    protected void initialize() throws JackException {

    	// init getters for out ports
        for (OUT port : OUT.values())
            outPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsOutput));
        int sz = outPorts.size();
        if (sz > OUT.CLOCK_OUT.ordinal()) clockOut = outPorts.get(OUT.CLOCK_OUT.ordinal());
        if (sz > OUT.SYNTH_OUT.ordinal()) {
        	fluidOut = outPorts.get(OUT.SYNTH_OUT.ordinal());
        	keyboardSynth = fluidOut;
        }
        if (sz > OUT.CALF_OUT.ordinal()) calfOut = outPorts.get(OUT.CALF_OUT.ordinal());
        if (sz > OUT.CRAVE_OUT.ordinal()) {
        	craveOut = outPorts.get(OUT.CRAVE_OUT.ordinal());
        	JudahZone.getChannels().getCrave().setSync(craveOut);
        }
        if (sz > OUT.UNO_OUT.ordinal()) {
        	unoOut = outPorts.get(OUT.UNO_OUT.ordinal());
        	JudahZone.getChannels().getUno().setSync(unoOut);
        }
        if (sz > OUT.CIRCUIT_OUT.ordinal()) circuitOut = outPorts.get(OUT.CIRCUIT_OUT.ordinal());
        if (sz > OUT.AUX1_OUT.ordinal()) auxOut1 = outPorts.get(OUT.AUX1_OUT.ordinal());

        // connect midi controllers to software handlers
        for (IN port : IN.values())
            inPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsInput));
        sz = inPorts.size();
        if (sz > IN.KEYBOARD.ordinal()) {
        	keyboard = inPorts.get(IN.KEYBOARD.ordinal());
        	switchboard.put(keyboard, new MPK());
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
        
        if (sz > IN.CIRCUIT_IN.ordinal()) {
        	circuitIn = inPorts.get(IN.CIRCUIT_IN.ordinal());
        	switchboard.put(circuitIn, new CircuitTracks());
        }
        if (sz > IN.LINE6_IN.ordinal()) {
        	line6 = inPorts.get(IN.LINE6_IN.ordinal());
        	switchboard.put(line6, new Line6FBV());
        }
		//    if (sz > IN.JAMSTIK_IN.ordinal()) {
		//    	jamstikIn = inPorts.get(IN.JAMSTIK_IN.ordinal());
		//    	switchboard.put(jamstikIn, new Jamstik());
		//    }
        if (sz > IN.AUX_IN.ordinal()) {
        	auxIn = inPorts.get(IN.AUX_IN.ordinal());
        }
        if (switchboard.isEmpty()) Console.warn(new NullPointerException("JudahMidi: no controllers connected"));
        
        Thread timePolling = new Thread(scheduler);
        timePolling.setPriority(7);
        timePolling.setName(scheduler.getClass().getSimpleName());
        timePolling.start();
        
        clock = JudahClock.getInstance();
        clock.setClockOut(clockOut);

		//    if (sz > IN.CRAVE_IN.ordinal()) {
		//    	craveIn = inPorts.get(IN.CRAVE_IN.ordinal());
		//    	switchboard.put(craveIn, new Crave());}
		//    if (sz > IN.PEDAL.ordinal()) {
		//    	pedal = inPorts.get(IN.PEDAL.ordinal());
		//    	switchboard.put(pedal, new MidiPedal());}
		//    if (sz > IN.ARDUINO.ordinal()) {
		//    	arduino = inPorts.get(IN.ARDUINO.ordinal());
		//    	switchboard.put(arduino, new ArduinoPedal());}

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
//        for (String port : jack.getPorts(jackclient, Crave.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
//            RTLogger.log(this, "connecting korg mixer: " + port + " to " + craveIn.getName());
//            jack.connect(jackclient, port, craveIn.getName());
//        }
        
//		String[] usbSource = jack.getPorts(jackclient, "MIDI2x2", MIDI, EnumSet.of(JackPortIsOutput));
//		for (String portname : usbSource) {
//			if (portname.contains("Midi Out 1"))
//				jack.connect(jackclient, portname, drumsIn.getName());
//			else if (portname.contains("Midi Out 2"))
//				jack.connect(jackclient, portname, arduino.getName());
//		}
        
//        String[] usbSink = jack.getPorts(jackclient, "MIDI2x2", MIDI, EnumSet.of(JackPortIsInput));
//        if (usbSink.length == 2) {
//            for (String portname : usbSink) {
//				/* if (portname.contains(" Midi Out 1")) { jack.connect(jackclient,
//				 * drumsOut.getName(), portname); RTLogger.log(this, "BeatBuddy connected"); }
//				 * else */
//            	if (portname.contains(" Midi Out 2")) {
//					jack.connect(jackclient, clockOut.getName(), portname);
//					RTLogger.log(this, "Midi Clock Out connected to midi interface 2");
//				}
//            }
//            
//        }
//        jack.connect(jackclient, fluidOut.getName(), FluidSynth.MIDI_PORT);
        
//        for (String port : jack.getPorts(jackclient, "CRAVE MIDI 1", MIDI, EnumSet.of(JackPortIsInput))) {
//        	RTLogger.log(this, "connecting CRAVE: " + craveOut.getName() + " to " + port);
//            jack.connect(jackclient, craveOut.getName(), port);
//        }

    }
    
    @Override
    public boolean process(JackClient client, int nframes) {
        if (JudahZone.getMasterTrack() == null || JudahZone.getMasterTrack().isOnMute()) return true;
        try {

            scheduler.offer(frame++);
            for (JackPort port : outPorts)
                JackMidi.clearBuffer(port);
            if (JudahClock.getMode() == Mode.Internal)
            	clock.process();
            
        	// check for incoming midi
        	int eventCount;
        	Midi midi;
        	byte[] data;
        	for (JackPort port : inPorts) {
                eventCount = JackMidi.getEventCount(port);
                for (int index = 0; index < eventCount; index++) {
                    if (JackMidi.getEventCount(port) != eventCount) {
                        RTLogger.warn(this, "eventCount found " +
                                JackMidi.getEventCount(port) + " expected " + eventCount);
                        continue;
                    }
                    JackMidi.eventGet(midiEvent, port, index);
                    if (midiEvent.size() == 1) { // if (port == circuitIn)
                    	if (JudahClock.getMode() == Mode.Midi24) {
                    		midiEvent.read(DATA1);
                    		clock.processTime(DATA1);
                    	}
                    	continue;
                    }
                    
                    switch (midiEvent.size()) {
	                    case 2: data = DATA2; break;
	                    case 3: data = DATA3; break;
	                    default: data = new byte[midiEvent.size()];
                    }
                    midiEvent.read(data);
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
                    	MainFrame.updateTime(); // ?
                }
            }

            // check sequencers for output
        	ShortMessage poll;
        	int offset = 0;
            for (BeatBox b : clock.getSequencers()) {
                poll = b.getQueue().poll();
                while (poll != null) {
                    JackMidi.eventWrite(b.getMidiOut(),
                            offset++, poll.getMessage(), poll.getLength());
                	poll = b.getQueue().poll();
                }
            }

            PortMessage route = queue.poll();
            while(route != null) {
            	if (route.getPort() == null) 
            		write(route.getMidi(), offset++);
            	else 
            		JackMidi.eventWrite(route.getPort(), 
            				offset++, route.getMidi().getMessage(), route.getMidi().getLength());
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

    public void nextKeyboardSynth() {
    	JackPort old = keyboardSynth;
    	if (fluidOut == keyboardSynth) {
    		keyboardSynth = craveOut;
    		clock.getGui().keyboardSynth("Crave");
    	}
    	else if (craveOut == keyboardSynth) {
    		keyboardSynth = unoOut;
    		clock.getGui().keyboardSynth("Uno");
    	}
    	else {
    		keyboardSynth = fluidOut;
    		clock.getGui().keyboardSynth("Fluid");
    	}
    	if (old != null)
    		new Panic(old).start();
    }

	public static void queue(ShortMessage midi, JackPort midiOut) {
    	queue.add(new PortMessage(midi, midiOut));
    }
    
    @Override @Deprecated 
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

	public static void synchronize(byte[] midi) {
		try {
			for (JackPort p : instance.sync)
				JackMidi.eventWrite(p, 0, midi, midi.length);
		} catch (JackException e) {
			RTLogger.warn("MIDI_SYNC", e);
		}
	}

}

    // To be revived
    //public void routeChannel(HashMap<String, Object> props) {
    //    try {
    //        boolean active = Boolean.parseBoolean("" + props.get("Active"));
    //        int from = Integer.parseInt("" + props.get("from"));
    //        int to = Integer.parseInt("" + props.get("to"));
    //        Route route = new Route(from, to);
    //        if (active) getRouter().add(route);
    //        else  getRouter().remove(route);
    //    } catch (NumberFormatException e) {
    //        log.error(e.getMessage() + " " + Constants.prettyPrint(props));
    //    }}


