package net.judah.api;

import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;

/** Create a Jack midi port and process queue'd messages<br>
 assisted by: https://github.com/jaudiolibs/examples/blob/master/src/main/java/org/jaudiolibs/examples/MidiThru.java */
public class MidiClient extends BasicClient implements MidiQueue {

	private final String[] inputs, outputs;
	private String synthMidiConnect;
	
	@Getter protected final ArrayList<JackPort> outPorts = new ArrayList<>(); 
	@Getter protected final ArrayList<JackPort> inPorts = new ArrayList<JackPort>();  

    protected final ConcurrentLinkedQueue<ShortMessage> queue = new ConcurrentLinkedQueue<ShortMessage>();
    
	public MidiClient(String name, String[] inPorts, String[] outPorts) throws JackException {
		super(name);
		inputs = inPorts;
		outputs = outPorts;
		start();
	}
	
	public MidiClient(String[] inPorts, String[] outPorts) throws JackException {
		this(MidiClient.class.getSimpleName(), inPorts, outPorts);
    }

	public MidiClient(String clientName, String[] in, String[] out, String synthMidiPort) throws JackException {
		this(clientName, in, out);
		synthMidiConnect = synthMidiPort;
	}

	@Override
	protected void initialize() throws JackException {
		for (String in : inputs) 
			inPorts.add(jackclient.registerPort(in, MIDI, JackPortIsInput));
		for (String out : outputs)
			outPorts.add(jackclient.registerPort(out, MIDI, JackPortIsOutput));
	}

	@Override 
	protected void makeConnections() throws JackException {
		if (synthMidiConnect == null) return;
		Jack.getInstance().connect(getJackclient(), getOutPorts().get(0).getName(), synthMidiConnect);
	}

	private ShortMessage poll;
    @Override
	public boolean process(JackClient client, int nframes) {
    	try {
    		for (JackPort p : outPorts)
    			JackMidi.clearBuffer(p);
        	
    		poll = queue.poll();
    		while (poll != null) {
    			write(poll, 0);
    			poll = queue.poll();
    		}
    		
    	} catch (Exception e) {
    		System.err.println(e.getMessage());
    		return false;
    	}
    	return state.get() == Status.ACTIVE;
    }

    private void write(ShortMessage midi, int time) throws JackException {
    	if (!outPorts.isEmpty())
    		JackMidi.eventWrite(outPorts.get(0), time, midi.getMessage(), midi.getLength());
    }
    
	@Override
	public void queue(ShortMessage message) {
		// log.debug("queued " + message);
		queue.add(message);
	}

}


// jack.connect(jackclient, dr5Port.getName(), "a2j:Komplete Audio 6 [20] (playback): Komplete Audio 6 MIDI 1");
