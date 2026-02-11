package net.judah.synth.taco;

import java.util.List;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.swing.ImageIcon;

import judahzone.fx.Convolution;
import judahzone.gui.Icons;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.WavConstants;
import lombok.Getter;
import net.judah.synth.Engine;

public class TacoTruck extends Engine {

	@Getter private final Vector<TacoSynth> tracks = new Vector<TacoSynth>();

	private static final float[] work = new float[Constants.bufSize()];

    public TacoTruck(String name, ImageIcon picture) {
    	super(name, Constants.MONO);
    	icon = picture;
    	getGain().setPreamp(WavConstants.TO_LINE);
    }

	public TacoTruck(String name) {
		this(name, Icons.get("Waveform.png"));
	}

	@Override public TacoSynth getTrack() {
		return tracks.getFirst();
	}

	public void addTrack(String name) throws InvalidMidiDataException {
		tracks.add(new TacoSynth(name, this, new Polyphony(this, tracks.size())));
	}

	@Override public String[] getPatches() {
		List<String> result = SynthDB.keys();
		return result.toArray(new String[result.size()]);
	}

	@Override public String progChange(int data2, int ch) {
		return tracks.get(ch).progChange(data2);
	}

	@Override public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage s && s.getChannel() < tracks.size())
			tracks.get(s.getChannel()).send(message, timeStamp);
	}

	@Override public void close() {
		tracks.clear();
	}

	@Override public void processImpl() {
		if (onMute)
			return;

		AudioTools.silence(work);
		for (TacoSynth t : tracks) { // gather tracks
			t.process();
			AudioTools.mix(t.mono, work);
		}
		if (effects.contains(IR))
			((Convolution.Mono)IR).process(work);
		gain.monoToStereo(work, left, right);
		hotSwap(); // activate FX
		for( int i = 0, n = active.size(); i < n; i++)
			active.get(i).process(left, right);
	}


}
