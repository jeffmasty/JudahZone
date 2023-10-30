package net.judah.midi;
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
import net.judah.JudahZone;
import net.judah.api.BasicClient;
import net.judah.api.MidiReceiver;
import net.judah.controllers.*;
import net.judah.gui.MainFrame;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Setup MIDI ports and handle MIDI integration with Jack */
@Getter 
public class JudahMidi extends BasicClient implements Closeable /* , ReRoute */{
	public static final String JACKCLIENT_NAME = "JudahMidi";

	@RequiredArgsConstructor
	public static enum IN { // in Midi ports
		MIDICLOCK("clock24"),
		KEYBOARD("keyboard"), 
		MIXER("mixer"),
		PADS("pads"),
		LINE6_IN("line6In"),
		BEATSTEP("beatstep"),
		JAMSTIK("jamstik");
		@Getter public final String port;
	}

	@RequiredArgsConstructor
	public enum OUT { // out ports
		TEMPO("Tempo"),
		SYNTH_OUT("Fluid"), 
		CRAVE_OUT("Crave");
		@Getter public final String port;
	}
	
	private static Process a2j; // jack to alsa midi bridge
	private final JudahClock clock;
	private ArrayList<JackPort> inPorts = new ArrayList<>();  
    private JackPort midiclock;
    private JackPort keyboard;
    private JackPort pedal;
    private JackPort pads;
    private JackPort mixer;
    private JackPort line6;
    private JackPort beatstep;
    private JackPort gtrMidi;
    private final ArrayList<JackPort> outPorts = new ArrayList<>(); 
    private JackPort fluidOut;
    private JackPort craveOut;
    private JackPort tempo;
    private final ArrayList<MidiReceiver> sync = new ArrayList<>();
    private final MidiScheduler scheduler = new MidiScheduler();
    private final HashMap<JackPort, Controller> switchboard = new HashMap<>();

    private static final ConcurrentLinkedQueue<PortMessage> queue = new ConcurrentLinkedQueue<>();
    private static int ticker;

    private final Jamstik jamstik = new Jamstik();
    private MPKmini mpk;

    private long counter;
    private final byte[] DATA1 = new byte[1], DATA2 = new byte[2], DATA3 = new byte[3]; // for process()
    private final Event midiEvent = new JackMidi.Event(); // for process()

    public JudahMidi(JudahClock clock) throws Exception {
    	this(JACKCLIENT_NAME, clock);
    }
    
    public JudahMidi(String name, JudahClock clock) throws Exception {
        super(name);
        this.clock = clock;
        JudahZone.getServices().add(this);
        a2j();
        start();
    }

    @Override public void close() {
    	super.close();
    	if (a2j != null) a2j.destroy();
    }

    private void a2j() {
		String shellCommand = "a2jmidid -e";
		try {
			if (a2j != null) {
				a2j.destroy();
				Constants.sleep(50);
			}
			
			a2j = Runtime.getRuntime().exec(shellCommand);
			Constants.sleep(333);
		} catch (Exception e) { System.err.println(e.getMessage()); }
	}
    
