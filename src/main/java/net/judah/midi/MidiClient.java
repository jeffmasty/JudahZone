package net.judah.midi;

import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiMessage;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackMidi.Event;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.instruments.MPK;
import net.judah.instruments.Pedal;
import net.judah.jack.BasicClient;
import net.judah.jack.ClientConfig;
import net.judah.jack.Status;
import net.judah.settings.Command;
import net.judah.settings.Service;
import net.judah.util.RTLogger;
import net.judah.util.Tab;

// process MIDI ports
// assisted by: https://github.com/jaudiolibs/examples/blob/master/src/main/java/org/jaudiolibs/examples/MidiThru.java
@Log4j
public class MidiClient extends BasicClient implements Service {

	/** Judah's own special sauce */
	public static final ClientConfig DEFAULT_CONFIG = new ClientConfig("JudahMidi", new String[0], new String[0],
			new String[] {"keyboard", "pedal", "midiIn"},
			new String[] {"synth", "effects", "midiOut"});
	
	@Getter private static MidiClient instance;
	
	private final ClientConfig config;
	private final CommandHandler commander;

	private ArrayList<JackPort> inPorts = new ArrayList<JackPort>();  // Keyboard, Pedal, MidiIn
	private JackPort keyboard;
	private JackPort pedal;
	private JackPort midiIn;

	private ArrayList<JackPort> outPorts = new ArrayList<>(); // Synth, Effects, MidiOut
	@Getter private JackPort synth;
	@SuppressWarnings("unused")
	private JackPort effects;
	@SuppressWarnings("unused")
	private JackPort midiOut;

    ConcurrentLinkedQueue<Long> timeRequest = new ConcurrentLinkedQueue<>(); // for future Timebase interface
	
    private final ConcurrentLinkedQueue<MidiMessage> queue = new ConcurrentLinkedQueue<MidiMessage>();
    
	// for process()
	private Event midiEvent = new JackMidi.Event();
	private byte[] data = null;
    private int index, eventCount, size;
	private MidiMessage poll;
    
    public MidiClient(CommandHandler commander) throws JackException {
    	this(DEFAULT_CONFIG, commander);
    }
    
	public MidiClient(ClientConfig config, CommandHandler commander) throws JackException {
		super(config.getName());
		this.config = config;
		this.commander = commander;
		instance = this;
		start();
	}

	// Service interface
	@Override public List<Command> getCommands() { return Collections.emptyList(); }
	@Override public void execute(Command cmd, HashMap<String, Object> props) throws Exception { }
	@Override public Tab getGui() { return null; }
	

	@Override
	protected void initialize() throws JackException {
        for (String in : config.getMidiInputNames()) {
        	inPorts.add(jackclient.registerPort(in, MIDI, JackPortFlags.JackPortIsInput));
        }
        for (String out : config.getMidiOutputNames()) {
        	outPorts.add(jackclient.registerPort(out, MIDI, JackPortIsOutput));
        }

        if (inPorts.size() > 0) keyboard = inPorts.get(0);
        if (inPorts.size() > 1) pedal = inPorts.get(1);
        if (inPorts.size() > 2) midiIn = inPorts.get(2);
        if (outPorts.size() > 0) synth = outPorts.get(0);
        if (outPorts.size() > 1) effects = outPorts.get(1);
        if (outPorts.size() > 2) midiOut = outPorts.get(2);
        
		//	jackclient.setTimebaseCallback(metro, false);
		//	jackclient.setSyncCallback(metro);
	}

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
    	// Fluid Synth connects when it is initialized, see getJackclient()
	}

    @Override
	public boolean process(JackClient client, int nframes) {
    	try {
    		JackMidi.clearBuffer(synth);

        	for (JackPort port : inPorts) {
        		eventCount = JackMidi.getEventCount(port);

        		for (index = 0; index < eventCount; ++index) {
        			JackMidi.eventGet(midiEvent, port, index);
                    size = midiEvent.size();
                    if (data == null || data.length != size) {
                        data = new byte[size];
                    }
                    midiEvent.read(data);
	                if (commander.midiProcessed(data) == false) {
	                    JackMidi.eventWrite(synth, midiEvent.time(), data, midiEvent.size());
	        		}
        		}
    		}
        	
    		poll = queue.poll();
    		while (poll != null) {
    			JackMidi.eventWrite(synth, 0, poll.getMessage(), poll.getLength());
    			poll = queue.poll();
    		}
    		
    	} catch (Exception e) {
    		RTLogger.warn(this, e);
    		return false;
    	}
    	return state.get() == Status.ACTIVE;
    }

	public void queue(MidiMessage message) {
		queue.add(message);
	}

	@Override
	public String getServiceName() {
		return MidiClient.class.getSimpleName();
	}

}

// jack.connect(jackclient, dr5Port.getName(), "a2j:Komplete Audio 6 [20] (playback): Komplete Audio 6 MIDI 1");
