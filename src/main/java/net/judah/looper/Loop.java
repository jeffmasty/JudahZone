package net.judah.looper;

import static net.judah.api.AudioMode.*;
import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;
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

	protected static final float INSTRUMENT_FACTOR = 1.5f;
	protected static final float SYNTH_FACTOR = 2f;
	protected static final float NO_GAIN = 1f;
	protected final Looper looper;
	@Setter @Getter protected boolean armed;
    protected SyncWidget sync;
    protected int syncCounter;
    @Getter private int loopCount;
    protected final Memory memory;
    protected Zone sources;
    @Getter protected Loop primary;
    protected long _start;
    protected final ArrayList<TimeListener> listeners = new ArrayList<>();
    @Getter protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
    
    protected final float[] workL = new float[bufferSize];
    protected final float[] workR = new float[bufferSize];
    protected final FloatBuffer bufL = FloatBuffer.wrap(workL);
    protected final FloatBuffer bufR = FloatBuffer.wrap(workR);
    protected final FloatBuffer[] buffer = new FloatBuffer[] {bufL, bufR};
    
    public Loop(String name, Looper parent, Zone sources) {
    	super(name);
    	looper = parent;
    	this.sources = sources;
    	memory = new Memory(Constants.STEREO, bufferSize);
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
            //RTLogger.log(this, name + " recording starting");
		} else if (active) {
            isRecording.set(STARTING);
            this.active = true;
            // RTLogger.log(this, name + " overdub starting");
        } else if (mode == RUNNING && !active) {
        	endRecord();
        }
    }

	private void endRecord() {
		isRecording.set(STOPPED);
        if (length == null) { // Initial Recording, cut tape for other loopers
        	if (this == looper.getLoopA()) recording.trim(); // fudge factor
        	
        	final int loopLength = length = recording.size();
            looper.setRecordedLength(System.currentTimeMillis() - _start);

            new ArrayList<>(listeners).forEach(
            		listener -> {listener.update(Notification.Property.STATUS, Status.TERMINATED);});
            new Thread(() -> { // set blank tape on other loopers
            	boolean firstArmed = false; // allows for loops to be chained up 1 after the other
            	for (Loop loop : looper) {
	        			if (loop != this && !loop.hasRecording()) { 
	            			loop.setRecording(new Recording(loopLength));
		    			}
	        			if (loop.isArmed() && !firstArmed) { // start overdub
	        				firstArmed = true;
	        				loop.setType(Type.ONE_SHOT);
	        				loop.setArmed(false);
	        				loop.record(true);
	        			}
		    	}
        	}).start();
        	RTLogger.log(this, name + " cut tape " + looper.getRecordedLength() / 1000f + " seconds");
        }
        else {
        	new ArrayList<>(listeners).forEach(
            		listener -> {listener.update(Notification.Property.STATUS, Status.OVERDUBBED);});
        }
        active = true;
        MainFrame.update(this);
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
        float[][] newBuffer = memory.getArray();
        boolean firstTime = true;
        for (LineIn in : sources) {
        	if (in.isOnMute() || in.isMuteRecord())continue;
            if (in.isSolo() && type != Type.SOLO) continue;
            if (!in.isSolo() && type == Type.SOLO) continue;
            if (in instanceof Instrument)
	            firstTime = recordInstrument(in.getLeftPort().getFloatBuffer(), 
	            		in.isStereo() ? in.getRightPort().getFloatBuffer() : null, 
	            		firstTime, newBuffer);
            else if (in instanceof JudahSynth)
            	firstTime = recordInternal((JudahSynth)in, firstTime, newBuffer, SYNTH_FACTOR);
            else if (in instanceof DrumMachine)
            	for (DrumKit drums : ((DrumMachine)in).getChannels())
            		firstTime = recordInternal(drums, firstTime, newBuffer, NO_GAIN);
        }
        
        if (hasRecording()) {
            recording.dub(newBuffer, recordedBuffer, counter);
        }
        else
            recording.add(newBuffer);
    }
    
    private boolean recordInstrument(FloatBuffer channelLeft, FloatBuffer channelRight, boolean firstTime, float[][] newBuffer) {
    		boolean stereo = channelRight != null;
        	if (firstTime) {
        		replace(INSTRUMENT_FACTOR, channelLeft, newBuffer[LEFT_CHANNEL]); // copy
        		replace(INSTRUMENT_FACTOR, stereo ? channelRight : channelLeft, newBuffer[RIGHT_CHANNEL]);
                firstTime = false;
            }
            else {
                processAddGain(INSTRUMENT_FACTOR, channelLeft, newBuffer[LEFT_CHANNEL]);
                processAddGain(INSTRUMENT_FACTOR, stereo ? channelRight :channelLeft, newBuffer[RIGHT_CHANNEL]);
            }
        	return firstTime;
    }
    
    private boolean recordInternal(Engine voice, boolean firstTime, float[][] newBuffer, float gain) {
    	if (!voice.hasWork() || voice.isMuteRecord()) 
    		return firstTime;
    	FloatBuffer channelLeft = voice.getBuffer()[LEFT_CHANNEL];
    	FloatBuffer channelRight = voice.getBuffer()[RIGHT_CHANNEL];
        boolean stereo = voice.getBuffer()[RIGHT_CHANNEL] != null;    
    	if (firstTime) {
        		replace(gain, channelLeft, newBuffer[LEFT_CHANNEL]); // copy
        		replace(gain, stereo ? channelRight : channelLeft, newBuffer[RIGHT_CHANNEL]);
                firstTime = false;
            }
            else {
                processAddGain(gain, channelLeft, newBuffer[LEFT_CHANNEL]);
                processAddGain(gain, stereo ? channelRight : channelLeft, newBuffer[RIGHT_CHANNEL]);
            }
    	
    	return firstTime;
    }
   
    private final boolean recording() {
        if (isRecording.compareAndSet(STARTING, RUNNING)) {
        	if (recording.isEmpty())
        		_start = System.currentTimeMillis();
            MainFrame.update(this);
        }
        return isRecording.get() == RUNNING;
    }

    @Override
	public void readRecordedBuffer() {

        recordedBuffer = recording.get(tapeCounter.get());
        
        int updated = tapeCounter.get() + 1;
        if (updated == recording.size()) {
            if (type == Type.ONE_SHOT) {
            	if (recording()) {
            		record(false);
            		setType(Type.FREE);
            	}
            	else
            		active = false;
            }
            updated = 0;
            new Thread() { @Override public void run() {
                for (int i = 0; i < listeners.size(); i++)
                    listeners.get(i).update(Notification.Property.LOOP, ++loopCount);
            }}.start();
        }
        tapeCounter.set(updated);
        if (active && this == looper.getLoopA()) 
        	MainFrame.update(getSync());
    }
 
    public SyncWidget getSync() {
    	if (sync == null)
    		 sync = new SyncWidget(this);
    	return sync;
    }
}

//    // mix external reverb (final fx) with output. A loop goes to Carla1 reverb, B to Carla2
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
    
