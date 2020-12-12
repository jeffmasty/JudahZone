package net.judah.midi;
import static net.judah.settings.Commands.OtherLbls.*;
import static net.judah.settings.MidiSetup.*;
import static net.judah.util.Constants.Param.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.io.File;
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
import net.judah.api.BasicClient;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.api.Service;
import net.judah.api.Status;
import net.judah.plugin.MPK;
import net.judah.plugin.Pedal;
import net.judah.sequencer.Sequencer;
import net.judah.settings.MidiSetup.IN;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Constants;
import net.judah.util.JudahException;
import net.judah.util.RTLogger;

/** handle MIDI for the project */
@Log4j
public class JudahMidi extends BasicClient implements Service, MidiQueue {

	public static final String JACKCLIENT_NAME = "JudahMidi";
	
	@Getter private static JudahMidi instance;
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
	
	private final Command routeChannel = new Command(ROUTECHANNEL.name, ROUTECHANNEL.desc, channelTemplate()) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			routeChannel(props);}};
	private final Command midiNote = new Command(MIDINOTE.name, MIDINOTE.desc, Midi.midiTemplate()) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			queue(Midi.fromProps(props));}};
    private final Command midiPlay = new Command(MIDIFILE.name, MIDIFILE.desc, playTemplate()) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			boolean active = false;
			if (midiData2 > 0)
				active = true;
			else if (props.containsKey(ACTIVE))
				try { active = Boolean.parseBoolean("" + props.get(ACTIVE));
				} catch (Throwable t) { log.debug(props.get(ACTIVE) + " " + t.getMessage());  }
			Object o = props.get(FILE);
			if (false == o instanceof String)  throw new JudahException("Missing midi file to play");
			File midi = new File(o.toString());
			if (!midi.isFile()) throw new JudahException("Not a midi file: " + o);
			if (active) { 
				JudahZone.getMetronome().setMidiFile(midi);
				JudahZone.getMetronome().play();
			}
			else
				JudahZone.getMetronome().stop();
		}

    };

	private final Octaver octaver = new Octaver(this);
    
	private final List<Command> cmds = Arrays.asList(new Command[] {routeChannel, midiNote, midiPlay, octaver});
	
    // for process()
	private Event midiEvent = new JackMidi.Event();
	private byte[] data = null;
    private int index, eventCount, size;
	private ShortMessage poll;
	private Midi midi;

	public JudahMidi(String name) throws JackException {
		super(name);
		instance = this;
		start();
		
	}
	
	public JudahMidi() throws JackException {
		this(JACKCLIENT_NAME);
    }
    
	// Service interface
	@Override public List<Command> getCommands() { return cmds; }

	public void routeChannel(HashMap<String, Object> props) {
		try {
			boolean active = Boolean.parseBoolean("" + props.get("Active"));
			int from = Integer.parseInt("" + props.get("from"));
			int to = Integer.parseInt("" + props.get("to"));
			Route route = new Route(from, to);
			if (active)
				JudahMidi.getInstance().getRouter().add(route);
			else 
				JudahMidi.getInstance().getRouter().remove(route);
		} catch (NumberFormatException e) {
			log.error(e.getMessage() + " " + Constants.prettyPrint(props));
		}
		
	}

	@Override
	protected void initialize() throws JackException {
		
		for (OUT port : OUT.values())
        	outPorts.add(jackclient.registerPort(port.name, MIDI, JackPortIsOutput));
		
        for (IN port : IN.values()) 
        	inPorts.add(jackclient.registerPort(port.name, MIDI, JackPortIsInput));
        

        if (inPorts.size() > 0) keyboard = inPorts.get(0);
        if (inPorts.size() > 1) pedal = inPorts.get(1);
        if (inPorts.size() > 2) midiIn = inPorts.get(2);
        if (outPorts.size() > 0) synth = outPorts.get(0);
        if (outPorts.size() > 1) drums = outPorts.get(1);
        if (outPorts.size() > 2) aux = outPorts.get(2);
        if (outPorts.size() > 3) komplete = outPorts.get(3);
        
		//	jackclient.setTimebaseCallback(this, false);

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
                    if (Sequencer.getCurrent() != null
	                    && Sequencer.getCurrent().getCommander().midiProcessed(midi) == false) {
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
    
	@Override
	public void queue(ShortMessage message) {
		// log.trace("queued " + message);
		queue.add(message);
	}

	@Override
	public String getServiceName() {
		return JudahMidi.class.getSimpleName();
	}
	
	public static HashMap<String, Class<?>> channelTemplate() {
		HashMap<String, Class<?>> result = new HashMap<String, Class<?>>();
		result.put("from", Integer.class);
		result.put("to", Integer.class);
		return result;
	}

	public static HashMap<String, Class<?>> playTemplate() {
		HashMap<String, Class<?>> result = new HashMap<String, Class<?>>();
		result.put(FILE, String.class);
		result.put(ACTIVE, Boolean.class);
		return result;
	}
	
	@Override
	public void properties(HashMap<String, Object> props) {
		
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

