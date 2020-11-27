package net.judah.midi;

import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackMidi.Event;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.instruments.MPK;
import net.judah.instruments.Pedal;
import net.judah.jack.BasicClient;
import net.judah.jack.Status;
import net.judah.settings.ActiveCommand;
import net.judah.settings.Command;
import net.judah.settings.Service;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

// process MIDI ports
// assisted by: https://github.com/jaudiolibs/examples/blob/master/src/main/java/org/jaudiolibs/examples/MidiThru.java
@Log4j
public class MidiClient extends BasicClient implements Service {

	
//	/** Judah's own special sauce */
//	public static final ClientConfig DEFAULT_CONFIG = new ClientConfig("JudahMidi", new String[0], new String[0],
//			new String[] {"keyboard", "pedal", "midiIn"},
//			new String[] {"synth", "drums", "aux", "komplete"});
	
	public static final int SYNTH_CHANNEL = 0;
	public static final int DRUMS_CHANNEL = 9;
	public static final int AUX_CHANNEL = 6;
	public static final int KOMPLETE_CHANNEL = 7;
	
	public static final String JACKCLIENT_NAME = "JudahMidi";
	
	@Getter private static MidiClient instance;
	@Getter private final Router router = new Router();

	private ArrayList<JackPort> inPorts = new ArrayList<JackPort>();  // Keyboard, Pedal, MidiIn
	@Getter private JackPort keyboard;
	@Getter private JackPort pedal;
	@Getter private JackPort midiIn;

	private ArrayList<JackPort> outPorts = new ArrayList<>(); // Synth, Effects, MidiOut
	@Getter private JackPort synth;
	@Getter private JackPort drums;
	@Getter private JackPort aux;
	@Getter private JackPort komplete; 

    private final ConcurrentLinkedQueue<ShortMessage> queue = new ConcurrentLinkedQueue<ShortMessage>();
    
	// for future Timebase interface
	ConcurrentLinkedQueue<Long> timeRequest = new ConcurrentLinkedQueue<>(); 
	
	private final Command routeChannel = new ActiveCommand("Route Midi Channel", this, channelProps(), 
			"take all commands on a given channel and route them to another");
	private final List<Command> cmds = Arrays.asList(new Command[] {routeChannel});
	
    // for process()
	private Event midiEvent = new JackMidi.Event();
	private byte[] data = null;
    private int index, eventCount, size;
	private ShortMessage poll;
	private Midi midi;

	public MidiClient(String name) throws JackException {
		super(name);
		instance = this;
		start();
		
	}
	
	public MidiClient() throws JackException {
		this(JACKCLIENT_NAME);
    }
    
