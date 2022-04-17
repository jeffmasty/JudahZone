package net.judah.tracks;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.midi.JudahMidi;

public class MidiOut extends JComboBox<JackPort>  {

	Track track;
	
	@Getter private final ArrayList<JackPort> ports = new ArrayList<>();
	
	public MidiOut(Track t) {
		track = t;
		
		((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		
		JudahMidi midi = JudahMidi.getInstance();
		
		if (t.isDrums()) {
			ports.add(midi.getSynthOut());
			ports.add(midi.getCalfOut());
			ports.add(midi.getDrumsOut());
		}
		else {
			ports.add(midi.getSynthOut());
			ports.add(midi.getCalfOut());
			ports.add(midi.getCraveOut());
		}
        
        for (JackPort p : ports)
        	addItem(p);
        addActionListener(e -> {
            if (!track.getMidiOut().equals(getSelectedItem())) {
            	track.setMidiOut((JackPort)getSelectedItem());
            	MainFrame.get().getTracker().getView(track).redoInstruments();
            }
        });
        setRenderer(new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        		JackPort item = (JackPort) value;
        		
        		setText(item == null ? "?" : item.getShortName());
        		return this;
        }});

	}

}



