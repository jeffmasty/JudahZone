package net.judah.channel;

import java.io.File;
import java.io.IOException;

import judahzone.gui.Icons;
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
	public void process(float[] l, float[] r) {
        hotSwap();
        gain.process(l, r);
        active.forEach(fx->fx.process(l, r));

        if (tape != null)
            tape.offer(l, r);
        if (copy) { // put out a read buffer like other channels offer (RMS meters)
            copy = false;
            System.arraycopy(l, 0, this.left, 0, l.length);
            System.arraycopy(r, 0, this.right, 0, r.length);
        }

        // publish RMS for buffers processed on-thread
        computeRMS(l, r);
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