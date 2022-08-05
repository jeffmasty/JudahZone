package net.judah.midi;

import static net.judah.api.Notification.Property.*;

import java.io.File;
import java.security.InvalidParameterException;
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
import net.judah.looper.Loop;
import net.judah.looper.SyncWidget;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.sequencer.Sequencer;
import net.judah.tracker.DrumTrack;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
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
	
	@Getter private static JudahClock instance;

	public static final Integer[] LENGTHS= {1, 1, 2, 2, 2, 3, 4, 4, 4, 4, 5, 6, 6, 7, 8, 8, 8, 8, 9, 10, 10, 11, 12, 12, 12, 13, 14, 15, 
			16, 16, 16, 16, 17, 18, 19, 20, 20, 21, 22, 23, 24, 24, 25, 26, 27, 28, 29, 30, 31, 32, 32, 32, 33, 34, 35, 36, 36, 48, 64};
	public static enum Mode { Internal, /**24 pulses per quarter note external midi clock*/ Midi24 };
	@Getter private Mode mode = Mode.Midi24;
	
    @Getter private boolean active;
    @Getter private float tempo = 89f;
    /** jack frames since transport start or frames between pulses */
    @Getter private long lastPulse = System.currentTimeMillis();
    @Getter private double interval = Constants.millisPerBeat(tempo) / MIDI_24;
    private long lastBeat = lastPulse;

    /** current beat */
    @Getter @Setter private static int beat = -1;
    /** current step */
	@Getter static private int step = 0;
    @Getter static private int steps = 16;
	@Getter static private int subdivision = 4;
	private static int _measure = steps / subdivision;
    /** current number of bars to record/computeTempo */
	@Getter private static int length = 4; 

	/** Loop Synchronization */
	private static SelectType onDeck;
	@Setter @Getter private static boolean loopSync = true;

    private static final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
	private static final float MAX_TEMPO = 300f; 

	private final JudahMidi midi;
	private TimeListener listener;
    private final ArrayList<TimeListener> listeners = new ArrayList<>();

	// @Getter private BeatBuddy drummachine = new BeatBuddy();
	private final JackPort clockOut;
	private int midiPulseCount;
	@Getter private final Track [] tracks;
    @Getter private Tracker tracker;
    
	public JudahClock(JudahMidi midiSystem) {
		
		midi = midiSystem;
		clockOut = midi.getClockOut();
		instance = this;
	    tracks = initTracks();
	    tracker = new Tracker(tracks);
	    setPriority(8);
	    start();
	}

	private Track[] initTracks() {
		
		DrumTrack drum1 = new DrumTrack(this, "Drum1", 9, midi.getCalfOut());
		DrumTrack drum2 = new DrumTrack(this, "Drum2", 9, midi.getCalfOut());
		DrumTrack drum3 = new DrumTrack(this, "Drum3", 9, midi.getFluidOut());
		DrumTrack drum4 = new DrumTrack(this, "Drum4", 9, midi.getFluidOut());
		
//		bass = new PianoTrack(this, "Bass", 0, midi.getCraveOut());
//		lead1 = new PianoTrack(this, "Lead1", 0, midi.getCalfOut());
//		lead2 = new PianoTrack(this, "Lead2", 0, midi.getFluidOut());
//		chords = new PianoTrack(this, "Chords", 0, midi.getFluidOut());

		
		Constants.timer(1500, () -> {
			drum1.setFile(new File(Track.DRUM_FOLDER, "try1"));
			// drum2.setFile(new File(Track.DRUM_FOLDER, "Hats2"));
			// drum3.setFile(new File(Track.DRUM_FOLDER, "Aux1"));
			// drum4.setFile(new File(Track.DRUM_FOLDER, "Drum2"));
			// bass.setFile(new File(Track.MELODIC_FOLDER, "Naima"));
			//lead.setFile(new File(Track.MELODIC_FOLDER, "BluesBass"));
			//chords.setFile(new File(Track.MELODIC_FOLDER, "CarveOut"));
		});
		
		return new Track[] {drum1, drum2, drum3, drum4 /*bass, lead1, lead2, chords */}; 
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
        
        int increment = 0;
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
				JackMidi.eventWrite(clockOut, increment++, MIDI_RT_CLOCK, CLOCK_SZ);

    		// supports 2,3,4,6,8 subdivision
			if (0 == midiPulseCount % (MIDI_24 / (steps / subdivision))) {
				step();
			}
    	}
    	
	}

	@Override
	public boolean setTempo(float tempo2) {
		if (tempo2 > MAX_TEMPO) {
			RTLogger.warn(this, tempo2 + " ignored.");
			return true;
		}
		if (tempo2 == tempo)
			return true;

		tempo = tempo2;
		if (mode == Mode.Internal) {
			// BeatBuddy.setTempo(tempo);
		}
		interval = Constants.millisPerBeat(tempo) / MIDI_24;
		shout(TEMPO, tempo);
		return true;
	}
	
	private void step() {
		
		if (!active) {
	        step++;
	        if (step == steps) {
	            step = 0;
	        }
			shout(STEP, step);
			return;
		}
		
		shout(STEP, step);
		
		new Thread( () -> {
			for (Track track : tracks)
				track.step(step);} ).run();

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

	
	/** receive clock message from external source in real-time */
	public void processTime(byte[] midi) throws JackException {
		if (midi.length > 2) return; // ignore external sequencer for now
		
		int stat = 0;
        if (midi.length == CLOCK_SZ)
            stat = midi[0] & 0xFF;
        else if (midi.length == 2) {
            stat = midi[1] & 0xFF;
            stat = (stat << 8) | (midi[0] & 0xFF);
        }

        if (ShortMessage.START == stat) {
            RTLogger.log(this, "MIDI24 START!");
            begin();
            JudahMidi.synchronize(midi);
        }

        else if (ShortMessage.STOP == stat) {
            RTLogger.log(this, "MIDI24 STOP");
            end();
            JackMidi.eventWrite(clockOut, 1, midi, CLOCK_SZ);
        }

        else if (ShortMessage.CONTINUE == stat) {
            RTLogger.log(this, "MIDI24 CONTINUE");
            shout(TRANSPORT, JackTransportState.JackTransportRolling); 
            waiting(JudahZone.getLooper().getLoopA());
            JudahMidi.synchronize(midi);
        }
        
        // process external time
        if (ShortMessage.TIMING_CLOCK == stat) {
        	if (clockOut != null) {
    			//fwd midi24 clock tick 
        		midiPulseCount++;
				JackMidi.eventWrite(clockOut, JudahMidi.getInstance().getTicker(), MIDI_RT_CLOCK, CLOCK_SZ);
				// supports 2,3,4,6,8 subdivision
				if (0 == midiPulseCount % (MIDI_24 / (steps / subdivision))) {
					step();
					if (step == 0) {
						long now = System.currentTimeMillis();
						int tempo2 = Math.round(4 * Constants.bpmPerBeat(now - lastPulse));
						if (tempo != tempo2) {
							tempo = tempo2;
							shout(Property.TEMPO, tempo);
						}
						lastPulse = now;
					}
				}
        	}
        }
        // else RTLogger.log(this, "unknown clock midi signal " + new Midi(midi));

	}
	

	@Override
    public void begin() {
	    active = true;
	    if (Sequencer.getCurrent() != null)
	        Sequencer.getCurrent().setClock(this);

	    shout(TRANSPORT, JackTransportState.JackTransportStarting);
	    lastPulse = lastBeat = System.currentTimeMillis();
	    midiPulseCount = 0;
	    step = 0;
		beat = -1;
		
		step();
	}

	@Override
    public void end() {
		// if (mode == Mode.Midi24) 
			// BeatBuddy.getQueue().offer(BeatBuddy.PAUSE_MIDI);
	    active = false;
	    shout(TRANSPORT, JackTransportState.JackTransportStopped);
	}

	public void reset() {
		end();
		step = 0;
        beat = -1;
	    lastPulse = lastBeat = System.currentTimeMillis();
	    //shout(BEAT, beat);
	    RTLogger.log(this, "reset");
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

    public void latch(Loop loopA) {
    	latch(loopA, length * getMeasure());
    }
    
	public void latch(Loop loop, int beats) {
		
		if (loop.hasRecording() && loop.getRecordedLength() > 0) {
			listen(loop); 
			// tempo tweak to loop
			// float tempo = Constants.computeTempo(loop.getRecordedLength(), beats);
			// RTLogger.log(this, "[" + loop.getName() + "] " + loop.getRecordedLength()/1000f + "s. from " + 
			//				+ getTempo() + " to " + tempo + " bpm ");
			// setTempo(tempo);
		}
	}

	void listen(final Loop target) {
		if (listener != null) return;
		listener = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop.equals(LOOP)) { // TODO
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
		midi.getGui().length(bars);
		MainFrame.update(JudahZone.getLooper().getLoopA());
	}
	
	public SyncWidget getSynchronized() {
		for (TimeListener l : listeners)
			if (l instanceof SyncWidget)
				return (SyncWidget)l;
		return null;
	}
	
	public static boolean waiting(Loop loop) {
		if (onDeck == null) return false;
		switch(onDeck) {
			case ERASE: 
				loop.erase();
				break;
			case SYNC:
				loop.getSync().syncUp();
				break;
		}
		onDeck = null;
		return true;
	}

	public static void setOnDeck(SelectType syncType) {
		onDeck = syncType;
		RTLogger.log(getInstance(), "Waiting... (" + syncType.name() + " " + length + " bars)");
	}

	
	
	public void setMode(Mode mode) {
		this.mode = mode;
		if (mode == Mode.Internal)
			lastPulse = System.currentTimeMillis();
		RTLogger.log(this, "Clock: " + mode);
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

	public static String toString(JackPosition position) {
		return position.getBeat() + "/" + position.getBar() + " " + position.getTick() + "/" + position.getTicksPerBeat();
	}

	public int indexOf(Track track) {
		for (int i = 0; i < tracks.length; i++)
			if (tracks[i] == track)
				return i;
		throw new InvalidParameterException("" + track);
	}

}


