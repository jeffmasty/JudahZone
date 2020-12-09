package net.judah.sequencer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.api.MidiClient;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.api.TimeNotifier;
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.util.Constants;

@Log4j
public class SeqClock implements TimeNotifier {

	// 2 channel 8-step sequence output to 2 channels
	/*feel good:bass drum on 1 and 2.5 (inside)
		 		closed hihat on 1.5, 2, 3, 3,5, 4, 4.5 (outside)
	// sequence active= note=36 duration=16 type=inside notes=0,2.5 
	// sequence active= duration=16 type=outside notes=0.5,1,1.5,2,3,3.5,4,4.5 
	  AndILove:  (metro clicktrack intro) 
	  		shaker on 1 and 3 (inside) */

	@Getter(value = AccessLevel.PACKAGE) 
	private final ArrayList<SeqData> sequences = new ArrayList<>();
	private final ArrayList<TimeListener> listeners = new ArrayList<>();
	private final Sequencer seq;

	public static final int DEFAULT_DOWNBEAT = 34;
	public static final int DEFAULT_BEAT = 33;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final WakeUp wakeUp = new WakeUp();
	private final AtomicBoolean changed = new AtomicBoolean(true);
	private ScheduledFuture<?> beeperHandle;
	private int count = 0;
	private Integer duration = null;
	private int steps;
	private int step = 0;
	
	
	private Midi downbeat, beat;
	private Midi bassdrum;
	// to Speakers only
	private final MidiClient externalMidi;
	// On record/loop
	private final JudahMidi internalMidi;
	private Integer bassThumps = null;

	public SeqClock(Sequencer sequencer, Integer intro, Integer length) throws InvalidMidiDataException {
		this(sequencer, intro, length, DEFAULT_DOWNBEAT, DEFAULT_BEAT);
	}

	// 8th-note step sequencer
	public SeqClock(Sequencer sequencer, Integer intro, Integer length, int downbeatNote, int beatNote) 
			throws InvalidMidiDataException {
		this(sequencer, intro, length, downbeatNote, beatNote, sequencer.getMeasure() * 2);
	}
		
	public SeqClock(Sequencer sequencer, Integer intro, Integer length, int downbeatNote, int beatNote, 
			int steps) throws InvalidMidiDataException {
		this.seq = sequencer;
		this.steps = steps;
		count = (intro == null) ? 0 : 0 - intro;
		duration = (length == null) ? 0 : count + length;

		downbeat = new Midi(ShortMessage.NOTE_ON, 9, downbeatNote, 100);
		beat = new Midi(ShortMessage.NOTE_ON, 9, beatNote, 100);
		bassdrum = new Midi(ShortMessage.NOTE_ON, 9, 36, 100);
		externalMidi = Metronome.getMidi();
		internalMidi = JudahZone.getMidi();
	}

	SeqClock(Sequencer seq) {
		this.seq = seq;
		addListener(seq);
		downbeat = beat = bassdrum = null;
		externalMidi = Metronome.getMidi();
		internalMidi = JudahZone.getMidi();
		steps = seq.getMeasure() * 2;
	}

	SeqClock(Sequencer seq, HashMap<String, Object> props) throws Exception {
		this(seq);
		seq.setTempo(Float.parseFloat("" + props.get(Constants.PARAM_BPM)));
		seq.setMeasure(Integer.parseInt("" + props.get(Constants.PARAM_MEASURE)));
		seq.setPulse(Integer.parseInt("" + props.get(Sequencer.PARAM_PULSE)));
		steps = seq.getMeasure() * 2;
		int intro = Integer.parseInt("" + props.get(SeqCommands.PARAM_INTRO));
		count = 0 - intro;
	}

	@Override
	public void addListener(TimeListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}

	class WakeUp implements Runnable {
		
		@Override public void run() {

			if (changed.compareAndSet(true, false)) { // initilization or tempo update
				beeperHandle.cancel(true);
				long period = Constants.millisPerBeat(seq.getTempo() * 2); // eighth notes
		        beeperHandle = scheduler.scheduleAtFixedRate(
		        		this, period, period, TimeUnit.MILLISECONDS);
		        scheduler.schedule(
		        		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
		        		24, TimeUnit.HOURS);
		        
			}
			
	        chirp();
		}
	}

	public SeqData byName(String name) {
		for (SeqData sequence : getSequences()) 
			if (sequence.getName().equals(name))
				return sequence;
		return null;
	}
	
	public boolean hasClicktrack() {
		return duration != null && duration > 0;
	}
	
	public void start() {
		
		if (beeperHandle != null) return;
		beeperHandle = scheduler.scheduleAtFixedRate(wakeUp, 0, 
				Constants.millisPerBeat(seq.getTempo() * 2), TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable() 
		{@Override public void run() {beeperHandle.cancel(true);}},
    		2, TimeUnit.HOURS);
		
	}

	public void stop() {
		if (beeperHandle == null) return;
		beeperHandle.cancel(true);
		beeperHandle = null;
		log.debug("internal shutdown");
	}
	
	public void changed() {
		changed.set(true);
	}

	void chirp() {

		if (step % 2 == 0) {
	        if (count == 0)
	        	listeners.forEach(listener -> {listener.update(
	        			Property.TRANSPORT, JackTransportState.JackTransportStarting);});
			listeners.forEach(listener -> {listener.update(Property.BEAT, count);});
			count++;
		}
        for (SeqData s : sequences) {
        	if (!s.isActive()) continue;
        	// TODO check loop count against beat count
        	for (int i : s.getSequence()) 
        		if (i == step)
        			if (s.isRecord())
        				internalMidi.queue(s.getNote());
        			else 
        				externalMidi.queue(s.getNote());
        }
		step++;
		if (step == steps) 
			step = 0;
		
		
		
//        if (duration != null && count <= duration) {
//			boolean first = count % seq.getMeasure() == 0;
//			internalMidi.queue(first ? downbeat : beat);
////			if (count == duration) 
////				log.debug("tick tock shutting down");
////				try {// Metronome.setActive(false);
////				} catch (Exception e) { log.warn(e.getMessage(), e); }
//        }
//
//        if (bassThumps != null && count < bassThumps) {
//        	internalMidi.queue(bassdrum);
//        }
        		
	}

	public void doBassDrum(int thumps) {
		bassThumps = thumps;
	}

	public void addSequence(SeqData seqData) {
		// TODO Auto-generated method stub
		
	}
	
}
