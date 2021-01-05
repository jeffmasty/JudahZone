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
import net.judah.StageCommands;
import net.judah.api.BasicClient;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.api.Service;
import net.judah.api.Status;
import net.judah.fluid.FluidSynth;
import net.judah.plugin.MPK;
import net.judah.plugin.Pedal;
import net.judah.sequencer.Sequencer;
import net.judah.settings.MidiSetup.IN;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** handle MIDI for the project */
@Log4j
public class JudahMidi extends BasicClient implements Service, MidiQueue {

	public static final String JACKCLIENT_NAME = "JudahMidi";
	private long frame = 0;
	
	@Getter private static JudahMidi instance;
	/** Jack Frame */
	public static long getCurrent() {
		return instance.scheduler.getCurrent();
	}
	
	@Getter private final MidiScheduler scheduler = new MidiScheduler(this);
	@Getter private final Router router = new Router();

	private ArrayList<JackPort> inPorts = new ArrayList<JackPort>();  // Keyboard, Pedal, MidiIn
	@Getter private JackPort keyboard;
	@Getter private JackPort pedal;
	@Getter private JackPort midiIn;

	private ArrayList<JackPort> outPorts = new ArrayList<>(); // Synth, Effects, MidiOut
	@Getter private JackPort synth;
	@Getter private JackPort drums;
	@Getter private JackPort aux;

    private final ConcurrentLinkedQueue<ShortMessage> queue = new ConcurrentLinkedQueue<ShortMessage>();
    private final MidiClock clock;
    
    @Getter MidiCommands commands = new MidiCommands(this);
	
    // for process()
	private Event midiEvent = new JackMidi.Event();
	private byte[] data = null;
    private int index, eventCount, size;
	private ShortMessage poll;
	private Midi midi;
	private final StageCommands stage = new StageCommands();

	public JudahMidi(String name, MidiClock clock) throws JackException {
		super(name);
		instance = this;
		this.clock = clock;
		start();
	}

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
        
		//	jackclient.setTimebaseCallback(this, false);
        new Thread(scheduler).start();

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
    	
    	log.debug("Connecting Midi Clock");

//    	jack.connect(jackclient, 
//    			"a2j:Komplete Audio 6 [24] (capture): Komplete Audio 6 MIDI 1", 
//    			midiIn.getName());
    	
    	String [] usbMidi = jack.getPorts("Komplete", MIDI, EnumSet.of(JackPortIsInput));
		if (usbMidi.length == 1)
			jack.connect(jackclient, drums.getName(), usbMidi[0]);
		
		usbMidi = jack.getPorts("Komplete", MIDI, EnumSet.of(JackPortIsOutput));
		if (usbMidi.length == 1)
			jack.connect(jackclient, usbMidi[0], midiIn.getName());
    	
		jack.connect(jackclient, synth.getName(), FluidSynth.MIDI_PORT);
		//fluid connect(midi.getJackclient(), midi.getSynth());
    	
	}


    @Override
	public boolean process(JackClient client, int nframes) {
    	
    	try {
    		
    		scheduler.offer(frame++);
    		
    		JackMidi.clearBuffer(synth);
    		JackMidi.clearBuffer(drums);
    		JackMidi.clearBuffer(aux);
    		
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
                    if (port == midiIn) {
                    	clock.process(data);
                    	continue;
                    }

                    midi = router.process(new Midi(data));
                    
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
		//	case KOMPLETE_CHANNEL:
		//		JackMidi.eventWrite(komplete, time, midi.getMessage(), midi.getLength());
		//		break;
    	case DRUMS_CHANNEL: // sending drum notes to external drum machine
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
	public void properties(HashMap<String, Object> props) {
		scheduler.clear();
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

