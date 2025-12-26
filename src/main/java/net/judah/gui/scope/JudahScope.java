package net.judah.gui.scope;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import be.tarsos.dsp.util.fft.HammingWindow;
import net.judah.JudahZone;
import net.judah.gui.Detached.Floating;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.omni.AudioTools;
import net.judah.omni.Recording;
import net.judah.omni.Threads;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/** Provides a Spectrometer, a Spectrogram and RMSmeter, listening to mixer's selected channels
 * (on a circular 20 sec buffer) or analyze a .wav file from disk */
public class JudahScope extends JPanel implements Live, Floating, Closeable {

	public static enum Mode{ LIVE_ROLLING, LIVE_STOPPED, FILE }

	static final int FFT_SIZE = Constants.fftSize();
	/** our transformer */
	public static final FFZ fft = new FFZ(FFT_SIZE, new HammingWindow());
	static final int JACK_BUFFER = Constants.bufSize();
	static final int S_RATE = Constants.sampleRate();
	static final int CHUNKS = FFT_SIZE / JACK_BUFFER;
	static final int TRANSFORM = FFT_SIZE * 2;
	static final int AMPLITUDES = FFT_SIZE / 2;
    private static final int INSET = 90;
    private static final int SPEC_DIFF = 300;

	private int w = Size.WIDTH_TAB - INSET;
	private Mode status = Mode.LIVE_STOPPED;
    private final Spectrometer spectrum;
	private final TimeDomain pausedDisplay = new TimeDomain(this, w);
    private final TimeDomain liveDisplay = new TimeDomain(pausedDisplay, w);
	private TimeDomain fileDisplay;
	private TimeDomain timeDomain = pausedDisplay;
	private final float[] transformBuffer = new float[TRANSFORM];
	// short recording buffer (4 jack_frames = 1 fft_frame)
	private Recording realtime = new Recording();
	private File file;

    // Controls
    private JPanel wrap;
	private final JToggleButton liveBtn = new JToggleButton("Live", /* inactive, */true);
    private final JToggleButton zoneBtn = new JToggleButton("Pause", false);
    private final JToggleButton fileBtn = new JToggleButton(" File ", false);
    private final JLabel feedback = new JLabel(" (load) ", JLabel.CENTER);

	/** dynamic box that will hold the current TimeDomain's controls panel */
	private final Box timeControlsBox = new Box(BoxLayout.X_AXIS);

