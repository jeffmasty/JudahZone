package net.judah.looper;

import static net.judah.api.AudioMode.*;
import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.*;
import net.judah.drumz.DrumKit;
import net.judah.drumz.DrumMachine;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.synth.JudahSynth;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class Loop extends AudioTrack implements TimeNotifier, RecordAudio {

	protected static final float INSTRUMENT_FACTOR = 2f;
	protected static final float SYNTH_FACTOR = 2.5f;
	protected static final float NO_GAIN = 1f;
	
	protected final Looper looper;
	@Getter protected boolean armed;
    @Getter protected Loop primary;
    @Getter private int loopCount;
    protected SyncWidget sync;
    protected int syncCounter;
    protected final Memory memory;
    protected Zone sources;
    protected long _start;
    protected final ArrayList<TimeListener> listeners = new ArrayList<>();
    @Getter protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
    
    protected final float[] workL = new float[bufSize];
    protected final float[] workR = new float[bufSize];
    protected final FloatBuffer bufL = FloatBuffer.wrap(workL);
    protected final FloatBuffer bufR = FloatBuffer.wrap(workR);
    protected final FloatBuffer[] buffer = new FloatBuffer[] {bufL, bufR};
    
    public Loop(String name, Looper parent, Zone sources) {
    	super(name);
    	looper = parent;
    	this.sources = sources;
    	memory = new Memory(Constants.STEREO, bufSize);
    	leftPort = parent.getLeft();
    	rightPort = parent.getRight();
    }
    
    @Override public AudioMode isRecording() {
        return isRecording.get();
    }
    
    @Override public void clear() {
    	isRecording.compareAndSet(RUNNING, STOPPING);
        tapeCounter.set(0);
        recording = null;
        length = null;
        new ArrayList<TimeListener>(listeners).forEach(listener -> 
        		listener.update(Notification.Property.STATUS, Status.TERMINATED));
        listeners.clear();
        isRecording.set(NEW);
        recording = null;
        active = false;
        MainFrame.update(this);
        if (this == looper.getLoopA())
        	MainFrame.update(LoopWidget.getInstance());
        listeners.clear();
    }

    public void setArmed(boolean sync) {
    	armed = sync;
    	MainFrame.update(this);
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
    
	@Override
    public void record(final boolean active) {
        AudioMode mode = isRecording.get();

        if (active && (recording == null || mode == NEW)) {
            recording = new Recording(true); // threaded to accept live stream
            isRecording.set(STARTING);
            _start = System.currentTimeMillis();
            new ArrayList<TimeListener>(listeners).forEach(
            		listener -> {listener.update(Notification.Property.STATUS, Status.ACTIVE);});
		} else if (active) {
            isRecording.set(STARTING);
            if (armed) 
            	setType(Type.ONE_SHOT);
            this.active = true;  // overdub
        } else if (mode == RUNNING && !active) {
        	endRecord();
        }
    }

	private void endRecord() {
		isRecording.set(STOPPED);
		active = true;

		if (length == null) { // Initial Recording 
            new Thread(() -> { // cut tape for other loopers
            	looper.setRecordedLength(System.currentTimeMillis() - _start, this);
            	length = recording.size();
            	for (Loop loop : looper) 
        			if (loop != this && !loop.hasRecording()) 
            			loop.setRecording(new Recording(length));
            	new ArrayList<>(listeners).forEach(
            		listener -> {listener.update(Notification.Property.STATUS, Status.TERMINATED);});
            	RTLogger.log(this, name + " cut tape " + looper.getRecordedLength() / 1000f + " seconds");
            }).start();
		}
        else {
        	armed = false;
        	new ArrayList<>(listeners).forEach(
            		listener -> {listener.update(Notification.Property.STATUS, Status.OVERDUBBED);});
        }
        MainFrame.update(this);
        
	}
	
	/** in Real-Time */
	@Override public void readRecordedBuffer() {
    	int updated = tapeCounter.get();
    	if (updated < recording.size())
    		recordedBuffer = recording.get(tapeCounter.get());
    	
        updated++;
        if (updated == recording.size()) 
            updated = 0;
        tapeCounter.set(updated);

        if (updated == 1) {
        	if (!listeners.isEmpty())
        		new Thread() { @Override public void run() {
	                for (int i = 0; i < listeners.size(); i++)
	                    listeners.get(i).update(Notification.Property.LOOP, ++loopCount);
        		}}.start();
        }
	    if (updated == 2 && this == looper.getPrimary()) 
	    	checkArmed();
        
    }

	private void checkArmed() {
    	boolean triggered = false;
		for (Loop loop : looper) {
			if (loop.isArmed() == false)
				continue;
			if (loop.getIsRecording().get() != RUNNING && !triggered) {
				loop.setType(Type.ONE_SHOT);
				triggered = true;
				new Thread(()->loop.record(true)).start(); // cutting fresh tape in background
			} 
			else if (loop.getIsRecording().get() == RUNNING) {
				loop.setType(Type.FREE);
				loop.record(false);
			}
		}
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

    // ---- Sync Section ----------------
    @Override public void addListener(TimeListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
            if (l instanceof Channel) {
            	MainFrame.update(l);
            }
        }
    }

    @Override public void removeListener(TimeListener l) {
        listeners.remove(l);
    }
    
    public SyncWidget getSync() {
    	if (sync == null)
    		 sync = new SyncWidget(this, JudahZone.getClock());
    	return sync;
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
    	
    	if (this == looper.getLoopA() && ++syncCounter > 7) { 
        	MainFrame.update(LoopWidget.getInstance());
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
            	newBuffer = recordInternal((JudahSynth)in, newBuffer, SYNTH_FACTOR);
            else if (in instanceof DrumMachine)
            	for (DrumKit kit : ((DrumMachine)in).getDrumkits())
            		newBuffer = recordInternal(kit, newBuffer, NO_GAIN);
        }
        
        if (hasRecording()) {
            recording.dub(newBuffer, recordedBuffer, counter);
        }
        else
            recording.add(newBuffer);
    }
    
    private float[][] recordInstrument(FloatBuffer channelLeft, FloatBuffer channelRight, float[][] newBuffer) {
		boolean stereo = channelRight != null;
    	if (newBuffer == null) {
    		newBuffer = memory.getArray();
    		replace(INSTRUMENT_FACTOR, channelLeft, newBuffer[LEFT_CHANNEL]); // copy
    		replace(INSTRUMENT_FACTOR, stereo ? channelRight : channelLeft, newBuffer[RIGHT_CHANNEL]);
        }
        else {
            processAddGain(INSTRUMENT_FACTOR, channelLeft, newBuffer[LEFT_CHANNEL]);
            processAddGain(INSTRUMENT_FACTOR, stereo ? channelRight :channelLeft, newBuffer[RIGHT_CHANNEL]);
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



//        	if (type == Type.ONE_SHOT && recording()) { //sync/armed record
//        		record(false);
//        		RTLogger.log(this, "de-triggered");
//        		setType(Type.FREE);
//            }
//        }
//        else 

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
    
