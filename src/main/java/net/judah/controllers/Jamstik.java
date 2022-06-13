package net.judah.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.jaudiolibs.jnajack.*;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Command;
import net.judah.api.Service;
import net.judah.effects.api.Gain;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.mixer.Channel;
import net.judah.settings.Channels;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

/** static Controller substitute */
public class Jamstik implements Service {
	
	@Getter private static boolean active = false;
	@Getter private static JackPort midiOut = JudahMidi.getInstance().getUnoOut();
	private static Channel channel = JudahZone.getChannels().getUno();
	private static String JAMSTIK = "Jamstik-->";
	@Getter private static final JLabel widget = new JLabel(JAMSTIK + channel.getName());
	private static int volStash = 50;
	
	static {
		widget.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e))
					toggle();
				else 
					nextMidiOut(); 
		} });
		widget.setOpaque(true);
		updateGui();
	}
		
	private static void activate() {
		if (midiOut == null) 
			return;
	
		try {
			Jack jack = Jack.getInstance();
			JudahMidi midi = JudahMidi.getInstance();
			JackClient client = midi.getJackclient();
			String search = "UNO Synth Pro MIDI 1";
			String jamstik = jack.getPorts(client, "UMC1820 MIDI 1", 
					JackPortType.MIDI, EnumSet.of(JackPortFlags.JackPortIsOutput))[0];				
			if (midiOut.equals(midi.getCraveOut()))
				search = "UMC1820 MIDI 1";
			else if (midiOut.equals(midi.getFluidOut()))
				search = FluidSynth.MIDI_PORT;
			String port = jack.getPorts(client, search, 
					JackPortType.MIDI, EnumSet.of(JackPortFlags.JackPortIsInput))[0];
			Gain guitar = JudahZone.getChannels().getGuitar().getGain();
			if (active) {
				volStash = guitar.getVol();
				guitar.setVol(0);
				jack.connect(client, jamstik, port);
				if (channel != null) 
					MainFrame.setFocus(channel);
				MainFrame.update(JudahZone.getChannels().getGuitar());
			} else {
				jack.disconnect(client, jamstik, port);
				new Panic(midiOut).start();
				guitar.setVol(volStash);
				MainFrame.setFocus(JudahZone.getChannels().getGuitar());
			}
			updateGui();
		} catch (JackException e) {
			RTLogger.warn(Jamstik.class, e);
		}
		
	}
	
	public static void toggle() {
		active = !active;
		new Thread(()-> activate()).run();
	}
	
	public static void setMidiOut(JackPort out, Channel ch) {
		if (midiOut == out)
			return;
		if (active && midiOut != null)
			new Panic(midiOut).start();
		if (active) {
			new Thread(()->{
				active = false;
				activate();
				channel = ch;
				midiOut = out;
				active = true;
				activate();
			} ).start();
		}
		else {
			channel = ch;
			midiOut = out;
			new Thread(()->updateGui()).start();
		}
	}
	
	public static void nextMidiOut() {
		JackPort port = midiOut;
		JudahMidi midi = JudahMidi.getInstance();
		Channels ch = JudahZone.getChannels();
		if (port == null || port == midi.getFluidOut()) {
			setMidiOut(midi.getUnoOut(), ch.getUno());
		}
		else if (port == midi.getCraveOut()) {
			setMidiOut(midi.getFluidOut(), ch.getFluid());
		}
		else {
			setMidiOut(midi.getCraveOut(), ch.getCircuit()); // TODO
		}
	}

	private static void updateGui() {
		widget.setText(JAMSTIK + channel.getName());
		widget.setBackground(active ? Pastels.GREEN : Pastels.EGGSHELL);
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
		activate();
	}

	@Override
	public void properties(HashMap<String, Object> props) {
	}

}
