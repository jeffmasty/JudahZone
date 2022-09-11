package net.judah.looper;

import static net.judah.JudahZone.getMasterTrack;
import static net.judah.api.AudioMode.*;
import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.*;
import net.judah.effects.Freeverb;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.plugin.Carla;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class Loop extends Channel implements ProcessAudio, TimeNotifier, RecordAudio {

    protected final int bufferSize = JudahMidi.getInstance().getBufferSize();
    protected final Looper looper;

    @Getter protected Recording recording = new Recording(0, true);
    @Getter protected final List<JackPort> outputPorts = new ArrayList<>();
    @Setter @Getter protected Type type = Type.FREE;
    @Setter @Getter protected boolean armed = false;
    @Getter protected boolean dirty = false;
    @Getter protected final AtomicInteger tapeCounter = new AtomicInteger();
    @Getter protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
    @Getter protected final SyncWidget sync = new SyncWidget(this);
    @Getter private int loopCount = 0;
    @Getter protected Integer length;
    protected final ArrayList<TimeListener> listeners = new ArrayList<>();
    /** synchronize loop to Time.. */
    @Getter protected Loop primary;
    
    // for process() and recording
    private float[][] recordedBuffer;
    private FloatBuffer toJackLeft, toJackRight;
    private final float[] workL = new float[bufferSize];
    private final float[] workR = new float[bufferSize];
    private final FloatBuffer bufL = FloatBuffer.wrap(workL);
    private final FloatBuffer bufR = FloatBuffer.wrap(workR);
    private final Memory memory;
    private long _start;
    
    public Loop(String name, Looper parent) {
    	super(name, true);
    	looper = parent;
    	memory = new Memory(Constants.STEREO, JudahMidi.getInstance().getBufferSize());
    	setOutputPorts(JudahZone.getOutPorts());
    }
    
    @Override public void setOutputPorts(List<JackPort> ports) {
        synchronized (outputPorts) {
            outputPorts.clear();
            outputPorts.addAll(ports);
        }
    }

    @Override public AudioMode isRecording() {
        return isRecording.get();
    }
    
    public boolean hasRecording() {
        return recording != null && length != null && length > 0;
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
        dirty = false;
        MainFrame.update(this);
        if (this == looper.getLoopA())
        	MainFrame.update(LoopWidget.getInstance());
        listeners.clear();
    }

    public void setRecording(Recording sample) {
    	if (recording != null)
            recording.close();
        recording = sample;
        length = recording.size();
        isRecording.set(STOPPED);
        if (this instanceof Sample == false) {
        	recording.startListeners();
        	MainFrame.update(this);
        }
    }

    public void delete() {
    	if (recording == null) return;
    	new Thread(() -> {
	    	if (recording != null) 
	    		recording.close();
	    	length = 0;
	    	recording.clear();
	    	dirty = false;
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
	    		dirty = false;
	    		RTLogger.log(this, "Erased: " + getName());
	    		MainFrame.update(this);
	    		
	    	}).start(); 
    }
    
    @Override public String toString() {
        return "Loop " + name;
    }

    public void setTapeCounter(int current) {
        tapeCounter.set(current);
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
            dirty = true;
            // RTLogger.log(this, name + " overdub starting");
        } else if (mode == RUNNING && !active) {
        	endRecord();
        }
    }

	private void endRecord() {
		isRecording.set(STOPPED);
        if (length == null) { // Initial Recording, cut tape for other loopers
        	if (this == looper.getLoopA()) recording.trim(); // fudge factor
        	
            length = recording.size();
            looper.setRecordedLength(System.currentTimeMillis() - _start);

            new ArrayList<>(listeners).forEach(
            		listener -> {listener.update(Notification.Property.STATUS, Status.TERMINATED);});
            new Thread(() -> { // set blank tape on other loopers
            	boolean firstArmed = false; // allows for loops to be chained up 1 after the other
            	for (Loop loop : looper) {
	        			if (loop != this && !loop.hasRecording()) { 
	            			loop.setRecording(new Recording(length));
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
        dirty = true;
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
    @Override
    public void process() {
    	
    	int counter = tapeCounter.get();
    	if (hasRecording())
    		readRecordedBuffer();
    	
    	if (this == looper.getLoopA() && ++counter > 7) { 
        	MainFrame.update(LoopWidget.getInstance());
        	counter = 0;
        }

    	playFrame(); 
    	if (!recording()) return;
        
        // recording section 
        float[][] newBuffer = memory.getArray();
        boolean firstTime = true;
        FloatBuffer fromJack;
        for (LineIn channel : JudahZone.getChannels()) {
            if (channel.isOnMute() || channel.isMuteRecord())
                continue;
            if (channel.isSolo() && type != Type.SOLO) continue;
            if (!channel.isSolo() && type == Type.SOLO) continue;
            
            fromJack = channel.getLeftPort().getFloatBuffer();
            if (firstTime) {
                FloatBuffer.wrap(newBuffer[LEFT_CHANNEL]).put(fromJack); // processEcho
                if (channel.isStereo())
                    FloatBuffer.wrap(newBuffer[RIGHT_CHANNEL]).put(
                            channel.getRightPort().getFloatBuffer());
                else {
                    fromJack.rewind();
                    FloatBuffer.wrap(newBuffer[RIGHT_CHANNEL]).put(fromJack);
                }
                firstTime = false;
            }
            else {
                processAdd(fromJack, newBuffer[LEFT_CHANNEL]);
                processAdd( channel.isStereo() ?
                        channel.getRightPort().getFloatBuffer() :
                            fromJack, newBuffer[RIGHT_CHANNEL]);
            }
        }

        if (hasRecording()) {
            if (recordedBuffer == null) {
                readRecordedBuffer();
            }
            // if (overwrite) recording.set(counter, newBuffer); else
            recording.dub(newBuffer, recordedBuffer, counter);
        }
        else
            recording.add(newBuffer);

    }

    protected void playFrame() {
        if (dirty == false || onMute || hasRecording() == false) return; // ok, we're on mute and we've progressed the tape counter.
        // gain & pan stereo
        float leftVol = (gain.getVol() * 0.025f) * (1 - getPan());
        float rightVol = (gain.getVol() * 0.025f) * getPan();

        AudioTools.processGain(recordedBuffer[LEFT_CHANNEL], workL, leftVol);
        AudioTools.processGain(recordedBuffer[RIGHT_CHANNEL], workR, rightVol);

        if (eq.isActive()) {
            eq.process(workL, true);
            eq.process(workR, false);
        }

        if (chorus.isActive())
            chorus.processStereo(bufL, bufR);
        
        if (overdrive.isActive()) {
            overdrive.processAdd(bufL);
            overdrive.processAdd(bufR);
        }
        
        if (reverb.isActive() && !reverb.isInternal() 
        		&& getMasterTrack().getOverdrive().isActive()) {
        	// if sending to external reverb, add Master Track overdrive if active
        	getMasterTrack().getOverdrive().processAdd(bufL);
        	getMasterTrack().getOverdrive().processAdd(bufR);
        }
        
        if (delay.isActive()) {
            delay.processAdd(bufL, bufL, true);
            delay.processAdd(bufR, bufR, false);
        }
        if (cutFilter.isActive()) {
            cutFilter.process(bufL, bufR, 1);
        }

        // blend possible reverb on last play processing
        toJackLeft = outputPorts.get(LEFT_CHANNEL).getFloatBuffer();
        toJackRight = outputPorts.get(RIGHT_CHANNEL).getFloatBuffer();
        if (reverb.isActive()) {
	        if (reverb == Carla.getInstance().getReverb()) {
	        	toJackLeft = JudahZone.getReverbL1().getFloatBuffer();
	        	toJackRight = JudahZone.getReverbR1().getFloatBuffer();
	        } else if (reverb == Carla.getInstance().getReverb2()) {
	        	toJackLeft = JudahZone.getReverbL2().getFloatBuffer();
	        	toJackRight = JudahZone.getReverbR2().getFloatBuffer();
	        }
	        toJackLeft.rewind();
	        toJackRight.rewind();
	        if (reverb.isInternal()) {
	        	((Freeverb)reverb).process(bufL, bufR);
	        	processMix(workL, toJackLeft);
	            processMix(workR, toJackRight);
	        }
	        else {
	            processAdd(bufL, getMasterTrack().getGainL(), toJackLeft); // grab master track gain
	            processAdd(bufR, getMasterTrack().getGainR(), toJackRight); // on the way out to reverb
	        }
        }
        else {
        	toJackLeft.rewind();
        	toJackRight.rewind();
        	processMix(workL, toJackLeft);
            processMix(workR, toJackRight);
        }
        
    }
   
    private final boolean recording() {
        if (isRecording.compareAndSet(STARTING, RUNNING)) {
        	if (isEmpty())
        		_start = System.currentTimeMillis();
            MainFrame.update(this);
        }
        return isRecording.get() == RUNNING;
    }


    protected void readRecordedBuffer() {

        recordedBuffer = recording.get(tapeCounter.get());
        
        int updated = tapeCounter.get() + 1;
        if (updated == recording.size()) {
            if (type == Type.ONE_SHOT) {
            	if (recording()) {
            		record(false);
            		setType(Type.FREE);
            	}
            	else
            		dirty = false;
            }
            updated = 0;
            new Thread() { @Override public void run() {
                for (int i = 0; i < listeners.size(); i++)
                    listeners.get(i).update(Notification.Property.LOOP, ++loopCount);
            }}.start();
        }
        tapeCounter.set(updated);
        if (dirty && this == looper.getLoopA()) 
        	MainFrame.update(getSync());
    }

	@Override
	public AudioMode isPlaying() {
		return dirty? AudioMode.RUNNING : AudioMode.ARMED;
	}

    
}
