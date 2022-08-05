package net.judah.controllers;

import java.awt.Component;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Command;
import net.judah.api.Service;
import net.judah.effects.api.Gain;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.midi.Path;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

/** Controller substitute, reroute guitar midi to synths */
public class Jamstik extends JComboBox<Path> implements Service {
	
	// private static Jamstik instance;
	@Getter private static boolean active = false;
	@Getter private static Path path;
	private static int volStash = 50;
	private static JPanel frame;
	
	public Jamstik(JPanel parent, ArrayList<Service> services) {
		services.add(0, this);
		Jamstik.frame= parent;
		
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
        for (Path p : JudahMidi.getInstance().getPaths())
        	addItem(p);
        addActionListener(e -> setMidiOut((Path)getSelectedItem()));
	}
	
	private static void run() {
		if (path == null) 
			return;
		// String search = "UNO Synth Pro MIDI 1";
		try {
			Jack jack = Jack.getInstance();
			JudahMidi midi = JudahMidi.getInstance();
			JackClient client = midi.getJackclient();
			
			String jamstik = jack.getPorts(client, Constants.getDi(), 
					JackPortType.MIDI, EnumSet.of(JackPortFlags.JackPortIsOutput))[0];				

			String search = "Circuit Tracks";
			if (path.getPort().equals(midi.getCraveOut()))
				search = Constants.getDi();
			else if (path.getPort().equals(midi.getFluidOut()))
				search = FluidSynth.MIDI_PORT;
			else if (path.getPort().equals(midi.getCalfOut()))
				search = "Calf Fluidsynth";
//			else if (path.getPort().equals(midi.getUnoOut()))
//				search = "UNO Synth";
			
			String port = jack.getPorts(client, search, 
					JackPortType.MIDI, EnumSet.of(JackPortFlags.JackPortIsInput))[0];
			Gain guitar = JudahZone.getChannels().getGuitar().getGain();
			if (active) {
				volStash = guitar.getVol();
				guitar.setVol(0);
				jack.connect(client, jamstik, port);
				MainFrame.update(path.getChannel());
				MainFrame.update(JudahZone.getChannels().getGuitar());
			} else {
				try {
					jack.disconnect(client, jamstik, port);
				} catch (JackException e) {
					RTLogger.log(Jamstik.class, "Jamstik disconnect: " + e.getMessage() );
				}
				new Panic(path.getPort()).start();
				guitar.setVol(volStash);
				MainFrame.update(JudahZone.getChannels().getGuitar()); // setFocus?
			}
			if (frame != null)
				frame.setBackground(active ? Pastels.GREEN : Pastels.BUTTONS);		
		} catch (Throwable e) {
			RTLogger.warn(Jamstik.class, e);
		}
		
	}
	
	public static void toggle() {
		active = !active;
		new Thread(()-> run()).start();
		if (frame != null) 
			frame.setBackground(active ? Pastels.GREEN : Pastels.BUTTONS);
	}
	
	public static void setMidiOut(Path path) {
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

	public static void nextMidiOut() {
		JudahMidi midi = JudahMidi.getInstance();
		List<Path> paths = midi.getPaths();
		int idx = paths.indexOf(path) + 1;
		if (idx == paths.size()) 
			idx = 0;
		setMidiOut(paths.get(idx));
	}


	@Override
	public List<Command> getCommands() {
		return null;
	}

	@Override
	public void close() {
		if (!active) 
			return;
		active = false;
		run();
	}

	@Override
	public void properties(HashMap<String, Object> props) {
	}

}