    //when external a2jmidi bridge fails
    public void recoverMidi() {
    	boolean running = clock.isActive();
    	JudahZone.setInitialized(false);
    	clock.end();
    	try {
			for (JackPort port : outPorts) 
				jackclient.unregisterPort(port);
			for (JackPort port : inPorts) 
				jackclient.unregisterPort(port);
			a2j();
			initialize();
			makeConnections();
			JudahZone.getFluid().setMidiPort(fluidOut);
			JudahZone.getCrave().setMidiPort(craveOut);
			
    	} catch (JackException e) { RTLogger.warn(this, e.getMessage());}
		JudahZone.setInitialized(true);
		if (running)
			clock.begin();
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
        	tempo = outPorts.get(OUT.TEMPO.ordinal());
        if (sz > OUT.SYNTH_OUT.ordinal()) 
        	fluidOut = outPorts.get(OUT.SYNTH_OUT.ordinal());
        if (sz > OUT.CRAVE_OUT.ordinal()) 
        	craveOut = outPorts.get(OUT.CRAVE_OUT.ordinal());

        while (JudahZone.getSeq() == null || JudahZone.getLooper() == null) // sync w/ audio Thread
        	Constants.sleep(10);
        
        // connect midi controllers to software handlers
        sz = inPorts.size();
        if (sz > IN.MIDICLOCK.ordinal()) 
        	midiclock = inPorts.get(IN.MIDICLOCK.ordinal());
        if (sz > IN.KEYBOARD.ordinal()) {
        	keyboard = inPorts.get(IN.KEYBOARD.ordinal());
        	if (mpk == null)
        		mpk = new MPKmini(this, JudahZone.getSeq());
        	switchboard.put(keyboard, mpk);
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
    	
    	// TODO ALSA connect midi clock to MPKMini (arpeggios) and Crave
    	// CLOCK IN
    	for (String port : jack.getPorts(jackclient, "a2j:midiclock", MIDI, OUT)) 
    		connect("MidiClock", midiclock.getName(), port);
    	// CLOCK OUT
    	for (String port : jack.getPorts(jackclient, "a2j:midiclock", MIDI, INS)) 
    		connect("TempoOut", port, tempo.getName());
    	
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
        for (String port : jack.getPorts(jackclient, Constants.getDi(), MIDI, INS)) 
        	connect("Crave", port, craveOut.getName());
        
        String[] fluid = jack.getPorts(jackclient, "midi_00", MIDI, INS);
        while (fluid.length == 0) {
        	Constants.sleep(30);
        	fluid = jack.getPorts(jackclient, "midi_00", MIDI, INS);
        }
    	for (String port : fluid) 
    		connect("Fluid", port, fluidOut.getName());
        for (String port : jack.getPorts(jackclient, "CASIO USB-MIDI", MIDI, OUT)) 
        	connect("Piano", keyboard.getName(), port);
    }

    private void connect(String controller, String inPort, String outPort) throws JackException {
    	RTLogger.log(controller, inPort + " connecting... " + outPort);
    	jack.connect(jackclient, outPort, inPort);
    }
    
    @Override
    public boolean process(JackClient client, int nframes) {
    	if (!JudahZone.isInitialized())
    		return true;
        try {
            scheduler.offer(counter++);
            ticker = 0;
            for (JackPort port : outPorts)
                JackMidi.clearBuffer(port);
            // if (clock.getMode() == Mode.Internal) clock.process();
            
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
                    if (port == midiclock) {
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
    
    public static void queue(MidiMessage msg, JackPort out) {
    	queue.add(new PortMessage(msg, out));
    }

    public void synchronize(MidiReceiver ch) {
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
		for (MidiReceiver p : sync)
			p.send(new Midi(midi), JudahMidi.ticker());
	}

//	public void createPatches(TrackList synths) {
//		mpk = new MidiPatch(synths);
//		jamstik = new Jamstik(synths);
//	}

}

// @Getter private JackPort circuitIn;
// @Getter private JackPort circuitOut;
//        	CircuitTracks.setOut1(calfOut);
//        	CircuitTracks.setOut2(craveOut);
//        if (sz > OUT.CIRCUIT_OUT.ordinal()) {
//        	circuitOut = outPorts.get(OUT.CIRCUIT_OUT.ordinal());
//        	paths.add(new Path(circuitOut, ch.getCircuit()));
//        	if (JudahZone.getChannels().getCircuit() != null)
//        		JudahZone.getChannels().getCircuit().setSync(circuitOut);
//        }
//        if (sz > IN.CIRCUIT_IN.ordinal()) {
//        	circuitIn = inPorts.get(IN.CIRCUIT_IN.ordinal());
//        	switchboard.put(circuitIn, new CircuitTracks());
//        for (String port : jack.getPorts(jackclient, Crave.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
//            RTLogger.log(this, "connecting korg mixer: " + port + " to " + craveIn.getName());
//            jack.connect(jackclient, port, craveIn.getName());
//		String[] usbSource = jack.getPorts(jackclient, "MIDI2x2", MIDI, EnumSet.of(JackPortIsOutput));
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
