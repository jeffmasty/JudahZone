package net.judah.gui.knobs;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.sound.midi.ShortMessage;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.widgets.Knob;
import judahzone.widgets.Knob.KnobListener;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.KitSetup;
import net.judah.midi.Actives;
import net.judah.midi.JudahMidi;

public class KitPad extends Gui.Opaque implements KnobListener {

	public static enum Modes {
		Volume, Pan, Attack, Decay; // Dist, pArTy;
	}

	private static final Color borderColor = Pastels.PURPLE;

	private final DrumType type;
	private final int idx;
	private final JComboBox<Modes> mode;
	private final Knob knob;
	private final JPanel top = new Gui.Opaque();
	private final JPanel bottom = new Gui.Opaque();
	private JCheckBox choke; // ohats only
	private final DrumMachine beatBox;

	public KitPad(DrumMachine drums, DrumType t, JComboBox<Modes> modes) {

		this.beatBox = drums;
		this.type = t;
		this.idx = t.ordinal();
		this.mode = modes;

		knob = new Knob(this);
		knob.setKnobColor(Pastels.RED);
		if (type == DrumType.OHat) {
			choke = new JCheckBox();
			choke.setToolTipText("Stop OHat when CHat plays");
			top.add(choke);
			choke.addItemListener(e->drums.getCurrent().getKit().setChoked(choke.isSelected()));
		}
		addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				DrumKit kit = drums.getCurrent().getKit();
				DrumSample s = kit.getSamples()[idx];
				Midi click = Midi.create(Midi.NOTE_ON, kit.getChannel(), s.getDrumType().getData1(), 100);
				kit.send(click, JudahMidi.ticker());
			}
		});

		setBorder(new LineBorder(borderColor, 1));
		setLayout(new GridLayout(0, 1, 0, 0));
		top.add(new JLabel(type.name()));
		bottom.add(knob);
		add(top);
		add(bottom);
	}

	void background(Actives a) {
		ShortMessage msg = a.find(type.getData1());
		top.setBackground(msg == null ? null : padColor(msg));
	}

	private static final int _CH = MidiConstants.DRUM_CH;
	private Color padColor(ShortMessage msg) {
		switch(msg.getChannel()) {
			case _CH: return Pastels.GREEN;
			case _CH + 1: return Pastels.BLUE;
			case _CH + 2 : return Pastels.PURPLE;
			default: return Pastels.RED;
		}
	}

	@Override public void knobChanged(int value) {
		KitSetup settings = beatBox.getSettings();
		switch((Modes)mode.getSelectedItem()) {
			case Volume:
				if (settings.getVol(idx) != value)
					settings.setVol(idx, value);
				break;
			case Pan:
				if (settings.getPan(idx) != value)
					settings.setPan(idx, value);
				break;
			case Attack:
				if (settings.getAtk(idx) != value)
					settings.setAtk(idx, value);
				break;
			case Decay:
				if (settings.getDk(idx) != value)
					settings.setDk(idx, value);
				break;
		}
	}

	public void update() {
		if (choke != null)
			if (beatBox.getCurrent().getKit().isChoked() != choke.isSelected())
				choke.setSelected(beatBox.getCurrent().getKit().isChoked());
		updateMode((Modes)mode.getSelectedItem());
	}

	public void updateMode(Modes mode) {
		int current = knob.getValue();
		KitSetup settings = beatBox.getSettings();
		switch(mode) {
			case Volume:
				if (settings.getVol(idx) != current)
					knob.setValue(settings.getVol(idx));
				break;
			case Pan:
				if (settings.getPan(idx) != current)
					knob.setValue(settings.getPan(idx));
				break;
			case Attack:
				if (settings.getAtk(idx) != current)
					knob.setValue(settings.getAtk(idx));
				break;
			case Decay:
				if (settings.getDk(idx) != current)
					knob.setValue(settings.getDk(idx));
				break;
		}
	}


}