package net.judah.drumkit;

import static judahzone.util.Constants.LEFT;
import static judahzone.util.Constants.RIGHT;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.ShortMessage;

import judahzone.api.Asset;
import judahzone.api.PlayAudio;
import judahzone.api.Played;
import judahzone.fx.Gain;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.Recording;
import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.midi.Actives;

/**
 * Updated to implement the new PlayAudio.setRecording(Asset) API.
 * - setRecording(Asset) bridges to setRecording(Recording) by using the Asset's recording
 *   or loading via DrumDB when necessary. Keeps backward-compatible setRecording(Recording).
 */
@Getter
public class DrumSample implements PlayAudio {

    protected final FloatBuffer left = FloatBuffer.wrap(new float[Constants.bufSize()]);
    protected final FloatBuffer right = FloatBuffer.wrap(new float[Constants.bufSize()]);

    private final DrumEnvelope envelope;
    private final DrumType drumType;
    private final Gain gain;
    private final Actives actives;
    @Setter protected float velocity = 1f;
    @Setter protected boolean onMute;
    @Setter protected Played played; // not used

    protected File file;
    protected boolean playing;
    protected Recording recording = new Recording();
    protected final AtomicInteger tapeCounter = new AtomicInteger(0);
    @Setter @Getter protected Type type = Type.ONE_SHOT;

    protected float[][] playBuffer;
    @Setter protected float env = 1f; // envelope/boost

    public DrumSample(DrumType type, Actives actives, KitSetup setup) {
        this.drumType = type;
        this.actives = actives;
        this.gain = setup.gain[type.ordinal()];
        this.envelope = new DrumEnvelope(setup, type.ordinal());
    }

//    /**
//     * Backwards-compatible: set file (may lazy-load via DrumDB) and ensure Asset registered.
//     */
//    public void setFile(File f) throws Exception {
//        if (f == null) {
//            this.file = null;
//            setRecording((Recording) null);
//            return;
//        }
//        this.file = f;
//        Recording rec = DrumDB.get(f); // may lazy-load
//        setRecording(rec);
//
//        // ensure there's an Asset registered for UI/inspection
//        Asset existing = DrumDB.assetFor(f);
//        if (existing == null) {
//            Asset a = new Asset(stripName(f), f, rec, rec == null ? 0L : rec.size() * Constants.bufSize(), Asset.Category.DRUMS);
//            DrumDB.registerAsset(a);
//        }
//    }

    /**
     * New PlayAudio API: set recording from Asset.
     * Bridges to existing setRecording(Recording).
     */
    @Override
    public void setRecording(Asset asset) {
        if (asset == null) {
            setRecording((Recording) null);
            return;
        }
        this.file = asset.file();
        setRecording(asset.recording());
    }

    /**
     * Existing API kept for compatibility: sets the in-memory Recording directly.
     */
    public void setRecording(Recording sample) {
        rewind();
        recording = sample == null ? new Recording() : sample;
    }

    public String stripName(File f) {
        String n = f.getName();
        int idx = n.lastIndexOf('.');
        return idx > 0 ? n.substring(0, idx) : n;
    }

    @Override
    public void rewind() {
        tapeCounter.set(0);
    }

    @Override
    public void play(boolean play) {
        this.playing = play;
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    public void reset() {
        tapeCounter.set(0);
        playing = false;
    }

    public void off() {
        playing = false;
        ShortMessage m = actives.find(drumType.getData1());
        if (m != null)
            actives.remove(m);
        MainFrame.update(actives);
    }

    protected void readRecordedBuffer() {
        int frame = tapeCounter.getAndIncrement();
        if (frame + 1 >= recording.size()) {
            tapeCounter.set(0);
            off();
        }
        playBuffer = recording.get(frame);
    }

    @Override
    public int getLength() {
        return recording.size();
    }

    @Override
    public final float seconds() {
        return getLength() / Constants.fps();
    }

    public void clear() {
        setRecording((Recording) null);
        playing = false;
        file = null;
    }

    @Override
    public final void setSample(long sample) {
        if (recording == null || recording.size() == 0 || sample <= 0) {
            tapeCounter.set(0);
            return;
        }

        int frames = recording.size();
        int samples = frames * Constants.bufSize();
        if (sample > samples)
            sample = samples - 1;
        int frame = (int) ((sample / (float) samples) * frames);
        tapeCounter.set(frame);
    }

    public void process(float[] outLeft, float[] outRight) {
        if (!playing) return;
        readRecordedBuffer();
        if (onMute)
            return;
        env = velocity * envelope.calcEnv();
        AudioTools.replace(playBuffer[LEFT], left, env * gain.getLeft() * gain.getGain());
        AudioTools.replace(playBuffer[RIGHT], right, env * gain.getRight() * gain.getGain());
        AudioTools.mix(left, outLeft);
        AudioTools.mix(right, outRight);
    }
}
