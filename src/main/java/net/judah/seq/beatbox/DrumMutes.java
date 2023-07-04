package net.judah.seq.beatbox;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIDefaults;

import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.gui.Gui;
import net.judah.seq.MidiView;
import net.judah.seq.track.MidiTrack;

public class DrumMutes extends JPanel {
	static UIDefaults def = new UIDefaults();
	
	private final MidiTrack track;
	private final MidiView view;
	private final Rectangle r;
	private final int rowHeight;
	
	private class Mute extends JToggleButton {
		private final DrumType type;
		Mute(DrumType type, int y) {
			super(type.name());
			this.type = type;
			setFont(Gui.FONT11);
			addActionListener(evt -> toggleMute(this, evt));
			setBounds(0, y * rowHeight, r.width, rowHeight);
		}
	}
	
	public DrumMutes(Rectangle r, MidiView view) {
		this.r = r;
		this.view = view;
		this.track = view.getTrack();
		setBounds(r);
		setLayout(null);
		rowHeight = (int)Math.ceil((r.height) / DrumType.values().length);
		for (int y = 0; y < DrumType.values().length; y++) {
			add(new Mute(DrumType.values()[y], y));
		}
		
	}
	
	public void update() {
		for (int i = 0; i < getComponentCount(); i++) {
			Component c = getComponent(i);
			if (false == c instanceof Mute)
				continue;
			Mute btn = (Mute)c;
			btn.setSelected(getSample(btn.type).isOnMute());
		}
	}
	
	public void toggleMute(Mute btn, ActionEvent evt) {
		if ((evt.getModifiers() & ActionEvent.CTRL_MASK) ==ActionEvent.CTRL_MASK) {
			view.getGrid().selectArea(track.getLeft(), track.getRight() + track.getBarTicks(), btn.type.getData1(), btn.type.getData1());
			btn.setSelected(!btn.isSelected());
		}
		else {
			DrumSample s = getSample(btn.type);
			s.setOnMute(!s.isOnMute());
		}
	}

	public DrumSample getSample(DrumType t) {
		return ((DrumKit) track.getMidiOut()).getSamples()[t.ordinal()];
	}

	public void update(DrumSample pad) {
		for (DrumSample s : ((DrumKit)track.getMidiOut()).getSamples()) 
			if (s == pad)
				getComponent(s.getDrumType().ordinal()).setBackground(pad.isOnMute() ? Color.GRAY : null);
	}
	

}
