package net.judah.controllers;

import static net.judah.JudahZone.*;

import java.awt.Component;
import java.io.Closeable;
import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.Pastels;
import net.judah.midi.MidiPort;
import net.judah.midi.Panic;
import net.judah.mixer.LineIn;

/** Controller substitute, reroute guitar midi to synths */
public class Jamstik extends JComboBox<MidiPort>{
	
	@Getter private static boolean active = false;
	@Getter private static MidiPort out;
	@Setter private JPanel frame;
	private final ArrayList<MidiPort> ports;
	private int volStash = 50;
	
	public Jamstik(ArrayList<Closeable> services, ArrayList<MidiPort> ports) {
		this.ports = ports;
		BasicComboBoxRenderer style = new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        		MidiPort item = (MidiPort) value;
        		setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
        		setText(item == null ? "?" : item.toString());
        		return this;
        }};
        style.setHorizontalAlignment(SwingConstants.CENTER);
        setRenderer(style);
        setOpaque(true);
        for (MidiPort p : ports)
        	addItem(p);
        out = ports.get(0);
        setSelectedItem(out);

        addActionListener(e -> setMidiOut((MidiPort)getSelectedItem()));
	}
	
	public void setActive(boolean active) {
		Jamstik.active = active;
		LineIn guitar = getGuitar();
		if (active) {
			volStash = guitar.getGain().getVol();
			guitar.getGain().setVol(0);
		} else {
			guitar.getGain().setVol(volStash);
		}
		new Thread(()->{
			if (frame != null)
				frame.setBackground(active ? Pastels.GREEN : Pastels.BUTTONS);		
			getMixer().update(guitar);
			if (getFxRack().getChannel() == getGuitar())
				getFxRack().getChannel().getGui().update();
		}).start();

	}
	
	public void toggle() {
		setActive(!active);
	}
	
	public void setMidiOut(MidiPort port) {
		if (out == port)
			return;
		if (active) {
			new Panic(out).start();
		}
		out = port;
	}

	public void nextMidiOut() {
		int idx = ports.indexOf(out) + 1;
		if (idx == ports.size()) 
			idx = 0;
		setMidiOut(ports.get(idx));
	}

}
