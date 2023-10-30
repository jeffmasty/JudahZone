package net.judah.gui.widgets;

import java.awt.Component;
import java.util.List;

import javax.sound.midi.ShortMessage;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.Updateable;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;

public abstract class MidiPatch extends JComboBox<PianoTrack> implements Updateable {

	@Getter protected PianoTrack track;
	protected JPanel frame;
	
	static final BasicComboBoxRenderer STYLE = new BasicComboBoxRenderer() {
		@Override public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, 
				Object value, int index, boolean isSelected, boolean cellHasFocus) {
			setHorizontalAlignment(SwingConstants.CENTER);
			MidiTrack item = (MidiTrack) value;
			setText(item == null ? "?" : item.toString());
			return this;
		}
	};
	
	public void install(JPanel frame, List<MidiTrack> ports) {
		setOpaque(true);
		setRenderer(STYLE);
		ports.forEach(track -> addItem((PianoTrack)track));
		track = (PianoTrack)ports.get(0);
		setSelectedItem(track);
		addActionListener(e -> setPort((PianoTrack)getSelectedItem()));

		this.frame = frame;
		frame.add(Gui.resize(this, Size.COMBO_SIZE));
	}

	public void setPort(PianoTrack out) {
		if (track == out)
			return;
		if (track != null)
			new Panic(track);
		track = out;
		MainFrame.update(this);
	}

	@Override public void update() {
		if (track != (MidiTrack)getSelectedItem())
			setSelectedItem(track);
	}
	
	public void send(ShortMessage m, int ticker) {
		if (m.getChannel() == track.getCh())
			track.getMidiOut().send(m, ticker);
		else
			track.getMidiOut().send(Midi.format(m, track.getCh(), 1f), ticker);
	}
	
}
