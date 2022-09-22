package net.judah.midi;
import static net.judah.midi.MidiSetup.DRUMS_CHANNEL;
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

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.BasicClient;
import net.judah.api.Midi;
import net.judah.api.PortMessage;
import net.judah.controllers.Controller;
import net.judah.controllers.KorgMixer;
import net.judah.controllers.KorgPads;
import net.judah.controllers.Line6FBV;
import net.judah.controllers.MPKmini;
import net.judah.midi.JudahClock.Mode;
import net.judah.midi.MidiSetup.IN;
import net.judah.midi.MidiSetup.OUT;
import net.judah.mixer.Channel;
import net.judah.tracker.Track;
import net.judah.util.RTLogger;

/** handle MIDI integration with Jack */
public class JudahMidi extends BasicClient implements Closeable {

	public static final String JACKCLIENT_NAME = "JudahMidi";

	private final JudahZone zone;
	@Getter private JudahClock clock;
    
    @Getter private ArrayList<JackPort> inPorts = new ArrayList<>();  
    @Getter private JackPort pulse;
    @Getter private JackPort keyboard;
    @Getter private JackPort pedal;
    @Getter private JackPort pads;
    @Getter private JackPort mixer;
    @Getter private JackPort arduino;
    @Getter private JackPort line6;
    @Getter private JackPort judahSynth;
    
    @Getter private final ArrayList<JackPort> outPorts = new ArrayList<>(); 
    @Getter private JackPort fluidOut;
    @Getter private JackPort craveOut;
    @Getter private JackPort clockOut;
    @Getter private JackPort calfOut;
    @Getter private JackPort tempo;
    
    @Getter private MidiPort keyboardSynth;
    @Getter private final ArrayList<Path> paths = new ArrayList<>();
    @Getter private final ArrayList<MidiPort> sync = new ArrayList<>();
    @Getter private final MidiScheduler scheduler = new MidiScheduler(this);
    @Getter private static final ConcurrentLinkedQueue<PortMessage> queue = new ConcurrentLinkedQueue<>();
    private static int ticker;
    private final HashMap<JackPort, Controller> switchboard = new HashMap<>();

    private long frame = 0;
    private String[] sources, destinations; // for GUI
    private final byte[] DATA1 = new byte[1], DATA2 = new byte[2], DATA3 = new byte[3]; // for process()
    private final Event midiEvent = new JackMidi.Event(); // for process()

    public JudahMidi(String name, JudahZone zone) throws JackException {
        super(name);
        this.zone = zone;
        JudahZone.getServices().add(this);
        start();
    }

