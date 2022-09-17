package net.judah.tracker;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.midi.JudahMidi;
import net.judah.midi.ReRoute;
import net.judah.util.SettableCombo;

public class MidiOut extends SettableCombo<JackPort>  {

	public static final BasicComboBoxRenderer STYLE = new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        		JackPort item = (JackPort) value;
        		setText(item == null ? "?" : item.getShortName());
        		setHorizontalAlignment(SwingConstants.CENTER);
        		return this;
        }};

	
	public MidiOut(JackPort[] items, ReRoute in) {
		super(()->change(in));
		setRenderer(STYLE);
		for (JackPort port : items) {
			addItem(port);
		}
	}
	
	public MidiOut(final Track track) {
		super(()->change(track));
		setRenderer(STYLE);
		JudahMidi midi = JudahMidi.getInstance();
		if (!track.isDrums()) {
			addItem(midi.getCraveOut());
		}
		addItem(midi.getFluidOut());
		addItem(midi.getCalfOut());
		setSelectedItem(track.getMidiOut());
	}

	
	public static void change(Track t) {
		JackPort port = (JackPort)((MidiOut)SettableCombo.getFocus()).getSelectedItem();
		t.setMidiOut(port);
	}
	
	public static void change(ReRoute in) {
		JackPort port = (JackPort)((MidiOut)SettableCombo.getFocus()).getSelectedItem();
		in.patch(port);
	}
	
	public ArrayList<JackPort> getAvailable() {
		ArrayList<JackPort> result = new ArrayList<JackPort>();
		for (int i = 0; i < getItemCount(); i++)
			result.add(getItemAt(i));
		return result;
	}

}



