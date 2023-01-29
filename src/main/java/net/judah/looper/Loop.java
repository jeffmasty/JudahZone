package net.judah.looper;

import static net.judah.api.AudioMode.*;
import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
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

public class Loop extends AudioTrack {

	protected static final float LINE_BOOST = 3f;
	protected static final float SYNTH_BOOST = 2f;
	protected static final float DRUM_BOOST = 4f;
	
	protected final Looper looper;
	protected final JudahClock clock;
    protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
    @Getter private int loopCount;
    protected final Memory memory;
    protected final Zone sources;
    protected long _start;
    protected final float[] workL = new float[bufSize];
    protected final float[] workR = new float[bufSize];
    protected final FloatBuffer bufL = FloatBuffer.wrap(workL);
    protected final FloatBuffer bufR = FloatBuffer.wrap(workR);
    protected final FloatBuffer[] buffer = new FloatBuffer[] {bufL, bufR};
    
    public Loop(String name, Looper loops, Zone sources, String icon, Type type, JudahClock clock,
    		JackPort l, JackPort r) {
    	super(name, type);
    	this.looper = loops;
    	this.sources = sources;
    	this.clock = clock;
    	memory = new Memory(Constants.STEREO, bufSize);
    	leftPort = l;
    	rightPort = r;
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
    }

    @Override
	public void setRecording(Recording music) {
    	if (type == Type.FREE) 
    		type = Type.SYNC;
    	isRecording.set(STOPPED);
    	super.setRecording(music);
    	recording.startListeners();
    	MainFrame.update(this);
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
		} else if (active) {
            isRecording.set(STARTING);
            this.active = true;  // overdub
        } else if (mode == RUNNING) {
        	endRecord();
        }
    }

	private void endRecord() {
		isRecording.set(STOPPED);
		active = true;
		clock.syncDown(this);
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
        if (recording == null) 
        	return;
        
		int updated = tapeCounter.get();
        updated++;
        
        if (updated >= recording.size()) 
            updated = 0;
        tapeCounter.set(updated);
        if (updated == 1) {
        	loopCount++;
        	if (this == looper.getPrimary()) 
        		looper.topUp(loopCount);
        }
        
	    // peek ahead/latency
	    updated++;
	    if (updated >= recording.size())
	    	updated = 0;
	    if (recording.isEmpty()) return;
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

    public void trigger() {
		if (isRecording() == AudioMode.RUNNING) 
			if (type == Type.BSYNC && !hasRecording() && clock.isSync(this))
				clock.tail(this);
			else 
				record(false);
		else if (!hasRecording()) {
			if (type == Type.FREE)
				record(true);
			else 
				clock.syncUp(this, -1);
		}
		else 
			record(isRecording() != AudioMode.RUNNING);
	}
    
    ////////////////////////////////////
    //     Process Realtime Audio     //
    ////////////////////////////////////
	public void process() {
    	
    	int counter = tapeCounter.get();
		readRecordedBuffer();
		if (active && !onMute && hasRecording()) {
			playFrame(buffer, leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
		}
		if (!recording()) 
    		return;

		// recording section 
    	LineIn solo = looper.getSoloTrack().isSolo() ? looper.getSoloTrack().getSoloTrack() : null;
    	float[][] newBuffer = null;
        for (LineIn in : sources) {
        	if (in.isOnMute() || in.isMuteRecord())continue;
            if (in == solo && type != Type.SOLO) continue;
            if (in != solo && type == Type.SOLO) continue;
            if (in instanceof Instrument)
            	newBuffer = recordInstrument(in.getLeftPort().getFloatBuffer(), 
	            		in.isStereo() ? in.getRightPort().getFloatBuffer() : null, newBuffer);
            else if (in instanceof JudahSynth)
            	newBuffer = recordInternal((JudahSynth)in, newBuffer, SYNTH_BOOST);
            else if (in instanceof DrumMachine)
            	for (DrumKit kit : ((DrumMachine)in).getKits())
            		newBuffer = recordInternal(kit, newBuffer, DRUM_BOOST);
        }
        
        if (hasRecording()) 
            recording.dub(newBuffer, recordedBuffer, counter);
        else 
            recording.add(newBuffer);
        
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
    
