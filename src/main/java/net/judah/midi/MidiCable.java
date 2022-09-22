package net.judah.midi;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.judah.JudahZone;
import net.judah.tracker.Track;
import net.judah.util.SettableCombo;

/** Connects sequencers and midi keyboards to either a Jack midi port or an internal midi consumer */
public class MidiCable extends SettableCombo<MidiPort> {
	
	public static final BasicComboBoxRenderer STYLE = new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		setHorizontalAlignment(SwingConstants.CENTER);
        		MidiPort item = (MidiPort) value;
        		setText(item == null ? "?" : item.toString());
        		return this;
    }};
	
	public MidiCable(ReRoute in, boolean isDrums) {
		super(()->changePort(in));
		setRenderer(STYLE);
		fillItems(isDrums, this);
	}

	public MidiCable(final Track track) {
		super(()->changeTrack(track));
		setRenderer(STYLE);
		fillItems(track.isDrums(), this);
	}
	
	public static void fillItems(boolean isDrums, JComboBox<MidiPort> combo) {
		for (MidiPort p : isDrums ? JudahZone.getDrumPorts() : JudahZone.getSynthPorts()) {
			combo.addItem(p);
		}
	}

	public static void changeTrack(Track t) {
		t.setMidiOut( (MidiPort) (SettableCombo.getFocus().getSelectedItem()));
	}
	
	public static void changePort(ReRoute in) {
		in.patch((MidiPort) (SettableCombo.getFocus().getSelectedItem()));
	}
	
	public ArrayList<MidiPort> getAvailable() {
		ArrayList<MidiPort> result = new ArrayList<MidiPort>();
			for (int i = 0; i < getItemCount(); i++)
				result.add(getItemAt(i));
		    return result;
	}
    

}
