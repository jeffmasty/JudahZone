package net.judah.drumkit;

import java.awt.event.MouseAdapter;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.JComboBox;

import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.util.RTLogger;

public class GMPad extends Pad {
	private final GMKitView parent;
	
	public GMPad(DrumType type, GMKitView view) { 
		super(type);
		this.parent = view;
		// setPreferredSize(new Dimension(85, 65));
		
		addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(java.awt.event.MouseEvent evt) {
				try {JudahMidi.queue(
						new Midi(Midi.NOTE_ON, 9, type.getData1(), 100), 
						parent.getCurrent().getMidiPort().getPort());

				} catch (InvalidMidiDataException e) {
					RTLogger.warn(this, e);
				}
			}
		});
			
			
		JComboBox<String> patch = new JComboBox<>();
		patch.addItem(GMDrum.ElectricSnare.getDisplay());
		bottom.add(patch); // new Knob(null)
	}
	
	
	@Override
	public void update() {
		// setBackground(s.isActive() ? Pastels.GREEN : Pastels.EGGSHELL);
	}

}
