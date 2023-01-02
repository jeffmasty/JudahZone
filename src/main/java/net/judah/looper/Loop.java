package net.judah.looper;

import static net.judah.api.AudioMode.*;
import static  net.judah.api.Notification.Property.LOOP;
import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Engine;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.synth.JudahSynth;
import net.judah.util.Constants;
import net.judah.util.RTLogger;
import net.judah.widgets.LoopWidget;
import net.judah.widgets.SyncWidget;

public class Loop extends AudioTrack {

	protected static final float LINE_BOOST = 3f;
	protected static final float SYNTH_BOOST = 3f;
	protected static final float DRUM_BOOST = 4.5f;
	
	protected final Looper looper;
	@Getter protected final JudahClock clock;
    protected final Zone sources;
    protected final Memory memory;

    @Getter protected Loop primary;
    @Getter private int loopCount;
    protected SyncWidget sync;
    @Setter protected LoopWidget feedback;
    protected int syncCounter;
//    protected final ArrayList<TimeListener> listeners = new ArrayList<>();
    @Getter protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
    protected long _start;
    
    protected final float[] workL = new float[bufSize];
    protected final float[] workR = new float[bufSize];
    protected final FloatBuffer bufL = FloatBuffer.wrap(workL);
    protected final FloatBuffer bufR = FloatBuffer.wrap(workR);
    protected final FloatBuffer[] buffer = new FloatBuffer[] {bufL, bufR};
    
    public Loop(String name, Looper loops, Zone sources, String icon, Type type, JudahClock clock) {
    	super(name, type);
    	this.looper = loops;
    	this.sources = sources;
    	this.clock = clock;
    	memory = new Memory(Constants.STEREO, bufSize);
    	leftPort = looper.getLeft();
    	rightPort = looper.getRight();
    	if (icon != null)
    		setIcon(Icons.get(icon));
    }
    
    public AudioMode isRecording() {
        return isRecording.get();
    }
    
    @Override public void clear() {
    	isRecording.compareAndSet(RUNNING, STOPPING);
        tapeCounter.set(0);
        recording = null;
        length = null;
        isRecording.set(NEW);
        recording = null;
        active = false;
        MainFrame.update(this);
        if (feedback != null)
        	MainFrame.update(feedback);
    }

    @Override
	public void setRecording(Recording music) {
    	isRecording.set(STOPPED);
    	super.setRecording(music);
    }
    
    public void delete() {
    	if (recording == null) return;
    	new Thread(() -> {
	    	if (recording != null) 
	    		recording.close();
	    	length = 0;
	    	recording.clear();
	    	active = false;
	    	RTLogger.log(this, "Deleted: " + getName());
	    	MainFrame.update(this);
    	}).start();
    }
    
    /** keep length, erase music */
    public void erase() {
    	if (recording == null) return;
    		record(false);
	    	new Thread( () -> {
	    		Constants.sleep(5);
	    		for (int i = 0; i < recording.size(); i++)
	    			recording.set(i, new float[Constants.STEREO][Constants.bufSize()]);
	    		active = false;
	    		RTLogger.log(this, "Erased: " + getName());
	    		MainFrame.update(this);
	    		
	    	}).start(); 
    }
    
    public void record(final boolean active) {
        AudioMode mode = isRecording.get();

        if (active && (recording == null || mode == NEW)) {
            recording = new Recording(true); // threaded to accept live stream
            isRecording.set(STARTING);
            _start = System.currentTimeMillis();
//            new ArrayList<TimeListener>(listeners).forEach(
//            		listener -> {listener.update(LOOP, Status.INITIALISING);});
		} else if (active) {
            isRecording.set(STARTING);
            this.active = true;  // overdub
        } else if (mode == RUNNING && !active) {
        	endRecord();
        }
    }

