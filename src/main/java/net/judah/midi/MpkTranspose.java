package net.judah.midi;

import javax.sound.midi.ShortMessage;
import javax.swing.JButton;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.seq.CUE;
import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTrack;

public class MpkTranspose extends JButton implements TimeListener {

	private final MidiTrack track;
	@Getter private int amount;
	@Setter @Getter private static Integer onDeck;
	@Getter private boolean active;
	
	public MpkTranspose(MidiTrack track) {
		super(" 🔁 ");
		this.track = track;
		addActionListener(e->toggle());
		setOpaque(true);
	}
	
	public void setActive(boolean active) {
		
		this.active = active;
		if (active) {
			track.getClock().addListener(this);
			JudahZone.getMidi().setKeyboardSynth(track);
		}
		else
			track.getClock().removeListener(this);
		JudahZone.getMidiGui().transpose(active);
	}
	
	public void toggle() {
		setActive(!active);
		MainFrame.update(track);
	}

	public ShortMessage apply(ShortMessage midi)  {
		if (!active)
			return midi;
		return Midi.create(midi.getCommand(), midi.getChannel(), midi.getData1() + amount, midi.getData2());
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			if (onDeck != null) { // shuffle to next transposition at bar downbeat
				amount = onDeck;
				onDeck = null;
			}
		}
	}

	public void update() {
		setBackground(active ? Pastels.PINK : null);
	}

	public boolean setAmount(Midi midi) {
		if (!active) return false;
		if (track.getCue() == CUE.Hot)
			amount = midi.getData1() - MidiConstants.MIDDLE_C;
		else
			onDeck = midi.getData1() - MidiConstants.MIDDLE_C;
		return true;
	}
	
}
