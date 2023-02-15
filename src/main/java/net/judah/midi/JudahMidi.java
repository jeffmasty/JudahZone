package net.judah.midi;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.MIDI;

import java.io.Closeable;
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
import org.jaudiolibs.jnajack.JackPortFlags;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.api.BasicClient;
import net.judah.api.MidiReceiver;
import net.judah.controllers.*;
import net.judah.gui.MainFrame;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Setup MIDI ports and handle MIDI integration with Jack */
@Getter 
public class JudahMidi extends BasicClient implements Closeable {
	public static final String JACKCLIENT_NAME = "JudahMidi";

	@RequiredArgsConstructor
	public static enum IN { // in Midi ports
		MIDICLOCK("midiclock"),
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
	
	private final JudahClock clock;
    @Getter private MidiTrack keyboardSynth;
	private ArrayList<JackPort> inPorts = new ArrayList<>();  
    private JackPort midiclock;
    private JackPort keyboard;
    private JackPort pedal;
    private JackPort pads;
    private JackPort mixer;
    private JackPort line6;
    private JackPort beatstep;
    private JackPort jamstik;
    private final ArrayList<JackPort> outPorts = new ArrayList<>(); 
    private JackPort fluidOut;
    private JackPort craveOut;
    private JackPort tempo;
    private final ArrayList<MidiPort> sync = new ArrayList<>();
    private final MidiScheduler scheduler = new MidiScheduler();
    private final HashMap<JackPort, Controller> switchboard = new HashMap<>();
    private static final ConcurrentLinkedQueue<PortMessage> queue = new ConcurrentLinkedQueue<>();
    private static int ticker;
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
        start();
    }

