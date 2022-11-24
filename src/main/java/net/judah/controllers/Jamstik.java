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
import net.judah.midi.MidiPort;
import net.judah.midi.Panic;
import net.judah.mixer.LineIn;
import net.judah.util.Pastels;

/** Controller substitute, reroute guitar midi to synths */
public class Jamstik extends JComboBox<MidiPort>{
	
	@Getter private static boolean active = false;
	@Getter private static MidiPort out;
	private final ArrayList<MidiPort> ports;
	
	private int volStash = 50;
	@Setter private JPanel frame;
	
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
	
//	private void run() {
//		if (path == null) 
//			return;
//		try {
//			Jack jack = Jack.getInstance();
//			JudahMidi midi = JudahZone.getMidi();
//			JackClient client = midi.getJackclient();
//			
//			String jamstik = jack.getPorts(client, Constants.getDi(), 
//					JackPortType.MIDI, EnumSet.of(JackPortFlags.JackPortIsOutput))[0];				
//
//			String search = IN.JAMSTIK.getPort();
//			if (midi.getCraveOut() == path.getPort().getPort())
//				search = Constants.getDi();
//			else if (midi.getFluidOut() == path.getPort().getPort())
//				search = FluidSynth.MIDI_PORT;
//			
//			
//			String port = jack.getPorts(client, search, 
//					JackPortType.MIDI, EnumSet.of(JackPortFlags.JackPortIsInput))[0];
//
//			Gain guitar = JudahZone.getGuitar().getGain();
//			if (active) {
//				volStash = guitar.getVol();
//				guitar.setVol(0);
//				MainFrame.update(path.getChannel());
//				jack.connect(client, jamstik, port);
//			} else {
//				try {
//					jack.disconnect(client, jamstik, port);
//				} catch (JackException e) {
//					RTLogger.log(Jamstik.class, "Jamstik disconnect: " + e.getMessage() );
//				}
//				new Panic(path.getPort()).start();
//				guitar.setVol(volStash);
//			}
//			if (frame != null)
//				frame.setBackground(active ? Pastels.GREEN : Pastels.BUTTONS);		
//			MainFrame.update(guitar); 
//		} catch (Throwable e) {
//			RTLogger.warn(Jamstik.class.getSimpleName(), e);
//		}
//	}
	
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
			if (getFxRack().getCurrent() != null && getFxRack().getCurrent().getChannel() == getGuitar())
				getFxRack().getCurrent().update();
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