	private void endRecord() {
		isRecording.set(STOPPED);
		active = true;
		if (sync != null)
			sync.syncDown();
		if (length == null && recording != null) { // Initial Recording 
			length = recording.size();
			final int time = length;
			new Thread(() -> { // cut tape for other loopers
				looper.setRecordedLength(System.currentTimeMillis() - _start, this);
            	for (Loop loop : looper) 
        			if (loop != this && !loop.hasRecording()) 
            			loop.setRecording(new Recording(time));
            	
            	RTLogger.log(this, name + " cut tape " + looper.getRecordedLength() / 1000f + " seconds");
            }).start();
		}
        MainFrame.update(this);
        
	}
	
	/** in Real-Time */
	@Override public void readRecordedBuffer() {
    	int updated = tapeCounter.get();
    	
        updated++;
        if (updated >= recording.size()) 
            updated = 0;
        tapeCounter.set(updated);
        if (updated == 1) {
        		new Thread() { @Override public void run() {
        			
        			for (int i = 0; i < looper.getListeners().size(); i++)
	                    looper.getListeners().get(i).update(LOOP, this);
        		}}.start();
        }
	    if (updated == 2 && this == looper.getPrimary()) 
	    	looper.checkOnDeck();
	    // peek ahead/latency
	    updated++;
	    if (updated >= recording.size())
	    	updated = 0;
	    recordedBuffer = recording.get(updated);
        
    }

	public void duplicate() {
		new Thread(()-> {
			Recording source = getRecording();
			Recording dupe =  new Recording(source, 2, source.isListening());
			int offset = source.size();
			float[][] sound;
			for (int i = 0; i < offset; i++) {
				sound = source.elementAt(i);
				dupe.set(i, sound);
				dupe.set(i + offset, sound);
			}
			setRecording(dupe); 
			RTLogger.log(this, name + " duplicated (" + recording.size() + " frames)");
		}).start();
	}

//    // ---- Sync Section ----------------
//    @Override public void addListener(TimeListener l) {
//        if (!listeners.contains(l)) {
//            listeners.add(l);
//            if (l instanceof Channel) {
//            	MainFrame.update(l);
//            }
//        }
//    }
//
//    @Override public void removeListener(TimeListener l) {
//        listeners.remove(l);
//    }
//    
    public SyncWidget getSync() {
    	if (sync == null)
    		 sync = new SyncWidget(this, JudahZone.getClock());
    	return sync;
    }

    public void trigger() {
		if (isRecording() == AudioMode.RUNNING) {
			if (this == looper.getLoopB() && !hasRecording() && sync != null) {
				sync.bSync(SyncWidget.BSYNC_DOWN);
			}
			else {
				record(false);
				if (sync != null)
					sync.syncDown();
			}
		}
		else if (!hasRecording()) {
			// if sync remove sync else sync
			if (clock.getListeners().contains(sync))
				getSync().syncDown();
			else {
				if (this == looper.getLoopB())
					getSync().bSync(SyncWidget.BSYNC_UP);
				else if (getType() == Type.FREE) 
					record(true);
				else getSync().syncUp(); {
					if (this == looper.getSoloTrack())
						looper.getLoopA().getSync().syncUp();
					else if (looper.getSoloTrack().isSolo())
						looper.getSoloTrack().getSync().syncUp();
				}
			}
		}
		else 
			record(isRecording() != AudioMode.RUNNING);
	}
    
