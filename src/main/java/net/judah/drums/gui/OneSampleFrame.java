// language: java
package net.judah.drums.gui;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import judahzone.gui.Gui;
import judahzone.util.Constants;
import net.judah.drums.DrumType;
import net.judah.drums.oldschool.DrumSample;
import net.judah.drums.oldschool.OldSchool;
import net.judah.gui.Size;
import net.judah.gui.widgets.Arrow;

public class OneSampleFrame extends OneFrame {

	private static OneSampleFrame instance;

	private OneSampleView current;
	private final OldSchool container;

	private final Arrow left = new Arrow(SwingConstants.WEST, x -> rotary(LEFT));
	private final Arrow right = new Arrow(SwingConstants.EAST, x -> rotary(RIGHT));
	private final JLabel name = new JLabel("Sample", JLabel.CENTER);
	private final JPanel header;

	private OneSampleFrame(OldSchool kit, DrumSample open) {
		this.container = kit;

		name.setBorder(Gui.SUBTLE);
		Gui.resize(name, Size.COMBO_SIZE);
		header = Gui.wrap(left, name, right);

		setTitle("Sample Drum");

		getContentPane().setLayout(new BorderLayout(5, 10));
		setCurrent(open);
		pack();
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
	}

	public static void load(OldSchool kit, DrumSample sample) {
		if (instance == null || !instance.isDisplayable())
			instance = new OneSampleFrame(kit, sample);
		else
			instance.setCurrent(sample);
	}

	protected void rotary(boolean direction) {
		if (current == null) return;
		DrumType type = currentSampleType();
		int idx = type.ordinal();
		int total = DrumType.values().length;
		int next = Constants.rotary(idx, total, direction);
		DrumSample target = container.getSample(DrumType.values()[next]);
		setCurrent(target);
	}

	private void setCurrent(DrumSample sample) {
		Container mine = getContentPane();
		mine.removeAll();

		current = new OneSampleView(sample);
		DrumType type = sample.getDrumType();
		if (tap != null)
			name.removeMouseListener(tap);
		tap = new NotePad(container, (byte) type.getData1(), container.getChannel());
		name.addMouseListener(tap);
		name.setText(type.name());

		mine.add(header, BorderLayout.NORTH);
		mine.add(current, BorderLayout.CENTER);
		validate();
		repaint();
	}

	private DrumType currentSampleType() {
		if (current == null) return DrumType.Kick; // fallback
		// access underlying DrumSample via reflection-free public getter on the view's sample:
		// OneSampleView doesn't expose a getter, so use current view's components to infer type.
		// Instead, store the sample in the current view by constructing it above and keeping reference:
		try {
			java.lang.reflect.Field f = OneSampleView.class.getDeclaredField("s");
			f.setAccessible(true);
			return ((DrumSample) f.get(current)).getDrumType();
		} catch (Exception ex) {
			// reflection fallback in case of unexpected access issues
			return DrumType.Kick;
		}
	}
}
