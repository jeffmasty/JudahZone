package net.judah.gui.drums.views;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import judahzone.api.DrumType;
import judahzone.drums.DefaultKits;
import judahzone.drums.Drum;
import judahzone.drums.DrumJson;
import judahzone.drums.DrumsetJson;
import judahzone.drums.ZoneDrums;
import judahzone.drums.fm.FMOsc;
import judahzone.drums.noise.NoiseOsc;
import judahzone.drums.pluck.PluckOsc;
import judahzone.drums.wav.WavDrum;
import judahzone.drums.wav.WavDrumDB;
import judahzone.gui.Gui;
import judahzone.util.Constants;
import lombok.Getter;
import net.judah.drums.Drumset;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.drums.NotePad;
import net.judah.gui.widgets.Arrow;

public final class DrumFrame extends JFrame {

	private static final boolean LEFT = false;
	private static final boolean RIGHT = true;

	private final Drumset kit;
	@Getter private DrumView<?> current;

	private NotePad tap;
	private JComboBox<ZoneDrums> registration = new JComboBox<>(ZoneDrums.values());
	protected JPanel header;

	public DrumFrame(Drumset kit, Drum drum) {
		this.kit = kit;
		this.current = factory(drum);
		this.tap = new NotePad(kit, current.getDrum().getType());

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		initKeys();
		initLayout(current);
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

		toolbar.add(registration);
		registration.setSelectedItem(ZoneDrums.ofDrum(current.getDrum()));
		registration.addActionListener(e -> {
			DrumType type = current.drum.getType();
			ZoneDrums selected = (ZoneDrums) registration.getSelectedItem();
			dispose(); // close old drum/layout frame

			DrumJson next;
			if (selected == ZoneDrums.Wav) {
				DrumsetJson waves = DefaultKits.specifyWavKit(WavDrumDB.getPatches()[0], 0);
				next = waves.drums()[type.ordinal()];
			} else {
				next = DefaultKits.get(selected, type);
			}
			kit.replace(next);
			// re-open frame with new drum.
			new DrumFrame(kit, kit.get(type));
		});

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
		registration.setSelectedItem(ZoneDrums.ofDrum(current.getDrum()));
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
