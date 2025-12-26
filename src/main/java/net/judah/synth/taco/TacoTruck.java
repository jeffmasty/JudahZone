package net.judah.synth.taco;

import java.util.List;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.swing.ImageIcon;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Engine;
import net.judah.fx.Convolution;
import net.judah.omni.AudioTools;
import net.judah.util.Constants;

public class TacoTruck extends Engine {

	@Getter private final Vector<TacoSynth> tracks = new Vector<TacoSynth>();


    public TacoTruck(String name, ImageIcon picture) {
    	super(name, Constants.MONO);
    	icon = picture;
    	gain.setPreamp(0.65f);
    }

	@Override public TacoSynth getTrack() {
		return tracks.getFirst();
	}

	public void addTrack(String name) throws InvalidMidiDataException {
		tracks.add(new TacoSynth(name, this, new Polyphony(this, tracks.size())));
	}

	@Override public String[] getPatches() {
		List<String> result = JudahZone.getSynthPresets().keys();
		return result.toArray(new String[result.size()]);
	}

	@Override public String progChange(int data2, int ch) {
		return tracks.get(ch).progChange(data2);
	}

	@Override public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage s && s.getChannel() < size())
			tracks.get(s.getChannel()).send(message, timeStamp);
	}

	@Override public void close() {
		tracks.clear();
	}

	@Override public void process() {
		if (onMute)
			return;
		AudioTools.silence(left);
		for (TacoSynth t : tracks) { // gather tracks
			t.process();
			AudioTools.mix(t.mono, left);
		}
		((Convolution.Mono)IR).monoToStereo(left, right);
		// AudioTools.copy(left, right); // make stereo
		fx();
	}


}
