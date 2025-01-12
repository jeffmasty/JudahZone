package net.judah.midi;

import static net.judah.JudahZone.*;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.MIDI;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiMessage;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackMidi.Event;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.BasicClient;
import net.judah.api.MidiClock;
import net.judah.api.ZoneMidi;
import net.judah.controllers.Beatstep;
import net.judah.controllers.Controller;
import net.judah.controllers.Jamstik;
import net.judah.controllers.KorgMixer;
import net.judah.controllers.KorgPads;
import net.judah.controllers.Line6FBV;
import net.judah.controllers.MPKmini;
import net.judah.gui.MainFrame;
import net.judah.omni.Threads;
import net.judah.synth.fluid.FluidSynth;
import net.judah.util.Constants;
import net.judah.util.RTLogger;


/** Setup MIDI ports and handle MIDI integration with Jack */
@Getter
public class JudahMidi extends BasicClient implements Closeable {
	public static final String JACKCLIENT_NAME = "JudahMidi";

	/** Hard-coded Line-In MidiPorts */ @RequiredArgsConstructor
	public static enum IN {
		MIDICLOCK("clockIn"),
		KEYBOARD("keyboard"),
		MIXER("mixer"),
		PADS("pads"),
		LINE6_IN("line6In"),
		BEATSTEP("beatstep"),
		JAMSTIK("jamstik");
		public final String port;
	}

	/** Hard-coded Line-Out MidiPorts */ @RequiredArgsConstructor
	public enum OUT {
		TEMPO("clockOut"),
		SYNTH_OUT("Fluid"),
		CRAVE_OUT("Crave");
		public final String port;
	}

    private static int ticker;
	private static Process a2j; // jack to alsa midi bridge

	private final JudahClock clock;
    private JackPort clockIn;
    private JackPort keyboard;
    private JackPort pedal;
    private JackPort pads;
    private JackPort mixer;
    private JackPort line6;
    private JackPort beatstep;
    private final Jamstik jamstik = new Jamstik();
    private JackPort gtrMidi;
    private JackPort fluidOut;
    private JackPort craveOut;
    private JackPort clockOut;
	private final ArrayList<JackPort> inPorts = new ArrayList<>();
    private final ArrayList<JackPort> outPorts = new ArrayList<>();
    private final ArrayList<ZoneMidi> sync = new ArrayList<>();
    private final MidiScheduler scheduler = new MidiScheduler();
    private long counter;
    private final HashMap<JackPort, Controller> switchboard = new HashMap<>();
    private static final ConcurrentLinkedQueue<PortMessage> queue = new ConcurrentLinkedQueue<>();
    // for process()
    private final byte[] DATA1 = new byte[1], DATA2 = new byte[2], DATA3 = new byte[3];
    private final Event midiEvent = new JackMidi.Event();

    public JudahMidi() throws Exception {
    	this(JACKCLIENT_NAME);
    }

    public JudahMidi(String name) throws Exception {
        super(name);
        clock = new JudahClock(this);
        a2j();
        getServices().add(this);
        start();
    }

    @Override public void close() {
    	super.close();
    	if (a2j != null) a2j.destroy();
    }

    @SuppressWarnings("deprecation")
	private void a2j() {
		String shellCommand = "a2jmidid -e";
		try {
			if (a2j != null) {
				a2j.destroy();
				Threads.sleep(50);
			}

			a2j = Runtime.getRuntime().exec(shellCommand);
			Threads.sleep(333);
		} catch (Exception e) { System.err.println(e.getMessage()); }
	}

    //when external a2jmidi bridge fails
    public void recoverMidi() {
    	boolean running = clock.isActive();
    	setInitialized(false);
    	clock.end();
    	try {
			for (JackPort port : outPorts)
				jackclient.unregisterPort(port);
			for (JackPort port : inPorts)
				jackclient.unregisterPort(port);
			a2j();
			initialize();
			makeConnections();
			getFluid().setMidiPort(fluidOut);
			getBass().setMidiPort(craveOut);
			// getMidiGui().recover(this);

    	} catch (JackException e) { RTLogger.warn(this, e.getMessage());}
		setInitialized(true);
		if (running)
			clock.begin();
		RTLogger.log(this, "Midi re-connect");
    }

