package net.judah.clock;

import static net.judah.beatbox.GMDrum.AcousticSnare;
import static net.judah.beatbox.GMDrum.BassDrum;
import static net.judah.beatbox.GMDrum.ClosedHiHat;
import static net.judah.beatbox.GMDrum.OpenHiHat;
import static net.judah.settings.MidiSetup.OUT.CALF_OUT;
import static net.judah.settings.MidiSetup.OUT.CRAVE_OUT;
import static net.judah.settings.MidiSetup.OUT.DRUMS_OUT;
import static net.judah.settings.MidiSetup.OUT.SYNTH_OUT;
import static net.judah.tracks.Track.Type.MIDI_DRUM;
import static net.judah.tracks.Track.Type.MIDI_MONO;
import static net.judah.tracks.Track.Type.MIDI_POLY;
import static net.judah.tracks.Track.Type.STEP_DRUM;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.ShortMessage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPosition;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.beatbox.BeatBox;
import net.judah.clock.LoopSynchronization.SelectType;
import net.judah.looper.Recorder;
import net.judah.sequencer.Sequencer;
import net.judah.tracks.MidiTrack;
import net.judah.tracks.StepTrack;
import net.judah.tracks.Track;
import net.judah.util.Constants;
import net.judah.util.RTLogger;
import net.judah.util.RainbowFader;
import net.judah.util.Size;

public class JudahClock extends Thread implements TimeProvider, TimeListener {

	@Getter private static JudahClock instance = new JudahClock();

	@Data
	class Notification {
		final Property prop;
		final Object value;
	}
	
	public static final int[] LENGTHS= {1, 2, 4, 6, 8, 10, 12, 16, 20, 22, 24, 28, 32, 36};
	public static enum Mode { Internal, /**24 pulses per quarter note external midi clock*/ Midi24 };
	@Getter private static Mode mode = Mode.Internal;
	
    
	@Getter private final ArrayList<BeatBox> sequencers = new ArrayList<>();
	
	@Getter private BeatBuddy drummachine = new BeatBuddy();
    @Getter private final ClockGui gui;
    private final ArrayList<TimeListener> listeners = new ArrayList<>();
    private static final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>(); 
    private TimeListener listener;

    @Getter private boolean active;
    @Getter private float tempo = 90f;
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

	/** jack frames since transport start or frames between pulses */
    @Getter private long lastPulse;

	/** Loop Synchronization */
	private static SelectType onDeck;
	@Setter @Getter private static boolean loopSync = true;
	
	// listen to incoming midi clock
    private int pulse;
    private long ticker;

    // under construction
	@Getter private JScrollPane overview;
	private Track bass, chords, lead, drums, bdrum, sdrum, hihat1, hihat2, 
	drums2, drums3, bass2, chrds2, lead2; 	// trans, arp1, arp2, arp3,
	@Getter private Track [] tracks;

	static final byte[] MIDI_RT_CLOCK 	 = new byte[] {(byte)ShortMessage.TIMING_CLOCK};
	static final byte[] MIDI_RT_START    = new byte[] {(byte)ShortMessage.START}; 
	static final byte[] MIDI_RT_CONTINUE = new byte[] {(byte)ShortMessage.CONTINUE};
	static final byte[] MIDI_RT_STOP     = new byte[] {(byte)ShortMessage.STOP};
	static final int CLOCK_SZ = 1;
	static final float MIDI_24 = 24;

	@Getter @Setter private JackPort clockOut;
	private int midiPulseCount;
	@Getter @Setter private boolean syncCrave;
    
	private JudahClock() {
	    for (int i = 0; i < 16; i++) {
	        sequencers.add(new BeatBox(this, i));
	    }
	    
	    gui = new ClockGui(this);
		tempGui();
	    setPriority(8);
	    start();
	}

	@Override
	public void run() {
		try {
			while (true) {
				Notification n = notifications.take();
				new ArrayList<TimeListener>(listeners).forEach(
						listener -> listener.update(n.prop, n.value));
			}
		} catch (Exception e) {
			RTLogger.warn(this, e);		
		}
	}

