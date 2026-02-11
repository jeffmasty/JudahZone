package net.judah.drums.gui;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import judahzone.gui.Gui;
import judahzone.util.Constants;
import net.judah.drums.DrumType;
import net.judah.drums.synth.DrumOsc;
import net.judah.drums.synth.DrumSynth;
import net.judah.gui.Size;
import net.judah.gui.widgets.Arrow;

public class OneDrumFrame extends OneFrame {


	private static OneDrumFrame instance;

	private OneDrumView current;
	private final DrumSynth container;

	private final Arrow left = new Arrow(SwingConstants.WEST, x->rotary(LEFT));
	private final Arrow right = new Arrow(SwingConstants.EAST, x->rotary(RIGHT));
	private final JLabel name = new JLabel("Tap", JLabel.CENTER);
	private final JPanel header;


	private OneDrumFrame(OneDrumView view, DrumSynth synth) {
		container = synth;

		name.setBorder(Gui.SUBTLE);
		Gui.resize(name, Size.COMBO_SIZE);
		header = Gui.wrap(left, name, right);

		setTitle("Zone Drum");

		getContentPane().setLayout(new BorderLayout(5, 10));
		setCurrent(view);
		pack();
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);

	}

	public static void load(DrumOsc drum, DrumSynth synth) {
		if (instance == null || !instance.isDisplayable())
			instance = new OneDrumFrame(new OneDrumView(drum), synth);
		else
			instance.setCurrent(new OneDrumView(drum));
	}

	@Override
	protected void rotary(boolean direction) {
		// move to next drumOsc
		int idx = current.getDrum().getType().ordinal();
		int total = DrumType.values().length;
		int next = Constants.rotary(idx, total, direction);
		DrumOsc target = container.getDrum(DrumType.values()[next]);
		setCurrent(new OneDrumView(target));
	}

	private void setCurrent(OneDrumView view) {
		Container mine = getContentPane();
		mine.removeAll();

		current = view;
		DrumType type = current.getDrum().getType();
		if (tap != null)
			name.removeMouseListener(tap);
		tap = new NotePad(container,  (byte)type.getData1(), container.getChannel());
		name.addMouseListener(tap);
		name.setText(type.name());
		mine.add(header, BorderLayout.NORTH);
		mine.add(current, BorderLayout.CENTER);
		validate();
		repaint();
	}

}
