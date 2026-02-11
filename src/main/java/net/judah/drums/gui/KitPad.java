package net.judah.drums.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.function.Supplier;

import javax.sound.midi.ShortMessage;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import judahzone.api.MidiConstants;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.widgets.Knob;
import judahzone.widgets.Knob.KnobListener;
import lombok.Getter;
import net.judah.drums.DrumType;
import net.judah.drums.KitDB.BaseParam;
import net.judah.drums.gui.SampleDrums.Modes;
import net.judah.drums.oldschool.DrumSample;
import net.judah.drums.oldschool.OldSchool;
import net.judah.drums.oldschool.SampleParams;
import net.judah.midi.Actives;

public class KitPad extends Gui.Opaque implements KnobListener {

	private static final Color borderColor = Pastels.PURPLE;

	@Getter private final DrumSample drum;
	private final Supplier<Modes> mode;
	private final Knob knob;
	private final JPanel top = new Gui.Opaque();
	private final JPanel bottom = new Gui.Opaque();
	private JCheckBox choke; // ohats only
	private final OldSchool beatBox;
	private final int data1;

	public KitPad(OldSchool drums, DrumSample drum, Supplier<Modes> modes) {

		this.beatBox = drums;
		this.drum = drum;
		final DrumType type = drum.getDrumType();
		this.data1 = type.getData1();
		this.mode = modes;

		knob = new Knob(this);
		knob.setKnobColor(Pastels.RED);
		if (drum.getDrumType() == DrumType.OHat) {
			choke = new JCheckBox();
			choke.setToolTipText("Stop OHat when CHat plays");
			top.add(choke);
			choke.addItemListener(e->beatBox.setChoked(choke.isSelected()));
		}
		addMouseListener(new NotePad(beatBox, (byte)data1, beatBox.getChannel()));
		setBorder(new LineBorder(borderColor, 1));
		setLayout(new GridLayout(0, 1, 0, 0));
		top.add(new JLabel(type.name()));
		bottom.add(knob);
		add(top);
		add(bottom);
	}

	void background(Actives a) {
		ShortMessage msg = a.find(data1);
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
		final SampleDrums.Modes m = mode.get();
		if (m == null) return;
		final BaseParam p = BaseParam.values()[m.ordinal()];
		SampleParams.set(new SampleParams(drum, p, value));
	}

	public void update() {
		if (choke != null && beatBox.isChoked() != choke.isSelected())
				choke.setSelected(beatBox.isChoked());
		updateMode();
	}

	public void updateMode() {
		int current = knob.getValue();
		final BaseParam p = BaseParam.values()[mode.get().ordinal()];
		int value = SampleParams.get(drum, p);
		if (current != value)
			knob.setValue(value);
	}


}