	@Override
    protected void initialize() throws JackException {
    	switchboard.clear();
    	outPorts.clear();
    	inPorts.clear();
        for (OUT port : OUT.values())
            outPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsOutput));
        
        int sz = outPorts.size();
        if (sz > OUT.TEMPO.ordinal()) {
        	tempo = outPorts.get(OUT.TEMPO.ordinal());
        }
        if (sz > OUT.SYNTH_OUT.ordinal()) {
        	fluidOut = outPorts.get(OUT.SYNTH_OUT.ordinal());
        }
        if (sz > OUT.CRAVE_OUT.ordinal()) {
        	craveOut = outPorts.get(OUT.CRAVE_OUT.ordinal());
        }

        // connect midi controllers to software handlers
        for (IN port : IN.values())
            inPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsInput));
        sz = inPorts.size();
        if (sz > IN.MIDICLOCK.ordinal()) {
        	midiclock = inPorts.get(IN.MIDICLOCK.ordinal());
        }
        
        while (JudahZone.getSeq() == null) // sync w/ audio Thread
        	Constants.sleep(10);
        if (sz > IN.KEYBOARD.ordinal()) {
        	keyboard = inPorts.get(IN.KEYBOARD.ordinal());
        	switchboard.put(keyboard, new MPKmini(this, JudahZone.getSeq()));
        }
        if (sz > IN.MIXER.ordinal()) {
        	mixer = inPorts.get(IN.MIXER.ordinal());
        	KorgMixer controller = new KorgMixer();
        	switchboard.put(mixer, controller);
	        if (sz > IN.PADS.ordinal()) {
	        	pads = inPorts.get(IN.PADS.ordinal());
	        	switchboard.put(pads, new KorgPads(controller));
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
        	jamstik = inPorts.get(IN.JAMSTIK.ordinal());
        }
        
        if (switchboard.isEmpty()) 
        	RTLogger.warn(this, new NullPointerException("JudahMidi: no controllers connected"));

        Thread timePolling = new Thread(scheduler);
        timePolling.setPriority(7);
        timePolling.setName(scheduler.getClass().getSimpleName());
        timePolling.start();
    }

    @Override
    protected void makeConnections() throws JackException {
    	
    	final EnumSet<JackPortFlags> OUT = EnumSet.of(JackPortIsOutput);
    	final EnumSet<JackPortFlags> INS = EnumSet.of(JackPortIsInput);
    	
    	for (String port : jack.getPorts(jackclient, Beatstep.NAME, MIDI, OUT)) {
            RTLogger.log(this, "connecting beatstep: " + port + " to " + beatstep.getName());
    		jack.connect(jackclient, port, beatstep.getName());
    	}
        for (String port : jack.getPorts(jackclient, MPKmini.NAME, MIDI, OUT)) {
            RTLogger.log(this, "connecting MPK: " + port + " to " + keyboard.getName());
            jack.connect(jackclient, port, keyboard.getName());
        }
        for (String port : jack.getPorts(jackclient, "CASIO USB-MIDI", MIDI, OUT)) {
            RTLogger.log(this, "connecting piano: " + port + " to " + keyboard.getName());
        	jack.connect(jackclient, port, keyboard.getName());
        }
        for (String port : jack.getPorts(jackclient, KorgPads.NAME, MIDI, OUT)) {
            RTLogger.log(this, "connecting korg pads: " + port + " to " + pads.getName());
            jack.connect(jackclient, port, pads.getName());
        }
        for (String port : jack.getPorts(jackclient, KorgMixer.NAME, MIDI, OUT)) {
            RTLogger.log(this, "connecting korg mixer: " + port + " to " + mixer.getName());
            jack.connect(jackclient, port, mixer.getName());
        }
        for (String port : jack.getPorts(jackclient, Line6FBV.NAME, MIDI, OUT)) {
    		RTLogger.log(this, "connecting pedal board: " + port + " to " + line6.getName());
    		jack.connect(jackclient, port, line6.getName());
    		break;
        }
        for (String port : jack.getPorts(jackclient, Constants.getDi(), MIDI, OUT)) {
        	RTLogger.log(this, "connecting jamstik through DI: " + port);
        	jack.connect(jackclient, port, jamstik.getName());
        }
        
        for (String port : jack.getPorts(jackclient, IN.MIDICLOCK.getPort(), MIDI, OUT)) {
        	RTLogger.log(this, "connecting midiclock " + port + " to " + midiclock.getName());
        	jack.connect(jackclient, port, midiclock.getName());
        	// TODO connect MPKMini (arpeggios) and Crave to ALSA clock
        }
        
        for (String port : jack.getPorts(jackclient, Constants.getDi(), MIDI, INS)) {
        	RTLogger.log(this, "connecting Crave " + craveOut.getName() + " to " + port);
        	jack.connect(jackclient, craveOut.getName(), port);
        }
        
        for (String port : jack.getPorts(jackclient, IN.MIDICLOCK.getPort(), MIDI, INS)) {
        	RTLogger.log(this, "connecting Tempo " + tempo.getName() + " to " + port);
        	jack.connect(jackclient, tempo.getName(), port);
        }
        
        String [] fluid = jack.getPorts(jackclient, "midi_00", MIDI, INS);
        while (fluid.length == 0) {
        	Constants.sleep(100);
        	fluid = jack.getPorts(jackclient, "midi_00", MIDI, INS);
        }
    	for (String port : fluid) {
        	RTLogger.log(this, "connecting Fluid " + fluidOut.getName() + " to " + port);
        	jack.connect(jackclient, fluidOut.getName(), port); 
    	}
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
        	Midi midi;
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
                    midi = new Midi(data);
                    if (port == jamstik) {
                    	if (Jamstik.isActive())
                    		Jamstik.getOut().send(midi, index);
                    }
                    else if (switchboard.get(port).midiProcessed(midi))
            			MainFrame.updateCurrent(); // TODO overkill?
            		else if (midi.getChannel() == 9) 
            			JudahZone.getDrumMachine().getDrum1().send(midi, 0);
            		else if (Midi.isNote(midi) || Midi.isPitchBend(midi))
            			keyboardSynth.getMidiOut().send(Midi.format(midi, keyboardSynth.getCh(), 1), ticker());
                }
            }
        	// check sequencers for output
        	if (queue.isEmpty())
        		return true;
            PortMessage route = queue.poll();
            while(route != null) {
            	JackMidi.eventWrite(route.getPort(), 
            				ticker(), route.getMidi().getMessage(), route.getMidi().getLength());
            	route = queue.poll();
            }
        } catch (Throwable e) {
            RTLogger.warn(this, e);
            if ("ENOBUF".equals(e.getMessage())) {
            	// TODO zone.recoverMidi();
                return true;
            }
        }
        return true;
    }

    public static int ticker() {
    	return ++ticker;
    }
    
    public static void queue(ShortMessage msg, JackPort midiOut) {
    	queue.add(new PortMessage(msg, midiOut));
    }

    public void synchronize(MidiReceiver ch) {
    	if (sync.contains(ch.getMidiPort())) {
    		// TODO send MIDI_RT_STOP to ch
			sync.remove(ch.getMidiPort());
    	}
		else
			sync.add(ch.getMidiPort());
    	MainFrame.update(ch);
    }
    
    /** send start/stop to midi listeners */
	public void synchronize(byte[] midi) {
		for (MidiPort p : sync)
			p.send(new Midi(midi), JudahMidi.ticker());
	}

	public void setKeyboardSynth(MidiTrack port) {
		if (keyboardSynth == port)
			return;
		keyboardSynth = port;
		if (JudahZone.getMidiGui() != null)
			MainFrame.update(JudahZone.getMidiGui());
	}
	
}

// @Getter private JackPort clockOut;
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


