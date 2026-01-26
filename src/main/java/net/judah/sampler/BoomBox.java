package net.judah.sampler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import judahzone.api.Asset;
import judahzone.api.PlayAudio;
import judahzone.api.Played;
import judahzone.gui.Gui;
import judahzone.jnajack.BasicPlayer;
import judahzone.util.AudioMetrics;
import judahzone.util.AudioMetrics.RMS;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.MP3;
import judahzone.util.Memory;
import judahzone.util.Recording;
import lombok.Getter;
import net.judah.gui.Size;
import net.judah.gui.widgets.RMSImage;

/** Sampler's audio player UI—play/loop/gain controls + RMS meter visualization */
public class BoomBox extends JPanel implements PlayAudio, Played {
	private static final Dimension RMSSize = new Dimension(Size.WIDTH_KNOBS - 10, (int) (Size.HEIGHT_KNOBS * (2f / 3f)));

	@Getter private final BasicPlayer player = new BasicPlayer();

	private JToggleButton playBtn;
	private JToggleButton loopBtn;
	private JSlider volSlider;
	private JLabel filename;
	private RMSHolder rmsDisplay;
	private int headX = 0;

	public BoomBox(Sampler sampler) {
		player.setPlayed(this);
		sampler.add(player);
		SwingUtilities.invokeLater(this::gui);
	}

	private void gui() {
		// setBorder(Gui.SUBTLE);
		// Play button
		playBtn = new JToggleButton("▶️");
		playBtn.addActionListener(e -> play(!player.isPlaying()));

		// Volume slider: 0..100 maps to 0.0..1.0 gain
		volSlider = new JSlider(0, 100, 50);
		Gui.resize(volSlider, Size.MODE_SIZE);
		volSlider.setToolTipText("Output gain: 0% .. 100%");
		volSlider.addChangeListener(e -> {
			float gain = volSlider.getValue() / 100f;
			player.setEnv(gain);
		});

		// Loop button
		loopBtn = new JToggleButton("🔁");
		loopBtn.addActionListener(e -> {
			player.setType(loopBtn.isSelected() ? Type.LOOP : Type.ONE_SHOT);
		});

		// Filename label (click to load)
		filename = new JLabel(" (load) ");
		Gui.resize(filename, Size.TITLE_SIZE);
		filename.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) { load(); }
		});
		filename.setBorder(Gui.SUBTLE);

		JPanel top = new JPanel();
		top.add(filename);
		top.add(playBtn);
		top.add(volSlider);
		top.add(loopBtn);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));
		add(top);
		rmsDisplay = new RMSHolder();
		Gui.resize(rmsDisplay, RMSSize);
		add(Box.createVerticalStrut(10));
		add(rmsDisplay);
		add(Box.createVerticalStrut(15));
	}

	private void load() {
		File f = Folders.choose(Folders.getLoops());
		if (f == null) return;

		if (!Memory.check(f)) return;

		Recording recording = MP3.load(f);

		// Analyze for RMS visualization
		RMS[] db = new RMS[recording.size()];
		for (int i = 0; i < recording.size(); i++) {
			float[][] buf = recording.get(i);
			RMS left = AudioMetrics.analyze(buf[0]);
			RMS right = AudioMetrics.analyze(buf[1]);
			// largest left or right
			db[i] = left.rms() >= right.rms() ? left : right;
		}
		rmsDisplay.setRMS(db);

		Asset asset = new Asset(f.getName(), f, recording,
			recording.size(), Asset.Category.USER);
		setRecording(asset);

		filename.setText(f.getName());
	}

	@Override
	public void setRecording(Asset a) {
		player.setRecording(a);
	}

	@Override
	public void play(boolean onOrOff) {
		player.play(onOrOff);
		playState();
	}

	@Override
	public boolean isPlaying() {
		return player.isPlaying();
	}

	@Override
	public int getLength() {
		return player.getLength();
	}

	@Override
	public float seconds() {
		return player.seconds();
	}

	@Override
	public void rewind() {
		player.rewind();
	}

	@Override
	public void setEnv(float env) {
		player.setEnv(env);
	}

	@Override
	public void setType(Type type) {
		player.setType(type);
	}

	@Override
	public void setSample(long sampleFrame) {
		player.setSample(sampleFrame);
	}

	@Override
	public void setPlayed(Played p) {
		player.setPlayed(p);
	}

	@Override
	public void setHead(long sample) {
		// only repaint if pixel of head has changed
		int frames = player.getLength();
		if (frames <= 0) return;
		int framePos = (int) (sample / Constants.bufSize());
		int newHeadX = (int) ((framePos / (float) frames) * rmsDisplay.getWidth());
		if (newHeadX != headX) {
			headX = newHeadX;
			rmsDisplay.repaint();
		}
	}

	@Override
	public void playState() {
		SwingUtilities.invokeLater(() -> {
			boolean playing = player.isPlaying();
			playBtn.setSelected(playing);
			playBtn.setText(playing ? "❚❚" : "▶️");
		});
	}

	/** MIDI knob input: idx 0=volume, 1=playhead (seek), 2+=other */
	public void doKnob(int idx, final int value) {
		if (idx == 0) {
			SwingUtilities.invokeLater(() -> volSlider.setValue(value));
		} else
			rmsDisplay.doKnob(idx, value);
	}


	private class RMSHolder extends JPanel implements Gui.Mouse {
		private RMSImage rms;

		public RMSHolder() {
			addMouseListener(this);
			setBorder(Gui.SUBTLE);
			rms = new RMSImage(RMSSize);
		}

		public void doKnob(int idx, int value) {

			if (idx == 6)
				rms.setYScale(value * 0.01f);
			else if (idx == 7)
				rms.setIntensity(value * 0.01f);
			else if (idx == 1)
				seek(value);
			// in RangeSlider style
			// 4 = left zoom point
			// 5 == right zoom point
			repaint();
		}

		public void setRMS(RMS[] db) {
			rms.setRMS(db);
			repaint();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			player.setSample((int) ((e.getX() / (float) getWidth()) * player.getLength()));
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			int percent = (int) ((e.getX() / (float) getWidth()) * 100);
			seek(percent);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int percent = (int) ((e.getX() / (float) getWidth()) * 100);
			seek(percent);
		}


		public void seek(int percent) {
			int frames = player.getLength();
		    if (frames <= 0) return;

		    int clickFrame = (int) ((percent / 100f) * frames);
		    long sampleFrame = (long) clickFrame * Constants.bufSize();
		    player.setSample(sampleFrame);
		}

		@Override protected void paintComponent(Graphics g) {
	        super.paintComponent(g);
	        if (rms != null)
	        	g.drawImage(rms, 0, 0, getWidth(), getHeight(), null);
	        // paint head as vertical line: normalize tapeCounter to pixel
	        g.setColor(Color.RED);
	        int frames = player.getLength();
	        if (frames > 0) {
	            int framePos = player.getTapeCounter().get();
	            headX = (int) ((framePos / (float) frames) * getWidth());
	            g.drawLine(headX, 0, headX, getHeight());
	        }
	    }
	}

}