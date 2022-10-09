package net.judah.controllers;

import java.awt.Component;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.effects.api.Gain;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.midi.Path;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

/** Controller substitute, reroute guitar midi to synths */
public class Jamstik extends JComboBox<Path> implements Closeable {
	
	@Getter private boolean active = false;
	@Getter private static Path path;
	private int volStash = 50;
	@Setter private JPanel frame;
	
	public Jamstik(ArrayList<Closeable> services, ArrayList<Path> paths) {
		services.add(0, this);
		
		BasicComboBoxRenderer style = new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        		Path item = (Path) value;
        		setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
        		setText(item == null ? "?" : item.getChannel().getName());
        		return this;
        }};
        style.setHorizontalAlignment(SwingConstants.CENTER);
        setRenderer(style);
        setOpaque(true);
        for (Path p : paths)
        	addItem(p);
        setMidiOut(paths.get(0));

        addActionListener(e -> setMidiOut((Path)getSelectedItem()));
	}
	
	private void run() {
		if (path == null) 
			return;
		try {
			Jack jack = Jack.getInstance();
			JudahMidi midi = JudahZone.getMidi();
			JackClient client = midi.getJackclient();
			
			String jamstik = jack.getPorts(client, Constants.getDi(), 
					JackPortType.MIDI, EnumSet.of(JackPortFlags.JackPortIsOutput))[0];				

			String search = "JudahSynth";
			if (midi.getCraveOut() == path.getPort().getPort())
				search = Constants.getDi();
			else if (midi.getFluidOut() == path.getPort().getPort())
				search = FluidSynth.MIDI_PORT;
			
			
			String port = jack.getPorts(client, search, 
					JackPortType.MIDI, EnumSet.of(JackPortFlags.JackPortIsInput))[0];

			Gain guitar = JudahZone.getGuitar().getGain();
			if (active) {
				volStash = guitar.getVol();
				guitar.setVol(0);
				MainFrame.update(path.getChannel());
				jack.connect(client, jamstik, port);
			} else {
				try {
					jack.disconnect(client, jamstik, port);
				} catch (JackException e) {
					RTLogger.log(Jamstik.class, "Jamstik disconnect: " + e.getMessage() );
				}
				new Panic(path.getPort()).start();
				guitar.setVol(volStash);
			}
			if (frame != null)
				frame.setBackground(active ? Pastels.GREEN : Pastels.BUTTONS);		
			MainFrame.update(guitar); 
		} catch (Throwable e) {
			RTLogger.log(Jamstik.class.getSimpleName(), e.getMessage());
		}
	}
	
	public void setActive(boolean active) {
		this.active = active;
		new Thread(()-> run()).start();
	}
	
	public void toggle() {
		active = !active;
		new Thread(()-> run()).start();
	}
	
	public void setMidiOut(Path path) {
		if (Jamstik.path == path)
			return;
		if (active) {
			new Panic(path.getPort()).start();
			new Thread(()->{
				active = false;
				run();
				Jamstik.path = path;
				active = true;
				run();
			} ).start();
		}
		else {
			Jamstik.path = path;
		}
	}

	public void nextMidiOut() {
		JudahMidi midi = JudahZone.getMidi();
		List<Path> paths = midi.getPaths();
		int idx = paths.indexOf(path) + 1;
		if (idx == paths.size()) 
			idx = 0;
		setMidiOut(paths.get(idx));
	}

	@Override
	public void close() {
		if (!active) 
			return;
		active = false;
		run();
	}

}
