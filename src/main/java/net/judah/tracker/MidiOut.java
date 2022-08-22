package net.judah.tracker;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.midi.ReRoute;

public class MidiOut extends JComboBox<JackPort>  {

	public MidiOut() {
		BasicComboBoxRenderer style = new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        		JackPort item = (JackPort) value;
        		setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
        		setText(item == null ? "?" : item.getShortName());
        		return this;
        }};
        style.setHorizontalAlignment(SwingConstants.CENTER);
        setRenderer(style);
	}
	
	public MidiOut(JackPort[] items, ReRoute in) {
		this();
		for (JackPort port : items) {
			addItem(port);
		}
		addActionListener(e -> in.patch((JackPort)getSelectedItem()));
	}
	
	public MidiOut(final Track track) {
		this();
		
		JudahMidi midi = JudahMidi.getInstance();

		if (!track.isDrums()) {
			addItem(midi.getCraveOut());
		}
		addItem(midi.getFluidOut());
		addItem(midi.getCalfOut());
		addItem(midi.getCircuitOut());
		setSelectedItem(track.getMidiOut());
		addActionListener(e -> exec(track));
	}

	private void exec(Track track) {
		if (!track.getMidiOut().equals(getSelectedItem())) {
        	JackPort old = track.getMidiOut();
        	track.setMidiOut((JackPort)getSelectedItem());
        	if (old != null)
        		new Panic(old).start();
             // TODO Beats View MainFrame.get().getTracker().getView(track).redoInstruments();
		}

	}
	
	public ArrayList<JackPort> getAvailable() {
		ArrayList<JackPort> result = new ArrayList<JackPort>();
		for (int i = 0; i < getItemCount(); i++)
			result.add(getItemAt(i));
		return result;
	}

}



