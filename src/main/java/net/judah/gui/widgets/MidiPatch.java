package net.judah.gui.widgets;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.judah.api.ZoneMidi;
import net.judah.controllers.Jamstik;
import net.judah.controllers.MPKmini;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.Updateable;

public class MidiPatch extends JPanel implements Updateable {

	private final MPKmini controller;
	private final JComboBox<ZoneMidi> widget = new JComboBox<ZoneMidi>();
	private final Jamstik jamstik;


	public MidiPatch(MPKmini mpk, Jamstik jam, ZoneMidi[] outs) {
		this.controller = mpk;
		this.jamstik = jam;
		widget.setOpaque(true);
		widget.setRenderer(STYLE);
		for (ZoneMidi out : outs)
			widget.addItem(out);
		Gui.resize(widget, Size.COMBO_SIZE);
		widget.addActionListener(e -> controller.setMidiOut((ZoneMidi)widget.getSelectedItem()));
		add(Gui.wrap(new ClickIt(), widget));
	}



	static final BasicComboBoxRenderer STYLE = new BasicComboBoxRenderer() {
		@Override public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list,
				Object value, int index, boolean isSelected, boolean cellHasFocus) {
			setHorizontalAlignment(SwingConstants.CENTER);
			ZoneMidi item = (ZoneMidi) value;
			setText(item == null ? "?" : item.toString());
			return this;
		}
	};

	public void setMpkOut(ZoneMidi out) {
		if (controller.getMidiOut() != out)
			widget.setSelectedItem(out);
	}
	@Override public void update() {
		if (controller.getMidiOut() != widget.getSelectedItem())
			widget.setSelectedItem(controller.getMidiOut());
	}

	private class ClickIt extends Click implements ActionListener {
		ClickIt() {
			super("JAM");
			addActionListener(this);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if (right)
				jamstik.octaver();
			else
				jamstik.toggle();
		}

	}

}

//	public void capture() {
//	if (captureTrack == null) return;
//	captureTrack.setCapture(!captureTrack.isCapture());
//	if (frame == null)
//		return;
//	frame.setBackground(captureTrack.isCapture() ? Pastels.RED : null);
//}
//
//public void transpose() {
//	if (captureTrack == null) return;
//	captureTrack.getArp().toggle(Mode.MPK);
//	if (frame == null)
//		return;
//	frame.setBackground(captureTrack.getArp().getMode() == Mode.MPK ? Mode.MPK.getColor() : null);
//}