	// Service interface
	@Override public List<Command> getCommands() { return cmds; }
	@Override public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		if (cmd == routeChannel) {
			routeChannel(props);
		}
	}

	public void routeChannel(HashMap<String, Object> props) {
		try {
			boolean active = Boolean.parseBoolean("" + props.get("Active"));
			int from = Integer.parseInt("" + props.get("from"));
			int to = Integer.parseInt("" + props.get("to"));
			Route route = new Route(from, to);
			if (active)
				MidiClient.getInstance().getRouter().add(route);
			else 
				MidiClient.getInstance().getRouter().remove(route);
		} catch (NumberFormatException e) {
			log.error(e.getMessage() + " " + Constants.prettyPrint(props));
		}
		
	}

	@Override
	protected void initialize() throws JackException {
		
		for (JudahPort port : JudahPort.values())
        	outPorts.add(jackclient.registerPort(port.getPortName(), MIDI, JackPortIsOutput));
		
        for (JudahDevice device : JudahDevice.values()) {
        	inPorts.add(jackclient.registerPort(device.getPortName(), MIDI, JackPortIsInput));
        }

        if (inPorts.size() > 0) keyboard = inPorts.get(0);
        if (inPorts.size() > 1) pedal = inPorts.get(1);
        if (inPorts.size() > 2) midiIn = inPorts.get(2);
        if (outPorts.size() > 0) synth = outPorts.get(0);
        if (outPorts.size() > 1) drums = outPorts.get(1);
        if (outPorts.size() > 2) aux = outPorts.get(2);
        if (outPorts.size() > 3) komplete = outPorts.get(3);
        
		//	jackclient.setTimebaseCallback(metro, false);
		//	jackclient.setSyncCallback(metro);
	}

	@SuppressWarnings("deprecation")
	@Override 
	protected void makeConnections() throws JackException {

    	for (String port : jack.getPorts(jackclient, Pedal.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
    		log.debug("connecting foot pedal: " + port + " to " + midiIn.getName());
    		jack.connect(jackclient, port, pedal.getName());
    	}
    	for (String port : jack.getPorts(jackclient, MPK.NAME, MIDI, EnumSet.of(JackPortIsOutput))) {
        	log.debug("connecting keyboard: " + port + " to " + midiIn.getName());
        	jack.connect(jackclient, port, keyboard.getName());
    	}
    	
    	String [] audioInterface = jack.getPorts("Komplete", MIDI, EnumSet.of(JackPortIsInput));
		if (audioInterface.length == 1)
			jack.connect(komplete.getName(), audioInterface[0]);
		else 
			log.warn(Arrays.toString(jack.getPorts("Komplete", MIDI, EnumSet.of(JackPortIsInput))));
    	// Fluid Synth connects when it is initialized
	}


    @Override
	public boolean process(JackClient client, int nframes) {
    	try {
    		JackMidi.clearBuffer(synth);
    		JackMidi.clearBuffer(drums);
    		JackMidi.clearBuffer(aux);
    		JackMidi.clearBuffer(komplete);
    		
        	for (JackPort port : inPorts) {
        		eventCount = JackMidi.getEventCount(port);

        		for (index = 0; index < eventCount; index++) {
        			
        			if (JackMidi.getEventCount(port) != eventCount) {
        				RTLogger.warn(this, "eventCount found " + JackMidi.getEventCount(port) + " expected " + eventCount);
        				return true;
        			}
        			JackMidi.eventGet(midiEvent, port, index);
                    size = midiEvent.size();
                    if (data == null || data.length != size) {
                        data = new byte[size];
                    }
                    midiEvent.read(data);
                    midi = router.process(new Midi(data));
                    if (JudahZone.getCurrentSong() != null
	                    && JudahZone.getCurrentSong().getCommander().midiProcessed(midi) == false) {
	                	write(midi, midiEvent.time());
	        		}
        		}
    		}
        	
    		poll = queue.poll();
    		while (poll != null) {
    			write(poll, 0);
    			poll = queue.poll();
    		}
    		
    	} catch (Exception e) {
    		RTLogger.warn(this, e);
    		return false;
    	}
    	return state.get() == Status.ACTIVE;
    }

    private void write(ShortMessage midi, int time) throws JackException {
    	switch (midi.getChannel()) {
    	case KOMPLETE_CHANNEL:
    		JackMidi.eventWrite(komplete, time, midi.getMessage(), midi.getLength());
    		break;
    	case DRUMS_CHANNEL:
    		JackMidi.eventWrite(drums, time, midi.getMessage(), midi.getLength());
    		break;
    	case AUX_CHANNEL:
    		JackMidi.eventWrite(aux, time, midi.getMessage(), midi.getLength());
    		break;
    	default:
    		JackMidi.eventWrite(synth, time, midi.getMessage(), midi.getLength());
    	}
    }
    
	public void queue(ShortMessage message) {
		// log.info("queued " + message);
		queue.add(message);
	}

	@Override
	public String getServiceName() {
		return MidiClient.class.getSimpleName();
	}
	
	public void disconnectDrums() {
		try {
	    	for (String port : jack.getPorts(jackclient, drums.getShortName(), MIDI, EnumSet.of(JackPortIsOutput))) {
	    		log.debug("disconnecting " + port + " from " + FluidSynth.MIDI_PORT);
	    		jack.disconnect(jackclient, port, FluidSynth.MIDI_PORT);
	    	}
		} catch (JackException e) {
			log.error("disconnecting drums error, " + e.getMessage());
		}
	}

	private HashMap<String, Class<?>> channelProps() {
		HashMap<String, Class<?>> result = new HashMap<String, Class<?>>();
		result.put("from", Integer.class);
		result.put("to", Integer.class);
		return result;
	}

}


// jack.connect(jackclient, dr5Port.getName(), "a2j:Komplete Audio 6 [20] (playback): Komplete Audio 6 MIDI 1");
