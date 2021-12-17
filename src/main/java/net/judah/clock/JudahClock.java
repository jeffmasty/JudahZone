package net.judah.clock;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Midi;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.beatbox.BeatBox;
import net.judah.clock.LoopSynchronization.SelectType;
import net.judah.looper.Recorder;
import net.judah.metronome.MidiGnome;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiClock;
import net.judah.sequencer.Sequencer;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class JudahClock implements MidiClock, TimeProvider, TimeListener {

	@Getter private static JudahClock instance = new JudahClock();
    
	public static final int[] LENGTHS= {1, 2, 4, 8, 10, 12, 16, 20, 24, 32, 36};
	public static enum Mode { Internal, /**24 pulses per quarter note external midi clock*/ Midi24 };
	@Getter private static Mode mode = Mode.Internal;
    @Getter private final ArrayList<BeatBox> sequencers = new ArrayList<>();
	@Getter private BeatBuddy drummachine = new BeatBuddy();
    @Getter private ClockGui gui = new ClockGui(this);
    private final ArrayList<TimeListener> listeners = new ArrayList<>();
    private TimeListener listener;

    @Getter @Setter private boolean active;
    @Getter private float tempo = 90f;
    /** current beat */
    @Getter private int beat = -1;
    /** current step */
	@Getter private int step = 0;
    @Getter private int steps = 16;
	@Getter private int subdivision = 4;
	private int _measure = steps / subdivision;
	
    /** current number of bars to record/computeTempo */
	@Getter private int length = 4; 
    private float exTempo; // tempo of external clock when running on internal clock

	/** jack frames since transport start or frames between pulses */
	@Getter private long lastPulse;
	private long nextPulse;

	/** Loop Synchronization */
	private static SelectType onDeck;
	
	
	MidiGnome gnome;
	
	// listen to midi clock
    private int pulse;
    private long ticker;
    private long delta;

	private JudahClock() {
	    for (int i = 0; i < 16; i++) {
	        sequencers.add(new BeatBox(this, i));
	    }
//	    if (Constants.defaultDrumFile.isFile())
//	    	sequencers.get(9).load(Constants.defaultDrumFile); // load a default drum pattern
	    try {
	    	gnome = new MidiGnome(this, JudahMidi.getInstance());
	    } catch (Exception e) {
	    	RTLogger.warn(this, e);
	    }
	    
	    
	}

	public void process() throws JackException {
		ShortMessage poll = drummachine.getQueue().poll();
        while (poll != null) {
            JackMidi.eventWrite(JudahMidi.getInstance().getDrumsOut(), 0, poll.getMessage(), poll.getLength());
            RTLogger.log(this, "to beat buddy: " + poll);
            poll = drummachine.getQueue().poll();
        }
	
        if (mode == Mode.Midi24 || !active) 
        	return;
        if (System.currentTimeMillis() < nextPulse) 
        	return;
		lastPulse = nextPulse;
		nextPulse = computeNextPulse();
		step();
	}
	
	@Override
	public void processTime(byte[] midi) {
		int stat;
        if (midi.length == 1)
            stat = midi[0] & 0xFF;
        else if (midi.length == 2) {
            stat = midi[1] & 0xFF;
            stat = (stat << 8) | (midi[0] & 0xFF);
        }
        else {
            stat = 0;
            RTLogger.log(this, midi.length + " " + new Midi(midi));
        }

        if (ShortMessage.START == stat) {
            RTLogger.log(this, "MIDI START!");
            listeners.forEach(l -> {l.update(Property.TRANSPORT,
                    JackTransportState.JackTransportStarting); });
            beat = 0;
            pulse = 0;
        }

        else if (ShortMessage.STOP == stat) {
            RTLogger.log(this, "MIDI STOP");
            listeners.forEach(l -> {l.update(Property.TRANSPORT,
                    JackTransportState.JackTransportStopped); });
        }

        else if (ShortMessage.CONTINUE == stat) {
            RTLogger.log(this, "MIDI CONTINUE");
            listeners.forEach(l -> {l.update(Property.TRANSPORT,
                    JackTransportState.JackTransportRolling); });

        }
        else if (mode != Mode.Midi24) 
        	return;
        
        // process time
        if (ShortMessage.TIMING_CLOCK == stat) {
            pulse++;
            if (pulse == 1) {
                ticker = System.currentTimeMillis();
            }
            if (pulse == 25) {
                new Thread() {@Override public void run() {
                	listeners.forEach(listener -> {listener.update(Property.BEAT, ++beat);});
                }}.start();
                
            }
            if (pulse == 49) { // hopefully 2 beats will be more accurate than 1
                delta = System.currentTimeMillis() - ticker;
	                tempo = exTempo = Constants.toBPM(delta, 2);
	                new Thread() {@Override public void run() {
	                	listeners.forEach(listener -> {listener.update(Property.BEAT, ++beat);});
	                	listeners.forEach(l -> {l.update(Property.TEMPO, tempo); });
	                	gui.update();
	                	drummachine.tempoReceived(exTempo);
	                	// RTLogger.log(this, "TEMPO : " + tempo);
	                }}.start();
//                } // mode24
//                else {
//                	exTempo = Constants.toBPM(delta, 2);
//                	drummachine.tempoReceived(exTempo);
//                }
                pulse = 0;
            }
        }
        else
            RTLogger.log(this, "unknown beat buddy " + new Midi(midi));

	}

	
	public long computeNextPulse() {
	    return lastPulse + Constants.millisPerBeat(tempo * subdivision);
	}

	private void step() {

			// run the current beat
	        if (step % subdivision == 0) {
	            beat++;
//	        	new Thread( () -> {
	            // Console.info("step: " + step + " beat: " + step/2f + " count: " + count);
	        		listeners.forEach(listener -> {listener.update(Property.BEAT, beat);});
	        		//Console.info(beat + ((beat % measure == 0) ? "*" : ""));
	        		
	        		if (beat % getMeasure()== 0) {
	        			new ArrayList<TimeListener>(listeners).forEach(
	        					listener -> {listener.update(Property.BARS, beat / getMeasure());});
	        		}
//	        	}).start();
	        }
            listeners.forEach(listener -> {listener.update(Property.STEP, step);});
            for (BeatBox beatbox : sequencers)
                beatbox.process(step);
            step++;
            if (step == steps)
                step = 0;
	}

	@Override
    public void begin() {
	    lastPulse = System.currentTimeMillis();
	    if (Sequencer.getCurrent() != null)
	        Sequencer.getCurrent().setClock(this);
	    if (!active) {
        	active = true;
	    	gui.update();
        }
	    if (mode == Mode.Midi24) {
			drummachine.getQueue().offer(BeatBuddy.PAUSE_MIDI);
			return;
		}
	    step = 0;
		beat = -1;
	    step();
	    nextPulse = computeNextPulse();
//	    try {
//	    	if (gnome.isRunning()) gnome.stop();
//	    	if (3 == getMeasure())
//	    		gnome.setFile(new File("/home/judah/tracks/beatbuddy/midi_sources/BRUSHES BEATS/Brushes Song 4- 3_4 Shuffle Waltz/BRUSHES- beat 44- 3_4 Shuffle Waltz !!.mid"));
//	    	else if (4 == getMeasure()) {
//	    		gnome.setFile(new File("/home/judah/tracks/beatbuddy/midi_sources/BLUES/Blues Song 2/BLUES- beat 2b straight 8 ride!.mid"));
//	    	}
//	    	gnome.start();
//	    } catch (Exception e) {
//	    	RTLogger.warn(this, e);
//	    }
	}

	@Override
    public void end() {
		if (mode == Mode.Midi24) 
			drummachine.getQueue().offer(BeatBuddy.PAUSE_MIDI);
	    active = false;
	    gui.update();
	    Console.info(JudahClock.class.getSimpleName() + " end");
//	    if (gnome.isRunning()) gnome.stop();
	}

	public void reset() {
		end();
		step = 0;
        beat = -1;
	}
	
	@Override
	public boolean setTempo(float tempo2) {
		if (tempo2 < tempo || tempo2 > tempo) {
			tempo = tempo2;
			gui.update();
			listeners.forEach(l -> {l.update(Property.TEMPO, tempo);});
		}
		return true;
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

	@Override
    public void update(Property prop, Object value) {
	    if (Property.TEMPO == prop) {
	        setTempo((float)value);
	    }
	}

    public BeatBox getSequencer(int channel) {
        return sequencers.get(channel);
    }

    public void latch(Recorder loopA) {
    	latch(loopA, length * getMeasure());
    }
    
	public void latch(Recorder loopA, int beats) {
		if (loopA.hasRecording() && loopA.getRecordedLength() > 0) {
			tempo = Constants.computeTempo(loopA.getRecordedLength(), beats); 
			gui.update();
			listen(loopA);
			Console.info("Clock armed at " + tempo + " bpm form " + loopA.getName() + ")");
		}
	}

	void listen(final Recorder target) {
		if (listener != null) return;
		listener = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop.equals(TimeListener.Property.LOOP)) {
					step = 0;
					begin();
				}
				else if (Status.TERMINATED==value) {
					listener = null;
				}
			}
		};
		target.addListener(listener);
	}

	public void togglePlay() {
		if (isActive()) end();
		else begin();
	}

	public void setLength(int bars) {
		if (length != bars) {
			length = bars;
			RTLogger.log(this, "Latching to " + bars + " bars");
		}
	}
	
	public void synchronize(Recorder loop) {
		listeners.add(new LoopSynchronization(loop, length));
		RTLogger.log(this, "sync to " + loop.getName() + " " + length + " bars.");
	}

	public static boolean waiting(Recorder loop) {
		if (onDeck == null) return false;
		switch(onDeck) {
			case ERASE: 
				loop.erase();
				break;
			case SYNC:
				instance.synchronize(loop);
				break;
		}
		onDeck = null;
		return true;
	}

	public static void setOnDeck(SelectType syncType) {
		onDeck = syncType;
		RTLogger.log(getInstance(), "Waiting... (" + syncType.name() + " " + getInstance().length + " bars)");
	}

	public static void setMode(Mode mode) {
		JudahClock.mode = mode;
		if (instance.gui != null)
			instance.gui.update();
		RTLogger.log(instance, "Clock: " + mode);
	}

	public void toggleMode() {
		setMode(mode == Mode.Internal ? Mode.Midi24 : Mode.Internal);
	}
	
	@Override
	public int getMeasure() {
		return _measure;
	}
	public void setSubdivision(int sub) {
		subdivision = sub;
		_measure = steps / subdivision;
	}
	public void setSteps(int s) {
		steps = s;
		_measure = steps/subdivision;
	}
	
}


