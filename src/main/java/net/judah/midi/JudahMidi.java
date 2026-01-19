package net.judah.midi;

import static judahzone.jnajack.JackHelper.INS;
import static judahzone.jnajack.JackHelper.OUTS;
import static net.judah.JudahZone.isInitialized;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.MIDI;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.midi.MidiMessage;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackMidi.Event;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Controller;
import judahzone.api.Midi;
import judahzone.api.MidiClock;
import judahzone.api.Ports.Type;
import judahzone.jnajack.ZoneJackClient;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import judahzone.util.Services;
import judahzone.util.Threads;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.channel.LineIn;
import net.judah.controllers.Beatstep;
import net.judah.controllers.Jamstik;
import net.judah.controllers.KorgMixer;
import net.judah.controllers.KorgPads;
import net.judah.controllers.Line6FBV;
import net.judah.controllers.MPKmini;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channels.MixBus;
import net.judah.synth.ZoneMidi;
import net.judah.synth.fluid.FluidSynth;


/** Setup MIDI ports, handle MIDI integration with Jack,
 *  route midi from physical controllers */
public class JudahMidi extends ZoneJackClient implements Closeable, MixBus {
	public static final String JACKCLIENT_NAME = "JudahMidi";

	/** Hard-coded Line-In MidiPorts */ @RequiredArgsConstructor
	public static enum MIDI_IN {
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
	public enum MIDI_OUT {
		TEMPO("clocked"),
		FLUID_OUT("Fluid"); // system 取りあえず
		public final String port;
	}

    private static int ticker;
	private static Process a2j; // jack to alsa midi bridge

    protected final CopyOnWriteArrayList<JackPort> inPorts = new CopyOnWriteArrayList<JackPort>();
    protected final CopyOnWriteArrayList<JackPort> outPorts = new CopyOnWriteArrayList<JackPort>();

    private final JudahZone zone;
	private static JudahClock clock;
	public static JudahClock getClock() { return clock; }
	@Getter private final Jamstik jamstik;
    @Getter private JackPort fluidOut;
    private JackPort clockIn;
    private JackPort keyboard;
    private JackPort pads;
    private JackPort mixer;
    private JackPort line6;
    private JackPort beatstep;
    private JackPort gtrMidi;
    @Getter private JackPort clockOut;
    private final ArrayList<ZoneMidi> sync = new ArrayList<>();
    private final MidiScheduler scheduler;
    private long counter;
    private final HashMap<JackPort, Controller> switchboard = new HashMap<>();
    private static final ConcurrentLinkedQueue<PortMessage> queue = new ConcurrentLinkedQueue<>();
    // for process()
    private final byte[] DATA1 = new byte[1], DATA2 = new byte[2], DATA3 = new byte[3];
    private final Event midiEvent = new JackMidi.Event();

    public JudahMidi(JudahZone judahZone) throws Exception {
    	this(judahZone, JACKCLIENT_NAME);
    }

    public JudahMidi(JudahZone judahZone, String name) throws Exception {
        super(name);
        this.zone = judahZone;
        scheduler = new MidiScheduler(zone);
        jamstik = new Jamstik(zone);
        clock = new JudahClock(this, zone);
        a2j();
        Services.add(this);
        zone.getChannels().subscribe(this);
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

	@Override
    protected void initialize() throws Exception {
    	switchboard.clear();
    	inPorts.clear();
    	outPorts.clear();

    	for (MIDI_OUT port : MIDI_OUT.values()) // clock, fluid
            outPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsOutput));

