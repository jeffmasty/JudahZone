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
import net.judah.api.MidiReceiver;
import net.judah.fx.Gain;
import net.judah.gui.Pastels;
import net.judah.midi.Panic;
import net.judah.mixer.LineIn;

/** Controller substitute, reroute guitar midi to synths */
public class Jamstik extends JComboBox<MidiReceiver>{
	
	@Getter private static boolean active = false;
	@Getter private static MidiReceiver out;
	@Setter private JPanel frame;
	private final ArrayList<MidiReceiver> ports;
	private int volStash = 50;
	
	public Jamstik(ArrayList<Closeable> services, ArrayList<MidiReceiver> ports) {
		this.ports = ports;
		BasicComboBoxRenderer style = new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        		MidiReceiver item = (MidiReceiver) value;
        		setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
        		setText(item == null ? "?" : item.toString());
        		return this;
        }};
        style.setHorizontalAlignment(SwingConstants.CENTER);
        setRenderer(style);
        setOpaque(true);
        ports.forEach((rec)->addItem(rec));
        out = getItemAt(0);
        setSelectedItem(out);

        addActionListener(e -> setMidiOut((MidiReceiver)getSelectedItem()));
	}
	
	public void setActive(boolean active) {
		Jamstik.active = active;
		LineIn guitar = getGuitar();
		if (active) {
			volStash = guitar.getVolume();
			guitar.getGain().setGain(0);
		} else {
			guitar.getGain().set(Gain.VOLUME, volStash);
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
	
	public void setMidiOut(MidiReceiver port) {
		if (out == port)
			return;
		if (active) {
			new Panic(out.getMidiPort()).start();
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
