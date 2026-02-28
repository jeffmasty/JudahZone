package net.judah.gui.drums.views;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import judahzone.gui.Gui;
import judahzone.util.Constants;
import lombok.Getter;
import net.judah.drums.Drum;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumType;
import net.judah.drums.fm.FMOsc;
import net.judah.drums.noise.NoiseOsc;
import net.judah.drums.pluck.PluckOsc;
import net.judah.drums.wav.WavDrum;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.drums.NotePad;
import net.judah.gui.drums.OscTuning;
import net.judah.gui.widgets.Arrow;

public class DrumFrame extends JFrame {

	protected static final boolean LEFT = false;
	protected static final boolean RIGHT = true;

	protected final DrumKit kit;
	@Getter protected DrumView<?> current;
	protected NotePad tap;

	protected JPanel header;

	public DrumFrame(DrumKit kit, Drum drum) {
		this(kit, factory(drum));
	}

	public DrumFrame(DrumKit kit, DrumView<?> initial) {
		this.kit = kit;
		this.current = initial;
		this.tap = new NotePad(kit, initial.getDrum().getType());


		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		initKeys();
		initLayout(initial);
		setTitle(kit.getClass().getSimpleName());
		pack();
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
		TabZone.getFrames().add(this);
	}

	@Override
	public void dispose() {
		TabZone.getFrames().remove(this);
		super.dispose();
	}

	private Box toolbar() {
		Box toolbar = new Box(BoxLayout.X_AXIS);
		toolbar.add(Box.createHorizontalStrut(4));
		toolbar.add(new Arrow(Arrow.WEST, x -> rotary(LEFT)));
		toolbar.add(Box.createHorizontalStrut(3));
		toolbar.add(Gui.resize(tap, Size.SMALLER));
		toolbar.add(Box.createHorizontalStrut(3));
		toolbar.add(new Arrow(Arrow.EAST, x -> rotary(RIGHT)));
		toolbar.add(Box.createHorizontalStrut(4));
		OscTuning tuning = current.getTuning();
		if (tuning != null) {
			toolbar.add(tuning);
			toolbar.add(Box.createHorizontalStrut(1));
		}
		return toolbar;
	}

	private void initKeys() {
		JRootPane root = getRootPane();
		root.registerKeyboardAction(e -> dispose(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		root.registerKeyboardAction(e -> tap.sound(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		root.registerKeyboardAction(e -> rotary(LEFT),
			KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		root.registerKeyboardAction(e -> rotary(RIGHT),
			KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private void initLayout(DrumView<?> initial) {
		getContentPane().setLayout(new BorderLayout(5, 10));
		getContentPane().add(toolbar(), BorderLayout.NORTH);
		getContentPane().add(createMainContent(initial), BorderLayout.CENTER);
	}

	/** Override to customize content layout (e.g., split panes for FM). */
	protected JComponent createMainContent(DrumView<?> view) {
		return view;
	}


	public void setCurrent(DrumView<?> view) {
		current = view;
		current.update();
		tap.rebase(current.getDrum().getType());
		getContentPane().removeAll();
		getContentPane().add(toolbar(), BorderLayout.NORTH);
		getContentPane().add(createMainContent(view), BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	protected void rotary(boolean direction) {
		int idx = current.getDrum().getType().ordinal();
		int next = Constants.rotary(idx, DrumType.values().length, direction);
		setCurrent(factory(kit.get(DrumType.values()[next])));
	}

	protected static DrumView<?> factory(Drum drum) {
		if (drum instanceof NoiseOsc noise)
			return new NoiseView(noise);
		else if (drum instanceof WavDrum sample)
			return new WavView(sample);
		else if (drum instanceof FMOsc fm)
			return new FMView(fm);
		else if (drum instanceof PluckOsc pluck)
			return new PluckView(pluck);
		else
			throw new IllegalArgumentException("Unknown drum type: " + drum.getClass());
	}
}
