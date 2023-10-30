package net.judah.looper;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;
import static net.judah.util.Constants.STEREO;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.RecordAudio;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.FileChooser;
import net.judah.midi.JudahClock;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.synth.JudahSynth;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class Loop extends AudioTrack implements RecordAudio, Runnable {
	protected static int INIT = 9999; // nice chunk of preloaded blank tape
	protected static final float STD_BOOST = 3;
	protected static final float DRUM_BOOST = 1.5f;
	
    @Getter protected boolean isRecording;
    @Getter protected int length;
    protected final Zone sources;
	protected final Looper looper;
    protected final Memory memory;
    protected int loopCount;
    protected int frame;
	private final BlockingQueue<float[][]> newQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<float[][]> oldQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<Integer> locationQueue = new LinkedBlockingQueue<>();

    public Loop(String name, String icon, Type type, Looper loops, Zone sources, 
    		JackPort l, JackPort r, Memory mem) {
    	super(name, type);
    	this.looper = loops;
    	this.sources = sources;
    	this.memory = mem;
    	this.leftPort = l;
    	this.rightPort = r;
    	this.icon = Icons.get(icon);
    	for (int i = 0; i < INIT; i++)
    		recording.add(new float[STEREO][bufSize]);
    	new Thread(this).start(); // overdub listening
    }
    
	/** ignore super */
	@Override public void clear() {
		isRecording = false;
        onMute = false;
		playBuffer = null;
        loopCount = 0;
        AudioTools.silence(recording, length);
        tapeCounter.set(0);
        length = 0;
		MainFrame.update(this);
	}
    
    @Override public void setRecording(Recording music) {
    	isRecording = false;
    	super.setRecording(music);
    	MainFrame.update(this);
    }

    @Override public boolean isPlaying() { 
    	return length > 0; }
    
	@Override public void run() {
		try { 
			do { // overdub
				int idx = locationQueue.take();
				recording.set(idx, AudioTools.overdub(newQueue.take(), oldQueue.take()));
			} while (true);
		}
		catch (Exception e) { RTLogger.warn(this, e);}
	}
    
    /** loop boundary, hard sync to start.*/ 
    public void boundary() { 
    	if (length == 0 || type == Type.FREE || isRecording) 
    		return;
    	int idx = tapeCounter.get() % looper.getLength(); // maybe loop was duplicated
    	if (idx > 4)
    		tapeCounter.set(length - 1); 
    }

    void catchUp(int size) {
		while (recording.size() < size) 
			recording.add(memory.getArray());
    }
    
    public void save() {
		try {
			ToDisk.toDisk(recording, FileChooser.choose(Folders.getLoops()), length);
		} catch (Throwable t) { RTLogger.warn(this, t); }
	}

    public void load(File f, boolean primary) {
		clear();
		try {
			length = recording.load(f, 1); 
			if (primary) {
				rewind();
				looper.setPrimary(this);
			}
			MainFrame.update(this);
		} catch (Exception e) { RTLogger.warn(this, e);}
    }
    public void load(String name, boolean primary) {
    	if (name.endsWith(".wav") == false)
    		name += ".wav";
    	File file = new File(Folders.getLoops(), name);
    	if (file.exists())
    		load(file, primary);
    	else 
    		RTLogger.log(this, "Not a file: " + file.getAbsolutePath());
    }
	public void load(boolean primary) { 
		File file = FileChooser.choose(Folders.getLoops());
		if (file == null) return;
		load(file, primary);
	}

    @Override public void record(boolean record) {
        if (record) { 
        	Loop primary = looper.getPrimary();
        	if (primary == null) // we will be primary
        		tapeCounter.set(0); 
        	else if (length == 0) {
        		length = primary.getLength();
        		if (length >= recording.size())
        		catchUp(length);
        		tapeCounter.set(primary.getTapeCounter().get());
        	}
        	isRecording = true;
        }
        else if (isRecording) 
        	endRecord();
        else return;
        MainFrame.update(this);
    }

	private void endRecord() {
		isRecording = false;
		if (type == Type.FREE || type == Type.BSYNC)
			type = Type.SYNC;
		if (looper.getPrimary() == null) {
			length = tapeCounter.get() - 1;
			tapeCounter.set(length);
			looper.setPrimary(this);
		}
		else if (length == 0) 
			length = looper.getLength();
		looper.syncDown();
		MainFrame.update(this);
	}
	
	public void doubled() { 
		if (length == 0) { // activate blank tape of double primary's length
			Loop primary = looper.getPrimary();
			if (primary == null) return; 
			length = primary.getLength() * 2;
			Constants.execute(()->catchUp(length));
			RTLogger.log(toString(), "Duplicated: " + length);
			return;
		}

		Constants.execute(()->{ // duplicate recording
			if (this == looper.getPrimary())
				JudahZone.getClock().setLength(JudahClock.getLength() * 2);

			if (length * 2 >= recording.size()) 
				catchUp(length * 2);
			for (int i = 0; i < length; i++) 
				AudioTools.copy(recording.get(i), recording.get(i + length));
			length *= 2;
			RTLogger.log(toString(), "Audio Duplicated: " + length);
		});
			
	}
	
	/** user engaged the record button, setup depending on loop type */
    public void trigger() { 
		if (isRecording()) {
			if (type == Type.BSYNC && !looper.hasRecording() && looper.getCountdown() == this)
				looper.tail(this);
			else 
				record(false);
		} else if (looper.hasRecording()) 
			record(true); // everything ready for overdub
		else if (type == Type.FREE) {
			record(true); 
			looper.checkSoloSync();
		} else // start recording on downbeat
			looper.syncUp(this, -1);
	}
    
    ////////////////////////////////////
    //     Process Realtime Audio     //
    ////////////////////////////////////
	public void process() {
		frame = tapeCounter.getAndIncrement();
		if (isPlaying()) 
			playFrame();
		if (isRecording)
			recordFrame();
		playBuffer = null;
    }

	private void playFrame() {
		if (frame == 0 && this == looper.getPrimary()) 
			looper.loopCount(loopCount++);
		if (frame >= length) {
			frame = 0;
			tapeCounter.set(0);
		}
		playBuffer = recording.get(frame);
		if (!onMute && playBuffer != null) 
			playFrame(leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
	}
	
	private void recordFrame() {
    	// merge live recording sources into newBuffer
    	float[][] newBuffer = frame < recording.size() ? recording.get(frame) : memory.getArray();
    	LineIn solo = looper.getSoloTrack().isSolo() ? looper.getSoloTrack().getSoloTrack() : null;
    	for (LineIn in : sources) {
        	if (in.isOnMute() || in.isMuteRecord())continue;
            if (in == solo && type != Type.SOLO) continue;
            if (in != solo && type == Type.SOLO) continue;
            
            if (in instanceof Instrument)
            	recordCh(in.getLeft(), in.getRight(), newBuffer, STD_BOOST);
            else if (in instanceof JudahSynth) {
            	JudahSynth synth = (JudahSynth)in;
            	if (!synth.isMuteRecord() && synth.isActive() && !synth.isOnMute())
            		recordCh(synth.getLeft(), synth.getRight(), newBuffer, STD_BOOST);
            }
            else if (in instanceof DrumMachine)
            	for (DrumKit kit : ((DrumMachine)in).getKits())
            		recordCh(kit.getLeft(), kit.getRight(), newBuffer, DRUM_BOOST);
        }

    	if (frame < recording.size()) {
    		if (recording.get(frame) == newBuffer)
    			return;
			// off-thread overdub
			oldQueue.add(playBuffer);
			newQueue.add(newBuffer);
			locationQueue.add(frame);
    	}
    	else if (playBuffer == null) {
			if (frame < recording.size())
				recording.set(frame, newBuffer);
	        else {
	        	recording.add(newBuffer);
	        	looper.catchUp(this, frame);
	        }    		
    	} 
	}
	
    private void recordCh(FloatBuffer channelLeft, FloatBuffer channelRight, float[][] newBuffer, float amp) {
    	AudioTools.add(amp, channelLeft, newBuffer[LEFT]);
    	AudioTools.add(amp, channelRight, newBuffer[RIGHT]);
    }
	
}
