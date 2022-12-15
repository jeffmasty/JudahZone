package net.judah.looper;

import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.AudioMode;
import net.judah.api.ProcessAudio;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.util.AudioTools;
@Getter
public abstract class AudioTrack extends Channel implements ProcessAudio {

	@Setter protected Type type = Type.ONE_SHOT;
    protected Recording recording = new Recording(0, true);
    protected Integer length;
    protected float env = 1f;
    @Setter protected float velocity = 1f;
    @Setter protected boolean active;
    protected final AtomicInteger tapeCounter = new AtomicInteger();

    protected float[][] recordedBuffer;

    public AudioTrack(String name) {
    	this(name, Type.ONE_SHOT);
    }
    
	public AudioTrack(String name, Type type) {
    	super(name, true);
    	setType(type);
	}

	public boolean hasRecording() {
        return recording != null && length != null && length > 1;
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

    protected void playFrame(FloatBuffer[] in, FloatBuffer outLeft, FloatBuffer outRight) {
    	float[] workL = in[LEFT_CHANNEL].array();
    	float[] workR = in[RIGHT_CHANNEL].array();
    	if (recordedBuffer == null) return;
        // gain & pan stereo
    	float baseVol = env * velocity * gain.getGain();
        AudioTools.processGain(recordedBuffer[LEFT_CHANNEL], workL, baseVol * (1f - getPan()));
        AudioTools.processGain(recordedBuffer[RIGHT_CHANNEL], workR, baseVol * getPan());

        cutFilter.process(in[LEFT_CHANNEL], in[RIGHT_CHANNEL]);   
        hiCut.process(in[LEFT_CHANNEL], in[RIGHT_CHANNEL]);

        if (chorus.isActive())
            chorus.processStereo(in[LEFT_CHANNEL], in[RIGHT_CHANNEL]);

        if (compression.isActive()) {
        	compression.process(in[LEFT_CHANNEL]);
        	compression.process(in[RIGHT_CHANNEL]);
        }
        
        if (overdrive.isActive()) {
            overdrive.processAdd(in[LEFT_CHANNEL]);
            overdrive.processAdd(in[RIGHT_CHANNEL]);
        }

        if (eq.isActive()) {
            eq.process(workL, true);
            eq.process(workR, false);
        }

        if (delay.isActive()) {
            delay.processAdd(in[LEFT_CHANNEL], in[LEFT_CHANNEL], true);
            delay.processAdd(in[RIGHT_CHANNEL], in[RIGHT_CHANNEL], false);
        }
		if (reverb.isActive()) 
			reverb.process(in[LEFT_CHANNEL], in[RIGHT_CHANNEL]);
		
		AudioTools.mix(in[LEFT_CHANNEL], outLeft);
		AudioTools.mix(in[RIGHT_CHANNEL], outRight);
    }
    
    @Override // Loop has more sophisticated version
	public void readRecordedBuffer() { 
    	int updated = tapeCounter.get();
        recordedBuffer = recording.get(updated);
        if (++updated == recording.size()) {
            updated = 0;
        	if (type == Type.ONE_SHOT) {
            	active = false;
            	MainFrame.update(this);
            }
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
