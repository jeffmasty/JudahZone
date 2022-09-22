package net.judah.looper;

import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.api.ProcessAudio;
import net.judah.mixer.Channel;
import net.judah.samples.Sample;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

public abstract class AudioTrack extends Channel implements ProcessAudio {

	protected final int bufferSize = Constants.bufSize();

	@Setter @Getter protected Type type = Type.FREE;
    @Getter protected Recording recording = new Recording(0, true);
    @Getter protected Integer length;
    @Setter @Getter protected boolean active;
    @Getter protected final AtomicInteger tapeCounter = new AtomicInteger();

    protected float[][] recordedBuffer;

	public AudioTrack(String name) {
    	super(name, true);
	}

	public boolean hasRecording() {
        return recording != null && length != null && length > 0;
    }

    public void setRecording(Recording sample) {
    	if (recording != null)
            recording.close();
        recording = sample;
        length = recording.size();
        if (this instanceof Sample == false) {
        	recording.startListeners();
        	MainFrame.update(this);
        }
    }

    @Override
	public final AudioMode isPlaying() {
		return active? AudioMode.RUNNING : AudioMode.ARMED;
	}

    protected void playFrame(FloatBuffer inL, FloatBuffer inR) {
    	float[] workL = inL.array();
    	float[] workR = inL.array();
    	
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
            chorus.processStereo(inL, inR);
        
        if (overdrive.isActive()) {
            overdrive.processAdd(inL);
            overdrive.processAdd(inR);
        }
        
        if (delay.isActive()) {
            delay.processAdd(inL, inL, true);
            delay.processAdd(inR, inR, false);
        }
        cutFilter.process(inL, inR, 1);
        
        doReverb(inL, inR); // subclass for Carla Reverb, or Input to Looper
    }

    protected abstract void doReverb(FloatBuffer inL, FloatBuffer inR);
    
    @Override // Loop has more sophisticated version
	public void readRecordedBuffer() { 
        recordedBuffer = recording.get(tapeCounter.get());
        int updated = tapeCounter.get() + 1;
        if (updated == recording.size()) {
            if (type == Type.ONE_SHOT) {
            	active = false;
            }
            updated = 0;
        }
        tapeCounter.set(updated);
    }
    
    @Override public String toString() {
        return this.getClass().getSimpleName() + " " + name;
    }

    @Override
	public final void setTapeCounter(int current) {
        tapeCounter.set(current);
    }

    @Override
	public abstract void process();
   
}