	@Override
    protected void initialize() throws JackException {
    	switchboard.clear();
    	inPorts.clear();
    	outPorts.clear();
        for (OUT port : OUT.values())
            outPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsOutput));
        for (IN port : IN.values())
            inPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsInput));

        int sz = outPorts.size();
        if (sz > OUT.TEMPO.ordinal())
        	clockOut = outPorts.get(OUT.TEMPO.ordinal());
        if (sz > OUT.SYNTH_OUT.ordinal())
        	fluidOut = outPorts.get(OUT.SYNTH_OUT.ordinal());
        if (sz > OUT.CRAVE_OUT.ordinal())
        	craveOut = outPorts.get(OUT.CRAVE_OUT.ordinal());

        while (getSeq() == null || getLooper() == null) // sync w/ audio Thread
        	Threads.sleep(10);

        // connect midi controllers to software handlers
        sz = inPorts.size();
        if (sz > IN.MIDICLOCK.ordinal())
        	clockIn = inPorts.get(IN.MIDICLOCK.ordinal());
        if (sz > IN.KEYBOARD.ordinal()) {
        	keyboard = inPorts.get(IN.KEYBOARD.ordinal());
        	switchboard.put(keyboard, MPKmini.instance);
        }
        if (sz > IN.MIXER.ordinal()) {
        	mixer = inPorts.get(IN.MIXER.ordinal());
        	KorgMixer controller = new KorgMixer();
        	switchboard.put(mixer, controller);
	        if (sz > IN.PADS.ordinal()) {
	        	pads = inPorts.get(IN.PADS.ordinal());
	        	switchboard.put(pads, new KorgPads());
	        }
        }
        if (sz > IN.LINE6_IN.ordinal()) {
        	line6 = inPorts.get(IN.LINE6_IN.ordinal());
        	switchboard.put(line6, new Line6FBV());
        }
        if (sz > IN.BEATSTEP.ordinal()) {
        	beatstep = inPorts.get(IN.BEATSTEP.ordinal());
        	switchboard.put(beatstep, new Beatstep());
        }
        if (sz > IN.JAMSTIK.ordinal()) {
        	gtrMidi = inPorts.get(IN.JAMSTIK.ordinal());
        	switchboard.put(gtrMidi, jamstik);
        }

        RTLogger.log(this, switchboard.size() + " controllers connected");

        Thread timePolling = new Thread(scheduler);
        timePolling.setPriority(7);
        timePolling.setName(scheduler.getClass().getSimpleName());
        timePolling.start();
    }

    @Override
    public void makeConnections() throws JackException {

    	final EnumSet<JackPortFlags> OUT = EnumSet.of(JackPortIsOutput);
    	final EnumSet<JackPortFlags> INS = EnumSet.of(JackPortIsInput);

    	for (String port : jack.getPorts(jackclient, Beatstep.NAME, MIDI, OUT))
    		connect("Beatstep", beatstep.getName(), port);
        for (String port : jack.getPorts(jackclient, MPKmini.NAME, MIDI, OUT))
        	connect("MPK", keyboard.getName(), port);
        for (String port : jack.getPorts(jackclient, KorgPads.NAME, MIDI, OUT))
        	connect("Pads", pads.getName(), port);
        for (String port : jack.getPorts(jackclient, KorgMixer.NAME, MIDI, OUT))
        	connect("Mixer", mixer.getName(), port);
        for (String port : jack.getPorts(jackclient, Line6FBV.NAME, MIDI, OUT)) {
        	connect("Line6", line6.getName(), port);
        	break;
        }
        for (String port : jack.getPorts(jackclient, Constants.getDi(), MIDI, OUT))
        	connect("DI/Jamstik", gtrMidi.getName(), port);
        for (String port : jack.getPorts(jackclient, Constants.getDi(), MIDI, INS)) {
        	connect("Crave", port, craveOut.getName());
        	connect("BassClock", port, clockOut.getName());
        }

        String[] fluid = jack.getPorts(jackclient, FluidSynth.MIDI_PORT, MIDI, INS);
        while (fluid.length == 0) {
        	Threads.sleep(30);
        	fluid = jack.getPorts(jackclient, FluidSynth.MIDI_PORT, MIDI, INS);
        }
    	for (String port : fluid)
    		connect("Fluid", port, fluidOut.getName());

    	// MPK arpeggio clock
    	for (String port : jack.getPorts(jackclient, "MPKmini2", MIDI, INS))
    		connect("MPKClock", port, clockOut.getName());

    	try {
    	// External CLOCK IN Ticks
    	for (String port : jack.getPorts(jackclient, MidiClock.PORT_NAME, MIDI, OUT))
    		connect("MidiClock", clockIn.getName(), port);
    	} catch (Exception e) { RTLogger.log(this, "No external clock named " + MidiClock.PORT_NAME + "? " + e.getMessage()); }
    }

    private void connect(String controller, String inPort, String outPort) throws JackException {
    	RTLogger.log(controller, "connecting " + inPort + " to " + outPort);
    	jack.connect(jackclient, outPort, inPort);
    }

    @Override
    public boolean process(JackClient client, int nframes) {
    	if (!isInitialized())
    		return true;
        try {
            scheduler.offer(counter++);
            ticker = 0;

            for (JackPort port : outPorts)
                JackMidi.clearBuffer(port);
            // check to send midi_24 clock pulse
            clock.jackFrame();

            // check for incoming midi
        	int eventCount;
        	byte[] data;
        	for (JackPort port : inPorts) {
                eventCount = JackMidi.getEventCount(port);
                for (int index = 0; index < eventCount; index++) {
                    if (JackMidi.getEventCount(port) != eventCount) {
                        RTLogger.warn(this, "eventCount found " +
                                JackMidi.getEventCount(port) + " expected " + eventCount);
                        break;
                    }
                    JackMidi.eventGet(midiEvent, port, index);
                    if (port == clockIn) {
                    	midiEvent.read(DATA1);
                    	clock.processTime(DATA1);
                    	continue;
                    }

                    switch (midiEvent.size()) {
	                    case 2: data = DATA2; break;
	                    case 3: data = DATA3; break;
	                    default: data = new byte[midiEvent.size()];
                    }
                    midiEvent.read(data);
                    switchboard.get(port).midiProcessed(new Midi(data));
                }
            }
        	// check sequencers for output
            PortMessage route = queue.poll();
            while (route != null) {
            		JackMidi.eventWrite(route.getPort(), ticker(),
            				route.getMidi().getMessage(), route.getMidi().getLength());
            	route = queue.poll();
            }
        } catch (Throwable e) {
            RTLogger.warn(this, e);
            if ("ENOBUF".equals(e.getMessage())) ; // a2j?
        }
        return true;
    }

    public static int ticker() {
    	return ++ticker;
    }

    public void queue(MidiMessage msg) {
    	queue(msg, clockOut);
    }

    public static void queue(MidiMessage msg, JackPort out) {
    	queue.add(new PortMessage(msg, out));
    }

    public void synchronize(ZoneMidi ch) {
    	if (sync.contains(ch)) {
    		// TODO send MIDI_RT_STOP to ch
			sync.remove(ch);
    	}
		else
			sync.add(ch);
    	MainFrame.update(ch);
    }

    /** send start/stop to midi listeners */
	public void synchronize(byte[] midi) {
		for (ZoneMidi p : sync)
			p.send(new Midi(midi), JudahMidi.ticker());
	}

}