	private void shout(Property prop, Object value) {
		notifications.offer(new Notification(prop, value));
	}
	
	public void process() throws JackException {
        if (mode != Mode.Internal) return;
        
    	final long current = System.currentTimeMillis();
    	final long nextPulse = lastPulse + Math.round(
    			Constants.millisPerBeat(JudahClock.getInstance().getTempo()) / MIDI_24);

    	if (current >= nextPulse) {
    		if (clockOut != null) 
    			//send clock tick 
				JackMidi.eventWrite(clockOut, 0, MIDI_RT_CLOCK, CLOCK_SZ);
    		
    		midiPulseCount++;
    		if (midiPulseCount == MIDI_24)
    			midiPulseCount = 0;
    		// supports 2,3,4,6,8 subdivision
			if (active && 0 == midiPulseCount % (MIDI_24 / (steps / subdivision))) {
				step();
			}
			lastPulse = nextPulse;
    	}
    	
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
            shout(Property.TRANSPORT, JackTransportState.JackTransportStarting); 
            beat = 0;
            pulse = 0;
            waiting(JudahZone.getLooper().getLoopA());
            //ticker = System.currentTimeMillis() - Constants.millisPerBeat(tempo);
            //shout(Property.BEAT, 0);
            shout(Property.BARS, 0);
        }

        else if (ShortMessage.STOP == stat) {
            RTLogger.log(this, "MIDI24 STOP");
            shout(Property.TRANSPORT, JackTransportState.JackTransportStopped);
        }

        else if (ShortMessage.CONTINUE == stat) {
            RTLogger.log(this, "MIDI24 CONTINUE");
            shout(Property.TRANSPORT, JackTransportState.JackTransportRolling); 
            waiting(JudahZone.getLooper().getLoopA());
        }
        
        if (mode != Mode.Midi24) 
        	return;
        