	public JudahScope() {
		setName("JudahScope");
		spectrum = new Spectrometer(new Dimension(w, Size.HEIGHT_TAB - SPEC_DIFF), liveBtn);
		timeDomain.resize(w);

		feedback.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { load(); }});
		liveBtn.addActionListener(l ->setMode(Mode.LIVE_ROLLING));
		zoneBtn.addActionListener(l ->setMode(Mode.LIVE_STOPPED));
		fileBtn.addActionListener(l ->setMode(Mode.FILE));
		ButtonGroup gp = new ButtonGroup();
		gp.add(liveBtn); gp.add(zoneBtn); gp.add(fileBtn);

		// menu();
		Box menu = new Box(BoxLayout.X_AXIS);
		menu.add(Gui.box(liveBtn, zoneBtn, fileBtn));
		menu.add(Gui.resize(feedback, Size.TITLE_SIZE));
		menu.add(spectrum.getControls());

		// initial TimeDomain controls
		updateTimeControls(timeDomain);

		menu.add(timeControlsBox);
		menu.add(Box.createHorizontalGlue());

		// buildLayout();
		wrap = Gui.wrap(timeDomain);
		Box content = new Box(BoxLayout.Y_AXIS);
		content.add(menu, new Dimension(Size.WIDTH_TAB, 36));
		content.add(Box.createVerticalStrut(6));
		content.add(Gui.resize(spectrum, new Dimension(w, spectrum.getHeight())));
		content.add(Box.createVerticalStrut(12));
		content.add(wrap);
		content.add(Box.createVerticalStrut(1));
		setLayout(new GridLayout(1, 1));
		add(Gui.wrap(content));
	}

	/** Swap the controls panel to match the active TimeDomain. */
	private void updateTimeControls(TimeDomain td) {
		timeControlsBox.removeAll();
		if (td != null) {
			timeControlsBox.add(td.getControls());
		}
		timeControlsBox.revalidate();
		timeControlsBox.repaint();
	}

    @Override public void resized(int width, int height) {
		Gui.resize(this, new Dimension(width, height)).setSize(width, height);
        w = width - INSET;
        spectrum.resized(w, height - SPEC_DIFF);
        timeDomain.resize(w);
        revalidate();
    }

    /** load a file off the current thread */
    public void load() {
		file = Folders.choose(Folders.getLoops());
		if (file == null)
			return;
		Threads.execute(() -> {
			Recording recording = new Recording(file); // out-of-memory

			int frames = recording.size() / CHUNKS;
			Transform[] loaded = new Transform[frames];
			final float[] transformBuffer = new float[TRANSFORM];
			long start = System.currentTimeMillis();
			for (int frame = 0; frame < frames; frame++) {
				int idx = frame * FFT_SIZE;
				float[][] snippet = recording.getSamples(idx, FFT_SIZE);
				System.arraycopy(snippet[0], 0, transformBuffer, 0, FFT_SIZE); // mono
				fft.forwardTransform(transformBuffer);
				float[] amplitudes = new float[AMPLITUDES];
				fft.modulus(transformBuffer, amplitudes);
				loaded[frame] = new Transform(amplitudes, RMS.analyze(snippet));
			}
			// TODO tailing samples/zeropad
			long end = System.currentTimeMillis();
			RTLogger.debug(this, file.getName() + " frames: " + frames + " FFT compute millis: " + (end - start));
			fileDisplay = new TimeDomain(this, w, true, loaded);
			install(fileDisplay);
		});
	}

	private void install(TimeDomain time) {
		timeDomain = time;
		timeDomain.generate();
		wrap.removeAll();
		wrap.add(timeDomain);
		updateTimeControls(timeDomain);
		revalidate();
		repaint();
	}

	void setFeedback() {
		switch (status) {
		 	case LIVE_ROLLING -> feedback.setText(Arrays.toString(JudahZone.getSelected().toArray()));
		 	case LIVE_STOPPED -> feedback.setText(" ");
		 	case FILE -> feedback.setText(file == null ? " (load) " : file.getName());
		}
	}

	public void process() {
		if (status != Mode.LIVE_ROLLING)
			return;
		float[][] buf = JudahZone.getMem().getFrame();
		JudahZone.getSelected().forEach(ch->AudioTools.copy(ch, buf));
		realtime.add(buf);
		if (realtime.size() == CHUNKS) {
			MainFrame.update(new LiveData(this, realtime));
			realtime = new Recording();
		}
	}

	@Override public void analyze(Recording buffer) {
		RMS rms = RMS.analyze(new float[][] {buffer.getChannel(0), buffer.getChannel(1)});
		System.arraycopy(buffer.getLeft(), 0, transformBuffer, 0, FFT_SIZE); // mono
		fft.forwardTransform(transformBuffer);
		float[] amplitudes = new float[AMPLITUDES];
		fft.modulus(transformBuffer, amplitudes);

		Transform data = new Transform(amplitudes, rms);
		liveDisplay.analyze(data);
		spectrum.analyze(data);
	}

	public void click(Transform t) {
		if (t == null)
			spectrum.clear();
		else
			spectrum.analyze(t);
	}

	//////////////   MODE   //////////////
	public boolean isActive() { return status == Mode.LIVE_ROLLING; }
    public void setActive(boolean active) {
    	setMode(active ? Mode.LIVE_ROLLING : Mode.LIVE_STOPPED);
    }

    public void setMode(Mode stat) {
        if (stat == status)
            return;
        status = stat;

		if (status == Mode.LIVE_ROLLING) { // goLive();
			liveDisplay.fullRange();
			install(liveDisplay);

			if (liveBtn.isSelected() == false)
				liveBtn.setSelected(true);

		} else { // goDark();

			if (status == Mode.LIVE_STOPPED)
				install(pausedDisplay);
			else if (fileDisplay == null)
				load(); // first time
			else
				install(fileDisplay);
    	}
		setFeedback();
		repaint();
		MainFrame.update(this);
    }

    @Override public void close() throws IOException {
    	if (status == Mode.LIVE_ROLLING)
    		setMode(Mode.LIVE_STOPPED);
    }
}