    ////////////////////////////////////
    //     Process Realtime Audio     //
    ////////////////////////////////////
	public void process() {
    	
    	int counter = tapeCounter.get();
    	if (hasRecording()) {
    		readRecordedBuffer();
    		if (active && !onMute) {
    			playFrame(buffer, leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
    		}
    	}
    	
    	if (++syncCounter > 7) { 
        	if (feedback != null)
        		MainFrame.update(feedback);
        	syncCounter = 0;
        }
   	
    	if (!recording()) 
    		return;
        
        // recording section 
    	float[][] newBuffer = null;
        for (LineIn in : sources) {
        	if (in.isOnMute() || in.isMuteRecord())continue;
            if (in.isSolo() && type != Type.SOLO) continue;
            if (!in.isSolo() && type == Type.SOLO) continue;
            if (in instanceof Instrument)
            	newBuffer = recordInstrument(in.getLeftPort().getFloatBuffer(), 
	            		in.isStereo() ? in.getRightPort().getFloatBuffer() : null, newBuffer);
            else if (in instanceof JudahSynth)
            	newBuffer = recordInternal((JudahSynth)in, newBuffer, SYNTH_BOOST);
            else if (in instanceof DrumMachine)
            	for (DrumKit kit : ((DrumMachine)in).getKits())
            		newBuffer = recordInternal(kit, newBuffer, DRUM_BOOST);
        }
        
        if (hasRecording()) {
            recording.dub(newBuffer, recordedBuffer, counter);
        }
        else {
            recording.add(newBuffer);
            if (sync != null && syncCounter == 0)
        		MainFrame.update(sync);
        }
    }
    
    private float[][] recordInstrument(FloatBuffer channelLeft, FloatBuffer channelRight, float[][] newBuffer) {
		boolean stereo = channelRight != null;
    	if (newBuffer == null) {
    		newBuffer = memory.getArray();
    		replace(LINE_BOOST, channelLeft, newBuffer[LEFT_CHANNEL]); // copy
    		replace(LINE_BOOST, stereo ? channelRight : channelLeft, newBuffer[RIGHT_CHANNEL]);
        }
        else {
            processAddGain(LINE_BOOST, channelLeft, newBuffer[LEFT_CHANNEL]);
            processAddGain(LINE_BOOST, stereo ? channelRight :channelLeft, newBuffer[RIGHT_CHANNEL]);
        }
    	return newBuffer;
    }
    
    private float[][] recordInternal(Engine voice, float[][] newBuffer, float gain) {
    	if (!voice.hasWork() || voice.isMuteRecord()) 
    		return newBuffer;
    	FloatBuffer channelLeft = voice.getBuffer()[LEFT_CHANNEL];
    	FloatBuffer channelRight = voice.getBuffer()[RIGHT_CHANNEL];
        boolean stereo = voice.getBuffer()[RIGHT_CHANNEL] != null;    
    	if (newBuffer == null) {
    		newBuffer = memory.getArray();
    		replace(gain, channelLeft, newBuffer[LEFT_CHANNEL]); // copy
    		replace(gain, stereo ? channelRight : channelLeft, newBuffer[RIGHT_CHANNEL]);
        }
    	else {
    		processAddGain(gain, channelLeft, newBuffer[LEFT_CHANNEL]);
    		processAddGain(gain, stereo ? channelRight : channelLeft, newBuffer[RIGHT_CHANNEL]);
        }
    	return newBuffer;
    }
   
    private final boolean recording() {
        if (isRecording.compareAndSet(STARTING, RUNNING)) {
        	if (recording.isEmpty())
        		_start = System.currentTimeMillis();
            MainFrame.update(this);
        }
        return isRecording.get() == RUNNING;
    }
    
}

//   mix external reverb (final fx) with output. A loop goes to Carla1 reverb, B to Carla2
//    @Override
//	protected void doReverb(FloatBuffer inL, FloatBuffer inR) {  // TODO map ports in Carla 
//    	FloatBuffer outL, outR;
//		outL = JudahZone.getOutL().getFloatBuffer();
//		outR = JudahZone.getOutR().getFloatBuffer();
//        outL.rewind();
//        outR.rewind();
//        if (reverb.isActive()) {
//        	((Freeverb)reverb).process(inL, inR);
//        	mix(workL, outL);
//            mix(workR, outR);
//	        // else {  mix(inL, getMains().getGainL(), outL); // grab master track gain
//	        //     mix(inR, getMains().getGainR(), outR); }// on the way out to reverb
//        }
//        else {
//        	mix(workL, outL);
//            mix(workR, outR);
//        }
//    }
    
