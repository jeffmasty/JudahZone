package net.judah.drums.wav;

import static judahzone.util.Constants.LEFT;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.ShortMessage;

import judahzone.api.PlayAudio;
import judahzone.api.Played;
import judahzone.api.RTNotes;
import judahzone.data.Asset;
import judahzone.data.Letter;
import judahzone.data.Postage;
import judahzone.data.Recording;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain;
import judahzone.fx.Gain.GainT;
import judahzone.util.Constants;
import lombok.Setter;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.Drum;
import net.judah.drums.Drum.NonPitched;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;

/** Drum sample player with frame\-based attack/decay envelope. */
public class WavDrum extends Drum implements PlayAudio, NonPitched {

    @Setter protected boolean onMute;
    @Setter protected Played played;

    protected File file;
    protected Recording recording = new Recording();
    protected final AtomicInteger tapeCounter = new AtomicInteger(0);
    @Setter protected Type type = Type.ONE_SHOT;
    protected float[][] playBuffer;

    public WavDrum(DrumType type, RTNotes actives) {
        super(new DrumSetup(type,
                new GainT(1f),
                new Postage(1, Letter.MAX_DECAY_MS),
                new Freqs(new Coord(40, 1), new FilterT(440, 1, 0.5f), new Coord(19000, 1)),
                null,
                new String[] {}),
        		actives);
    }

    @Override public void setRecording(Asset asset) {
        if (asset == null) {
            setRecording((Recording) null);
            return;
        }
        this.file = asset.file();
        setRecording(asset.recording());
    }

    protected void setRecording(Recording sample) {
        rewind();
        recording = sample == null ? new Recording() : sample;
    }

    public String stripName(File f) {
        String n = f.getName();
        int idx = n.lastIndexOf('.');
        return idx > 0 ? n.substring(0, idx) : n;
    }

    @Override public void rewind() {
        tapeCounter.set(0);
    }

    @Override
    public void trigger(ShortMessage midi) {
    	super.trigger(midi);
        tapeCounter.set(0);
    }

    @Override
    public void play(boolean play) {
        if (play) {
            trigger(null); // default velocity for UI trigger
        } else {
            off();
        }
    }

    public void reset() {
        tapeCounter.set(0);
        offOff();
    }

    protected void readRecordedBuffer() {
        if (recording == null || recording.isEmpty()) {
            off();
            return;
        }
        int frame = tapeCounter.getAndIncrement();
        if (frame + 1 >= recording.size()) {
            tapeCounter.set(0);
            off();
        }
        playBuffer = recording.get(frame);
    }

    @Override public int getLength() {
        return recording.size();
    }

    @Override public final float seconds() {
        return getLength() / Constants.fps();
    }

    public void clear() {
        setRecording((Recording) null);
        open = false;
        file = null;
    }

    @Override public final void setSample(long sample) {
        if (recording == null || recording.size() == 0 || sample <= 0) {
            tapeCounter.set(0);
            return;
        }
        int frames = recording.size();
        int samples = frames * Constants.bufSize();
        if (sample > samples) sample = samples - 1;
        int frame = (int) ((sample / (float) samples) * frames);
        tapeCounter.set(frame);
    }

    @Override public void setAmp(float env) { gain.setGain(env); }
    public int getVol() { return gain.get(Gain.VOLUME); }
    public int getPan() { return gain.get(Gain.PAN); }

    @Override
    public void processImpl() {
        readRecordedBuffer();
        if (onMute || playBuffer == null)
            return;

        // Safety check for buffer lengths to prevent RT thread exceptions
        float[] src = playBuffer[LEFT];
        int len = Math.min(src.length, N_FRAMES);
        System.arraycopy(src, 0, work, 0, len);
    }

	@Override
	public void set(int idx, int knob) {
		// TODO kit folder "Sample" for DB lookup
	}

	@Override
	public int get(int idx) {
		// TODO
		return 0;
	}
}
