package net.judah.channel;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import judahzone.gui.Icons;
import judahzone.util.AudioTools;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.looper.ToDisk;

/**The unified effects/volume track just before hitting the speakers/external effects.
 * A master track initializes in a muted state.*/
public class Mains extends Channel {

    public static final float PREAMP = 13f;

    @Getter private ToDisk tape;
    @Getter private boolean hotMic;
    @Setter private boolean copy;

    public Mains() {
        super(Mains.class.getSimpleName(), true);
        icon = Icons.get("Speakers.png");
    }

    @Override
    public void processImpl() {
    }

    @Override
	public void process(FloatBuffer left, FloatBuffer right) {
        hotSwap();
        gain.process(left, right);
        active.forEach(fx->fx.process(left, right));

        if (tape != null)
            tape.offer(left, right);
        if (copy) { // put out a read buffer like other channels offer (RMS meters)
            copy = false;
            AudioTools.copy(left, getLeft().array());
            AudioTools.copy(right, getRight().array());
        }

        // publish RMS for buffers processed on-thread
        computeRMS(left, right);
    }

    public boolean isRecording() { return tape != null; }

    public void tape(boolean openFileDialog) {
        if (tape == null && openFileDialog) {
            File f = Folders.choose(new File(System.getProperty("user.home")));
            if (f == null) return;
            try {
                tape = new ToDisk(f);
            } catch (IOException e) { RTLogger.warn(this, e); }
        }
        else
            tape();
    }

    public void tape() {
        try {
            if (tape == null)
                tape = new ToDisk();
            else {
                ToDisk old = tape;
                tape = null;
                old.close();
            }
        } catch (IOException e) { RTLogger.warn(this, e); }
    }

    public void hotMic() {
        hotMic = !hotMic;
        MainFrame.update(this);
    }

}