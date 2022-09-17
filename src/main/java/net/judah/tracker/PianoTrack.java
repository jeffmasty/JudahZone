package net.judah.tracker;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.Channels;
import net.judah.util.Slider;

public class PianoTrack extends Track {

	@Setter @Getter private int gate = 2;
	@Getter private final Slider portVol = new Slider(e -> {
			Channel ch = JudahZone.getChannels().byName(Channels.volumeTarget(midiOut));
			int vol = ((Slider)e.getSource()).getValue();
			if (ch.getGain().getVol() != vol) {
				ch.getGain().setVol(vol);
				MainFrame.update(ch);
			}
		});
	
	public PianoTrack(JudahClock clock, String name, int octave, JackPort port) {
		super(clock, name, 0, port);
		edit = new PianoEdit(this, octave);
	}
	
	
	
}