// old connections
//		for (String portname : usbSource) {
//			if (portname.contains("Midi Out 1"))
//				jack.connect(jackclient, portname, drumsIn.getName());
//			else if (portname.contains("Midi Out 2"))
//				jack.connect(jackclient, portname, arduino.getName());
//        String[] usbSink = jack.getPorts(jackclient, "MIDI2x2", MIDI, EnumSet.of(JackPortIsInput));
//        if (usbSink.length == 2) {
//            for (String portname : usbSink) {
//				/* if (portname.contains(" Midi Out 1")) { jack.connect(jackclient,
//				 * drumsOut.getName(), portname); RTLogger.log(this, "BeatBuddy connected"); } * else */
//            	if (portname.contains(" Midi Out 2")) {
//					jack.connect(jackclient, clockOut.getName(), portname);
//					RTLogger.log(this, "Midi Clock Out connected to midi interface 2");
//        jack.connect(jackclient, fluidOut.getName(), FluidSynth.MIDI_PORT);
//        for (String port : jack.getPorts(jackclient, "CRAVE MIDI 1", MIDI, EnumSet.of(JackPortIsInput))) {
//        	RTLogger.log(this, "connecting CRAVE: " + craveOut.getName() + " to " + port);
//            jack.connect(jackclient, craveOut.getName(), port);
//    if (sz > IN.PEDAL.ordinal()) {
//    	pedal = inPorts.get(IN.PEDAL.ordinal());
//    	switchboard.put(pedal, new MidiPedal());}
//    if (sz > IN.ARDUINO.ordinal()) {
//    	arduino = inPorts.get(IN.ARDUINO.ordinal());
//    	switchboard.put(arduino, new ArduinoPedal());}