        // process time
        if (ShortMessage.TIMING_CLOCK == stat) {
            pulse++;
            if (pulse == 1) 
                ticker = System.currentTimeMillis();
            
            if (pulse == 25) {
            	shout(Property.BEAT, ++beat);
            	if (beat % getMeasure()== 0) 
            		shout(Property.BARS, beat / getMeasure());
            	pulse = 0;
            	tempo = Constants.toBPM(System.currentTimeMillis() - ticker, 1);
            	drummachine.tempoReceived(tempo);
            }
            
            if (pulse == 49) { // hopefully 2 beats will be more accurate than 1
                tempo = exTempo = Constants.toBPM(System.currentTimeMillis() - ticker, 2);
                shout(Property.BEAT, ++beat);
                if (beat % getMeasure()== 0) 
                	shout(Property.BARS, beat / getMeasure());
                shout(Property.TEMPO, tempo); 
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

	
	private void step() {

		shout(Property.STEP, step);
        for (BeatBox beatbox : sequencers)
            beatbox.step(step);

        if (step % subdivision == 0) {
        	// run the current beat
        	shout(Property.BEAT, ++beat);
    		if (beat % getMeasure()== 0) 
    			shout(Property.BARS, beat / getMeasure());
        }
        
        step++;
        if (step == steps)
            step = 0;
	}

	@Override
    public void begin() {
	    active = true;
	    if (Sequencer.getCurrent() != null)
	        Sequencer.getCurrent().setClock(this);

	    shout(Property.TRANSPORT, JackTransportState.JackTransportStarting);
	    if (mode == Mode.Midi24) {
			BeatBuddy.getQueue().offer(BeatBuddy.PAUSE_MIDI);
			return;
		}
	    lastPulse = System.currentTimeMillis();
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
	    RTLogger.log(this, "start");
	}

	@Override
    public void end() {
		if (mode == Mode.Midi24) 
			BeatBuddy.getQueue().offer(BeatBuddy.PAUSE_MIDI);
	    active = false;
	    shout(Property.TRANSPORT, JackTransportState.JackTransportStopped);
	    if (syncCrave && clockOut != null) 
	    	try {
	    		JackMidi.eventWrite(clockOut, 0, MIDI_RT_STOP, CLOCK_SZ);
	    	} catch (Exception e) {
	    		RTLogger.warn(this, e);
	    	}
	    RTLogger.log(this, "end");
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
			if (mode == Mode.Internal)
				BeatBuddy.setTempo(tempo);
			shout(Property.TEMPO, tempo);
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
    
	public void latch(Recorder loop, int beats) {
		if (loop.hasRecording() && loop.getRecordedLength() > 0) {
			setTempo(Constants.computeTempo(loop.getRecordedLength(), beats));
			listen(loop);
			RTLogger.log(this, "Clock armed at " + tempo + " bpm form " + loop.getName() + ")");
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
		if (isActive()) 
			end();
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
		RTLogger.log(getInstance(), "Waiting... (" + syncType.name() + " " + length + " bars)");
	}

	public static void setMode(Mode mode) {
		JudahClock.mode = mode;
		if (instance.gui != null)
			instance.gui.update(null, null);
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

	private void tempGui() {
				//keyboard = new Track(this, "keys", CONTROLLER, 0, CRAVE_OUT);
		bass = new MidiTrack(this, "bass", MIDI_MONO, 1, CRAVE_OUT);
		chords = new MidiTrack(this, "chords", MIDI_POLY, 2, SYNTH_OUT);
		lead = new MidiTrack(this, "lead", MIDI_MONO, 3, SYNTH_OUT);
		drums = new MidiTrack(this, "drums", MIDI_DRUM, 9, CALF_OUT);
		bdrum = new StepTrack(this, "bdrum", STEP_DRUM, BassDrum.toByte(), DRUMS_OUT);
		sdrum = new StepTrack(this, "sdrum", STEP_DRUM, AcousticSnare.toByte(), DRUMS_OUT);
		hihat1 = new StepTrack(this, "hihat1", STEP_DRUM, ClosedHiHat.toByte(), DRUMS_OUT);
		hihat2 = new StepTrack(this, "hihat2", STEP_DRUM, OpenHiHat.toByte(), DRUMS_OUT);
		drums2 = new StepTrack(this, "drums2", STEP_DRUM, CALF_OUT);
		drums3 = new MidiTrack(this, "drums3", MIDI_DRUM, 9, DRUMS_OUT);
		bass2 = new MidiTrack(this, "bass2", MIDI_MONO, 4, CALF_OUT);
		chrds2 = new MidiTrack(this, "chrds2", MIDI_POLY, 5, CALF_OUT);
		lead2 = new MidiTrack(this, "lead2", MIDI_MONO, 6, CALF_OUT);
		
		bass.setFile(new File("metronome/BoogieWoogie.mid"));
		drums.setFile(new File("metronome/JudahZone.mid"));
		chords.setFile(new File("metronome/323_Major_4-4_I_IV_II_V.mid"));
		lead.setFile(new File("metronome/44_Minor_4-4_i_-III_iv_V.mid"));
		hihat1.setFile(new File("patterns/HiHats"));
		drums2.setFile(Constants.defaultDrumFile); // load a default drum pattern
		
		tracks = new Track[] { bass, chords, lead, drums, bdrum, sdrum, hihat1, hihat2, 
			drums2, drums3, bass2, chrds2, lead2 }; // trans, arp1, arp2, arp3, 
	    
		JPanel canvas = new JPanel();
		canvas.setLayout(new BoxLayout(canvas, BoxLayout.Y_AXIS));
		Dimension sz = new Dimension(Size.WIDTH_SONG-100, 50);
		for (int idx = 0; idx < tracks.length; idx++) {
			Track track = tracks[idx];
			Color rainbow = RainbowFader.chaseTheRainbow((int)(idx/(float)tracks.length * 100));
			track.setBorder(BorderFactory.createLineBorder(rainbow));
			track.setPreferredSize(sz);
			canvas.add(track);
		}
		
		overview = new JScrollPane(canvas);
		overview.setPreferredSize(new Dimension(Size.WIDTH_SONG, Size.HEIGHT_FRAME - Size.HEIGHT_FRAME));
		overview.getVerticalScrollBar().setUnitIncrement(25);
        overview.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);  
		overview.doLayout();

	}


}


