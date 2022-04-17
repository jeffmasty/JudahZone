package net.judah.clock;

import static net.judah.api.Notification.Property.*;
import static net.judah.settings.MidiSetup.OUT.*;
import static net.judah.tracks.Track.Type.*;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPosition;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.beatbox.BeatBox;
import net.judah.clock.LoopSynchronization.SelectType;
import net.judah.looper.Recorder;
import net.judah.sequencer.Sequencer;
import net.judah.tracks.*;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class JudahClock extends Thread implements TimeProvider {
	static final byte[] MIDI_RT_CLOCK 	 = new byte[] {(byte)ShortMessage.TIMING_CLOCK};
	static final byte[] MIDI_RT_START    = new byte[] {(byte)ShortMessage.START}; 
	static final byte[] MIDI_RT_CONTINUE = new byte[] {(byte)ShortMessage.CONTINUE};
	static final byte[] MIDI_RT_STOP     = new byte[] {(byte)ShortMessage.STOP};
	static final int CLOCK_SZ = 1;
	public static final float MIDI_24 = 24;
	
	@Getter public static final String[] timeSignatures = new String[] {
			"4/4", "Swing", "6/8", "Waltz", "Polka", "5/4"
	};
	
	@Getter private static JudahClock instance = new JudahClock();

	public static final Integer[] LENGTHS= {1, 2, 4, 6, 8, 10, 12, 16, 20, 22, 24, 28, 32, 36};
	public static enum Mode { Internal, /**24 pulses per quarter note external midi clock*/ Midi24 };
	@Getter private static Mode mode = Mode.Internal;
	
    @Getter private boolean active;
    @Getter private float tempo = 89.3134f;
    /** jack frames since transport start or frames between pulses */
    @Getter private long lastPulse = System.currentTimeMillis();
    @Getter private double interval = Constants.millisPerBeat(tempo) / MIDI_24;
    private long lastBeat = lastPulse;

    /** current beat */
    @Getter private static int beat = -1;
    /** current step */
	@Getter static private int step = 0;
    @Getter static private int steps = 16;
	@Getter static private int subdivision = 4;
	private static int _measure = steps / subdivision;
    /** current number of bars to record/computeTempo */
	@Getter private static int length = 4; 
	private float exTempo; // tempo of external clock when running on internal clock

	/** Loop Synchronization */
	private static SelectType onDeck;
	@Setter @Getter private static boolean loopSync = true;
	// listen to incoming midi clock
    private int pulse;
    private long ticker;

    private final ArrayList<TimeListener> listeners = new ArrayList<>();
    private static final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
	private static final float MAX_TEMPO = 300; 
    private TimeListener listener;

    @Getter private final ArrayList<BeatBox> sequencers = new ArrayList<>();
	@Getter private BeatBuddy drummachine = new BeatBuddy();
	@Getter @Setter private JackPort clockOut;
	private int midiPulseCount;
	@Getter @Setter private boolean syncCrave;
	
	@Getter private final ClockGui gui;
	private Track drum1, drum2, drum3, beats, bass, lead, chords, kit;
	@Getter private final Track [] tracks;
    @Getter private final Tracker tracker;
    
	private JudahClock() {
	    for (int i = 0; i < 16; i++) {
	        sequencers.add(new BeatBox(this, i));
	    }
	    tracks = initTracks();
	    tracker = new Tracker(tracks);
	    gui = new ClockGui(this);
	    setPriority(8);
	    start();
	}

	private Track[] initTracks() {
		drum1 = new StepTrack(this, new BassAndSnare(), new File("patterns"));
		drum2 = new StepTrack(this, new File("patterns/Hats2"), new File("patterns"));
		drum3 = new StepTrack(this, new File("patterns/Aux1"), new File("patterns"));
		kit = new KitTrack(this, new File("patterns/Drum1"), new File("patterns"));
		bass = new MidiTrack(this, "Crave", MIDI_MONO, CRAVE_OUT, new File("midi/bass"));
		lead = new MidiTrack(this, "Lead", MIDI_MONO, SYNTH_OUT, new File("midi/lead"));
		chords = new MidiTrack(this, "Chords", MIDI_POLY, SYNTH_OUT, new File("midi/chords"));
		beats = new MidiTrack(this, "Beats", MIDI_DRUM, CALF_OUT, new File("metronome"));
		
		bass.setFile(new File("midi/bass/CmEbFmG7.mid"));
		lead.setFile(new File("midi/lead/CFmDmG7.mid"));
		chords.setFile(new File("midi/chords/chop.mid"));
		beats.setFile(new File("metronome/Rock1.mid"));

		return new Track[] {drum1, drum2, drum3, kit, bass, lead, chords, beats}; 
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				Notification n = notifications.take();
				new ArrayList<TimeListener>(listeners).forEach(
						listener -> {
							if (listener!=null)
								listener.update(n.prop, n.value);
						});
			}
		} catch (Exception e) {
			RTLogger.warn(this, e);		
		}
	}

	private void shout(Property prop, Object value) {
		notifications.offer(new Notification(prop, value));
	}
	
	/** Run the Clock in real-time */
	public void process() throws JackException {
        if (mode != Mode.Internal) return;
        
        long current = System.currentTimeMillis();
        
        if (current - lastBeat >= Math.floor(midiPulseCount * interval)) {
		
        	lastPulse = current;
        	midiPulseCount++;
    		if (midiPulseCount > MIDI_24) {
    			midiPulseCount = 1;
    			lastBeat = current;
    		}

        	if (clockOut != null) 
    			//send midi24 clock tick 
				JackMidi.eventWrite(clockOut, 0, MIDI_RT_CLOCK, CLOCK_SZ);

    		// supports 2,3,4,6,8 subdivision
			if (0 == midiPulseCount % (MIDI_24 / (steps / subdivision))) {
				step();
			}
    	}
    	
	}

	@Override
	public boolean setTempo(float tempo2) {
		if (tempo > MAX_TEMPO) {
			RTLogger.warn(this, tempo2 + " ignored.");
			return true;
		}
		if (tempo2 == tempo)
			return true;

		tempo = tempo2;
		if (mode == Mode.Internal)
			BeatBuddy.setTempo(tempo);
		interval = Constants.millisPerBeat(tempo) / MIDI_24;
		shout(TEMPO, tempo);
		return true;
	}
	
	private void step() {
		if (mode == Mode.Internal)
			gui.blink(step % subdivision == 0); 
		
		if (!active) {
	        step++;
	        if (step == steps)
	            step = 0;
			return;
		}
		
		shout(STEP, step);
        for (BeatBox beatbox : sequencers)
            beatbox.step(step);

        if (step % subdivision == 0) {
        	// run the current beat
        	shout(BEAT, ++beat);
    		if (beat % getMeasure() == 0) 
    			shout(BARS, Math.round(beat / getMeasure()));
        }
        
        step++;
        if (step == steps)
            step = 0;
	}

	
	public void processTime(byte[] midi) {
		if (midi.length > 2) return; // ignore external sequencer for now
		
		int stat = 0;
        if (midi.length == 1)
            stat = midi[0] & 0xFF;
        else if (midi.length == 2) {
            stat = midi[1] & 0xFF;
            stat = (stat << 8) | (midi[0] & 0xFF);
        }
		//else {
		//    stat = 0;
		//    // RTLogger.log(this, midi.length + " " + new Midi(midi));
		//}

        if (ShortMessage.START == stat) {
            RTLogger.log(this, "MIDI24 START!");
            shout(TRANSPORT, JackTransportState.JackTransportStarting); 
            beat = 0;
            pulse = 0;
            waiting(JudahZone.getLooper().getLoopA());
            //ticker = System.currentTimeMillis() - Constants.millisPerBeat(tempo);
            //shout(Property.BEAT, 0);
            shout(BARS, 0);
        }

        else if (ShortMessage.STOP == stat) {
            RTLogger.log(this, "MIDI24 STOP");
            shout(TRANSPORT, JackTransportState.JackTransportStopped);
        }

        else if (ShortMessage.CONTINUE == stat) {
            RTLogger.log(this, "MIDI24 CONTINUE");
            shout(TRANSPORT, JackTransportState.JackTransportRolling); 
            waiting(JudahZone.getLooper().getLoopA());
        }
        
        if (mode != Mode.Midi24) 
        	return;
        
        // process external time
        if (ShortMessage.TIMING_CLOCK == stat) {
            pulse++;
            if (pulse == 1) 
                ticker = System.currentTimeMillis();
            
            if (pulse == 25) {
            	shout(BEAT, ++beat);
            	if (beat % getMeasure()== 0) 
            		shout(BARS, beat / getMeasure());
            	pulse = 0;
            	tempo = Constants.toBPM(System.currentTimeMillis() - ticker, 1);
            	drummachine.tempoReceived(tempo);
            }
            
            if (pulse == 49) { // hopefully 2 beats will be more accurate than 1
                tempo = exTempo = Constants.toBPM(System.currentTimeMillis() - ticker, 2);
                shout(BEAT, ++beat);
                if (beat % getMeasure()== 0) 
                	shout(BARS, beat / getMeasure());
                shout(TEMPO, tempo); 
                drummachine.tempoReceived(exTempo);
				//        } // mode24
				//        else {
				//        	exTempo = Constants.toBPM(delta, 2);
				//        	drummachine.tempoReceived(exTempo);
				//        }
                pulse = 0;
            }
    		
        }
        // else RTLogger.log(this, "unknown beat buddy " + new Midi(midi));

	}
	

	@Override
    public void begin() {
	    active = true;
	    if (Sequencer.getCurrent() != null)
	        Sequencer.getCurrent().setClock(this);

	    shout(TRANSPORT, JackTransportState.JackTransportStarting);
	    if (mode == Mode.Midi24) {
			BeatBuddy.getQueue().offer(BeatBuddy.PAUSE_MIDI);
			return;
		}
	    lastPulse = lastBeat = System.currentTimeMillis();
	    midiPulseCount = 0;
	    step = 0;
		beat = -1;
		
		step();
	    
		if (syncCrave && clockOut != null) 
		    try {
		    	JackMidi.eventWrite(clockOut, 0, MIDI_RT_START, CLOCK_SZ);
		    } catch (Exception e) {
				RTLogger.warn(this, e);
			}
	}

	@Override
    public void end() {
		if (mode == Mode.Midi24) 
			BeatBuddy.getQueue().offer(BeatBuddy.PAUSE_MIDI);
	    active = false;
	    shout(TRANSPORT, JackTransportState.JackTransportStopped);
	    if (syncCrave && clockOut != null) 
	    	try {
	    		JackMidi.eventWrite(clockOut, 0, MIDI_RT_STOP, CLOCK_SZ);
	    	} catch (Exception e) {
	    		RTLogger.warn(this, e);
	    	}
	}

	public void reset() {
		end();
		step = 0;
        beat = -1;
	    lastPulse = lastBeat = System.currentTimeMillis();
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

    public BeatBox getSequencer(int channel) {
        return sequencers.get(channel);
    }

    public void latch(Recorder loopA) {
    	latch(loopA, length * getMeasure());
    }
    
	public void latch(Recorder loop, int beats) {
		if (loop.hasRecording() && loop.getRecordedLength() > 0) {
			listen(loop);
			// setTempo(Constants.computeTempo(loop.getRecordedLength(), beats));
			// RTLogger.log(this, "Clock armed at " + tempo + " bpm form " + loop.getName() + ")");
		}
	}

	void listen(final Recorder target) {
		if (listener != null) return;
		listener = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop.equals(LOOP)) {
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
		if (isActive()) 
			end();
		else begin();
	}

	public void setLength(int bars) {
		if (length == bars) 
			return;
		length = bars;
		MainFrame.update(JudahZone.getLooper().getLoopA());
		getGui().sync(bars);
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
		RTLogger.log(getInstance(), "Waiting... (" + syncType.name() + " " + length + " bars)");
	}

	public static void setMode(Mode mode) {
		JudahClock.mode = mode;
		if (mode == Mode.Internal)
			instance.lastPulse = System.currentTimeMillis();
		instance.getGui().update(null, null);
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

	public static String display() {
		if (!instance.active) return "" + beat;
		return beat / _measure + "/" + beat % _measure;
		
	}
	
	public static String toString(JackPosition position) {
		return position.getBeat() + "/" + position.getBar() + " " + position.getTick() + "/" + position.getTicksPerBeat();
	}

}


