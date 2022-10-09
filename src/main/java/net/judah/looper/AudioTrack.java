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
import net.judah.util.AudioTools;
import net.judah.util.Constants;
@Getter
public abstract class AudioTrack extends Channel implements ProcessAudio {

	protected final int bufferSize = Constants.bufSize();

	@Setter protected Type type = Type.FREE;
    protected Recording recording = new Recording(0, true);
    protected Integer length;
    protected float env = 1f;
    @Setter protected float velocity = 1f;
    @Setter protected boolean active;
    protected final AtomicInteger tapeCounter = new AtomicInteger();

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
        if (this instanceof Loop) {
        	recording.startListeners();
        	MainFrame.update(this);
        }
    }

    @Override
	public final AudioMode isPlaying() {
		return active? AudioMode.RUNNING : AudioMode.ARMED;
	}

    protected void playFrame(FloatBuffer[] in, FloatBuffer left, FloatBuffer right) {
    	float[] workL = in[LEFT_CHANNEL].array();
    	float[] workR = in[RIGHT_CHANNEL].array();
    	
        // gain & pan stereo
    	float baseVol = env * velocity * gain.getGain();
        AudioTools.processGain(recordedBuffer[LEFT_CHANNEL], workL, baseVol * (1f - getPan()));
        AudioTools.processGain(recordedBuffer[RIGHT_CHANNEL], workR, baseVol * getPan());

        cutFilter.process(in[LEFT_CHANNEL], in[RIGHT_CHANNEL], 1f);   
        hiCut.process(in[LEFT_CHANNEL], in[RIGHT_CHANNEL], 1f);

        if (eq.isActive()) {
            eq.process(workL, true);
            eq.process(workR, false);
        }

        if (chorus.isActive())
            chorus.processStereo(in[LEFT_CHANNEL], in[RIGHT_CHANNEL]);
        
        if (overdrive.isActive()) {
            overdrive.processAdd(in[LEFT_CHANNEL]);
            overdrive.processAdd(in[LEFT_CHANNEL]);
        }
        
        if (delay.isActive()) {
            delay.processAdd(in[LEFT_CHANNEL], in[LEFT_CHANNEL], true);
            delay.processAdd(in[RIGHT_CHANNEL], in[RIGHT_CHANNEL], false);
        }
        
		if (reverb.isActive()) 
			reverb.process(in[LEFT_CHANNEL], in[RIGHT_CHANNEL]);
		
		AudioTools.mix(in[0], left);
		AudioTools.mix(in[1], right);
    }
    
    @Override // Loop has more sophisticated version
	public void readRecordedBuffer() { 
        recordedBuffer = recording.get(tapeCounter.get());
        int updated = tapeCounter.get() + 1;
        if (updated == recording.size()) {
            if (type == Type.ONE_SHOT) {
            	active = false;
            	MainFrame.update(this);
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

}