    @Override
    protected void initialize() throws JackException {
    	// init getters for out ports
        for (OUT port : OUT.values())
            outPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsOutput));
        
        int sz = outPorts.size();
        if (sz > OUT.TEMPO.ordinal()) {
        	tempo = outPorts.get(OUT.TEMPO.ordinal());
        }
        if (sz > OUT.CLOCK_OUT.ordinal()) clockOut = outPorts.get(OUT.CLOCK_OUT.ordinal());
        if (sz > OUT.SYNTH_OUT.ordinal()) {
        	fluidOut = outPorts.get(OUT.SYNTH_OUT.ordinal());
        }
        if (sz > OUT.CALF_OUT.ordinal()) {
        	calfOut = outPorts.get(OUT.CALF_OUT.ordinal());
        }
        if (sz > OUT.CRAVE_OUT.ordinal()) {
        	craveOut = outPorts.get(OUT.CRAVE_OUT.ordinal());
        }
        
        // connect midi controllers to software handlers
        for (IN port : IN.values())
            inPorts.add(jackclient.registerPort(port.port, MIDI, JackPortIsInput));
        sz = inPorts.size();
        if (sz > IN.PULSE.ordinal()) {
        	pulse = inPorts.get(IN.PULSE.ordinal());
        }
        if (sz > IN.KEYBOARD.ordinal()) {
        	keyboard = inPorts.get(IN.KEYBOARD.ordinal());
        	switchboard.put(keyboard, new MPKmini(this));
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

        if (switchboard.isEmpty()) 
        	RTLogger.warn(this, new NullPointerException("JudahMidi: no controllers connected"));

        Thread timePolling = new Thread(scheduler);
        timePolling.setPriority(7);
        timePolling.setName(scheduler.getClass().getSimpleName());
        timePolling.start();
        
    }

    @Override
    protected void makeConnections() throws JackException {

        for (String port : jack.getPorts(jackclient, MPKmini.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
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
        this.clock = new JudahClock(this); 
        zone.finalizeMidi();
    }

    @Override
    public boolean process(JackClient client, int nframes) {
    	if (clock == null)
    		return true;
        try {

            scheduler.offer(frame++);
            ticker = 0;
            for (JackPort port : outPorts)
                JackMidi.clearBuffer(port);
            
            if (clock.getMode() == Mode.Internal)
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
                    if (midiEvent.size() == 1) { 
                    	if (clock.getMode() == Mode.Midi24) {
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
                    midi = new Midi(data, port.getShortName());
                    if (port.getShortName().equals(IN.JUDAH_SYNTH.port))
                    	JudahZone.getSynth().send(midi, -1);
                    else if (switchboard.get(port) != null) {
                		if (switchboard.get(port).midiProcessed(midi))
                			MainFrame.updateCurrent();
                		else 
                			write(midi, ticker());
                	}
                }
            }

            // check sequencers for output
            PortMessage route = queue.poll();
            while(route != null) {
            	if (route.getPort() == null) 
            		write(route.getMidi(), ticker());
            	else 
            		JackMidi.eventWrite(route.getPort(), 
            				ticker(), route.getMidi().getMessage(), route.getMidi().getLength());
            	route = queue.poll();
            }

        } catch (Throwable e) {

            RTLogger.warn(this, e);

            if ("ENOBUF".equals(e.getMessage())) {
                zone.recoverMidi();
                return false;
            }
        }
        return true;
    }

    public static int ticker() {
    	return ++ticker;
    }
    
    private void write(ShortMessage midi, int time) throws JackException {
        switch (midi.getChannel()) {
	        case DRUMS_CHANNEL: // sending drum notes to external drum machine
	            JackMidi.eventWrite(getCalfOut(), time, midi.getMessage(), midi.getLength());
	            break;
	        default: 
	        	keyboardSynth.send(midi, time);
        }
    }

	public static void queue(ShortMessage midi, JackPort midiOut) {
    	queue.add(new PortMessage(midi, midiOut));
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

    public void synchronize(MidiPort port) {
    	if (sync.contains(port)) {
			sync.remove(port);
			synchronize(JudahClock.MIDI_RT_STOP);
    	}
		else
			sync.add(port);
    	MainFrame.update(port);
    }
    
    /** send start/stop to midi listeners */
	public void synchronize(byte[] midi) {
		for (MidiPort p : sync)
			p.send(new Midi(midi), JudahMidi.ticker());
		for (Track t : JudahZone.getTracker().getTracks())
			t.setStep(0);
	}

	public Path getPath(JackPort port) {
		for (Path p : paths)
			if (port.equals(p.getPort().getPort()))
				return p;
		return null;
	}

	public Path getPath(Channel ch) {
		for (Path p : paths)
			if (p.getChannel().equals(ch))
				return p;
		return null;
	}

	public void setKeyboardSynth(MidiPort port) {
		if (keyboardSynth == port)
			return;
		MidiPort old = keyboardSynth;
		keyboardSynth = port;
		if (old != null)
    		new Panic(old).start();
	}
	
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
//        }


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

//    if (sz > IN.CRAVE_IN.ordinal()) {
		//    	craveIn = inPorts.get(IN.CRAVE_IN.ordinal());
		//    	switchboard.put(craveIn, new Crave());}
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

//    public JackPort getByName(String name) {
//        for (JackPort p : outPorts)
//            if (p.getShortName().equals(name))
//                return p;
//        for (JackPort p : inPorts)
//            if (p.getShortName().equals(name))
//                return p;
//        RTLogger.warn(this, name + " midi port not found, using default out");
//        return outPorts.get(0);
//    }


