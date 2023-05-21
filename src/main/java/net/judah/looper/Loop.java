package net.judah.looper;

import static net.judah.api.AudioMode.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.api.AudioMode;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.synth.JudahSynth;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class Loop extends AudioTrack {
	private static final float STD_BOOST = 4f;
	private static final float DRUM_BOOST = 1.5f;
	
	protected final Looper looper;
	protected final JudahClock clock;
    protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
    @Getter private int loopCount;
    protected final Memory memory;
    protected final Zone sources;
    protected long _start;
    
    public Loop(String name, Looper loops, Zone sources, String icon, Type type, JudahClock clock,
    		JackPort l, JackPort r) {
    	super(name, type);
    	this.looper = loops;
    	this.sources = sources;
    	this.clock = clock;
    	memory = looper.getMemory();
    	leftPort = l;
    	rightPort = r;
    	if (icon != null)
    		setIcon(Icons.get(icon));
    	recording.startListeners();
    }
    
    public AudioMode isRecording() {
        return isRecording.get();
    }
    
    @Override public void clear() {
    	isRecording.compareAndSet(RUNNING, STOPPING);
        tapeCounter.set(0);
        length = null;
        isRecording.set(NEW);
        recording.clear();
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
    
    /** keep length, erase music */
    public void erase() {
    	record(false);
    	Constants.execute(() -> {
	    		active = false;
	    		if (looper.getPrimary() != null)
	    			recording.setSize(looper.getPrimary().getLength());
    			for (int i = 0; i < recording.size(); i++)
	    			recording.set(i, new float[Constants.STEREO][Constants.bufSize()]);
    			MainFrame.update(this);
	    		RTLogger.log(this, "Erased: " + getName());
    		}); 
    }
    
    public void record(boolean record) {
        AudioMode mode = isRecording.get();
        if (record) {
            isRecording.set(STARTING);
            this.active = true;
            if (mode == NEW) 
            	_start = System.currentTimeMillis();
        } else if (mode == RUNNING) {
        	endRecord();
        }
    }

	private void endRecord() {
		isRecording.set(STOPPED);
		clock.syncDown(this);
		active = true;
		if (length == null && looper.getPrimary() == null) // initial recording 
			looper.setRecordedLength(_start, this);
		MainFrame.update(this);
	}
	
	/** in Real-Time */	
	@Override public void readRecordedBuffer() {
        if (length == null) 
        	return;
        
		int updated = tapeCounter.get();
		if (updated == 0) {
        	loopCount++;
        	if (this == looper.getPrimary()) 
        		looper.loopCount(loopCount);
		}
        updated++;
        if (updated >= recording.size()) 
            updated = 0;
        tapeCounter.set(updated);
        
	    // peek ahead/latency
        if (recording.isEmpty()) return;
        updated++;
	    if (updated >= recording.size())
	    	updated = 0;
	    recordedBuffer = recording.get(updated);
        
    }

	public void duplicate() {
		Constants.execute(()-> {
			Recording source = getRecording();
			Recording dupe =  new Recording(source, 2);
			int offset = source.size();
			float[][] sound;
			for (int i = 0; i < offset; i++) {
				sound = source.elementAt(i);
				dupe.set(i, sound);
				dupe.set(i + offset, sound);
			}
			setRecording(dupe); 
			RTLogger.log(this, name + " duplicated (" + recording.size() + " frames)");
		});
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
		if (active && !onMute && hasRecording() && recordedBuffer != null) {
			playFrame(leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
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
            	newBuffer = record(in.getLeft(), in.getRight(), newBuffer, STD_BOOST);
            else if (in instanceof JudahSynth) {
            	JudahSynth synth = (JudahSynth)in;
            	if (synth.hasWork() && !synth.isMuteRecord())
            		newBuffer = record(synth.getLeft(), synth.getRight(), newBuffer, STD_BOOST);
            }
            else if (in instanceof DrumMachine)
            	for (DrumKit kit : ((DrumMachine)in).getKits())
            		newBuffer = record(kit.getLeft(), kit.getRight(), newBuffer, DRUM_BOOST);
        }
        
        if (hasRecording()) 
            recording.dub(newBuffer, recordedBuffer, counter);
        else  {
            recording.add(newBuffer);
            looper.catchUp(this);
        }
        
    }
    
    private float[][] record(FloatBuffer channelLeft, FloatBuffer channelRight, float[][] newBuffer, float amp) {
    	if (newBuffer == null) {
    		newBuffer = memory.getArray();
    		AudioTools.replace(amp, channelLeft, newBuffer[LEFT_CHANNEL]); 
    		AudioTools.replace(amp, channelRight, newBuffer[RIGHT_CHANNEL]);
        }
        else {
            AudioTools.add(amp, channelLeft, newBuffer[LEFT_CHANNEL]);
            AudioTools.add(amp, channelRight, newBuffer[RIGHT_CHANNEL]);
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

	public void catchUp(int size) {
		while (recording.size() < size) 
			recording.add(memory.getArray());
		setTapeCounter(size);
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
    