        for (MIDI_IN port : MIDI_IN.values()) // controllers REGISTRATION PHASE II
            inPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsInput));

        int sz = outPorts.size();
        if (sz > MIDI_OUT.TEMPO.ordinal())
        	clockOut = outPorts.get(MIDI_OUT.TEMPO.ordinal());
        if (sz > MIDI_OUT.FLUID_OUT.ordinal())
        	fluidOut = outPorts.get(MIDI_OUT.FLUID_OUT.ordinal());

        while (zone.getSeq() == null || zone.getLooper() == null) // sync w/ audio Thread
        	Threads.sleep(10);

        // connect midi controllers to software handlers
        sz = inPorts.size();
        if (sz > MIDI_IN.MIDICLOCK.ordinal())
        	clockIn = inPorts.get(MIDI_IN.MIDICLOCK.ordinal());
        if (sz > MIDI_IN.KEYBOARD.ordinal()) {
        	keyboard = inPorts.get(MIDI_IN.KEYBOARD.ordinal());
        	switchboard.put(keyboard, zone.getMpkMini());
        }
        if (sz > MIDI_IN.MIXER.ordinal()) {
        	mixer = inPorts.get(MIDI_IN.MIXER.ordinal());
        	KorgMixer controller = new KorgMixer(zone);
        	switchboard.put(mixer, controller);
	        if (sz > MIDI_IN.PADS.ordinal()) {
	        	pads = inPorts.get(MIDI_IN.PADS.ordinal());
	        	switchboard.put(pads, new KorgPads(zone));
	        }
        }
        if (sz > MIDI_IN.LINE6_IN.ordinal()) {
        	line6 = inPorts.get(MIDI_IN.LINE6_IN.ordinal());
        	switchboard.put(line6, new Line6FBV(zone, this));
        }
        if (sz > MIDI_IN.BEATSTEP.ordinal()) {
        	beatstep = inPorts.get(MIDI_IN.BEATSTEP.ordinal());
        	switchboard.put(beatstep, new Beatstep(zone, clock));
        }
        if (sz > MIDI_IN.JAMSTIK.ordinal()) {
        	gtrMidi = inPorts.get(MIDI_IN.JAMSTIK.ordinal());
        	switchboard.put(gtrMidi, jamstik);
        }

        RTLogger.log(this, switchboard.size() + " controllers connected");

        zone.getChannels().initialize(Type.MIDI);

        Thread timePolling = new Thread(scheduler);
        timePolling.setPriority(7);
        timePolling.setName(scheduler.getClass().getSimpleName());
        timePolling.start();
    }

    @Override
    public void makeConnections() throws Exception {

    	// TODO user controllers
    	zone.getChannels().makeConnections(Type.MIDI);

    	for (String port : jack.getPorts(jackclient, Beatstep.NAME, MIDI, OUTS))
    		connect(beatstep.getName(), port);
        for (String port : jack.getPorts(jackclient, MPKmini.NAME, MIDI, OUTS))
        	connect(keyboard.getName(), port);
        for (String port : jack.getPorts(jackclient, KorgPads.NAME, MIDI, OUTS))
        	connect(pads.getName(), port);
        for (String port : jack.getPorts(jackclient, KorgMixer.NAME, MIDI, OUTS))
        	connect(mixer.getName(), port);
        for (String port : jack.getPorts(jackclient, Line6FBV.NAME, MIDI, OUTS)) {
        	connect(line6.getName(), port);
        	break;
        }
        for (String port : jack.getPorts(jackclient, Constants.getDi(), MIDI, OUTS))
        	connect(gtrMidi.getName(), port); // Jamstik Guitar

        // TODO custom
        String[] fluid = jack.getPorts(jackclient, FluidSynth.MIDI_PORT, MIDI, INS);
        while (fluid.length == 0) {
        	Threads.sleep(30);
        	fluid = jack.getPorts(jackclient, FluidSynth.MIDI_PORT, MIDI, INS);
        }
    	for (String port : fluid)
    		connect(port, fluidOut.getName());

    	// MPK arpeggio clock
    	for (String port : jack.getPorts(jackclient, "MPKmini2", MIDI, INS))
    		connect(port, clockOut.getName());

    	try {
    	// External CLOCK IN Ticks
    	for (String port : jack.getPorts(jackclient, MidiClock.PORT_NAME, MIDI, OUTS))
    		connect(clockIn.getName(), port);
    	} catch (Exception e) { RTLogger.log(this, "No external clock named " + MidiClock.PORT_NAME + "? " + e.getMessage()); }
    }

    private void connect(String inPort, String outPort) throws JackException {
    	RTLogger.debug(this, ": connecting " + inPort + " to " + outPort);
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
            clock.pulse();

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
            		JackMidi.eventWrite(route.port(), ticker(),
            				route.midi().getMessage(), route.midi().getLength());
            	route = queue.poll();
            }
            requests.process();
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

	@Override public void channelAdded(LineIn ch) {
		if (ch instanceof MidiInstrument midi && midi.getMidiPort() != null)
			outPorts.add(midi.getMidiPort());
	}

	@Override public void channelRemoved(LineIn ch) {
		if (ch instanceof MidiInstrument midi)
			outPorts.remove(midi.getMidiPort());
	}

	@Override public void reordered() { /* no-op */ }
	@Override public void update(LineIn ch) { /* no-op */ }

}

// old connections
//private JackPort pedal;
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
////when external a2jmidi bridge fails
//public void recoverMidi() {
//	boolean running = clock.isActive();
//	setInitialized(false);
//	clock.end();
//	try {
//		for (JackPort port : outPorts)
//			jackclient.unregisterPort(port);
//		for (JackPort port : inPorts)
//			jackclient.unregisterPort(port);
//		a2j();
//		initialize();
//		makeConnections();
//		zone.getFluid().setMidiPort(fluidOut);
//
//		// zone.getBass().setMidiPort(craveOut);
//		// getMidiGui().recover(this);
//
//	} catch (Exception e) { RTLogger.warn(this, e.getMessage());}
//	setInitialized(true);
//	if (running)
//		clock.begin();
//	RTLogger.log(this, "Midi re-connect");
//}

