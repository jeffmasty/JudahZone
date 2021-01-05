package net.judah;

import java.util.ArrayList;
import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.jack.ProcessAudio;
import net.judah.jack.ProcessAudio.Type;
import net.judah.looper.DrumTrack;
import net.judah.looper.Recorder;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.util.Console;
import net.judah.util.JudahException;

/** use {@link #addSample(Sample)} instead of add() */
@RequiredArgsConstructor
public class Looper extends ArrayList<Sample> {
	
	private final List<JackPort> outports;
	@Getter private DrumTrack drumtrack; 
	
    @Override
	public boolean add(Sample s) {
		s.setOutputPorts(outports);
		return super.add(s);
    }
	
	public void stopAll() {
		for (Sample s : this) {
			if (s instanceof Recorder) 
				((Recorder) s).record(false);
			s.play(false);
		}
	}

	public void init() {
		for (Sample s : this)
			if (Type.SOLO == s.getType()) { // recover from drumTrack muting
				Channel drums = JudahZone.getChannels().getDrums();
				drums.setOnMute(false);
				drums.setMuteRecord(false);
				drums.getGui().update();
			}

		clear();  
		Recorder loop = new Recorder("loop A", ProcessAudio.Type.FREE);
		loop.play(true); // armed;
		add(loop);
		loop = new Recorder("loop B", ProcessAudio.Type.FREE);
		loop.play(true);
		add(loop);
	}
	
	/**@return gain level of each sample before muting */
	public  ArrayList<Float> muteAll() {
		ArrayList<Float> result = new ArrayList<Float>();
		for (Sample sample : this) {
			result.add(sample.getGain());
			sample.setGain(0f);
		}
		return result;
	}

	public void restoreState(ArrayList<Float> mixerState) throws JudahException {
		if (mixerState.size() != size()) 
			throw new JudahException(mixerState.size() + " vs. " + size());
		for (int i = 0; i < size(); i++) {
			get(i).setGain(mixerState.get(i));
		}
	}
	
	/** in Real-Time thread */
	public void process(int nframes) {
		// do any recording or playing
		for (Sample sample : this) {
			sample.process(nframes);
		}
	}

	public void slave() {
		if (get(0).getRecording() == null || get(0).getRecording().isEmpty()) {
			Console.info("nothing to slave"); return; }
		((Recorder)get(0)).record(false);
		get(1).setRecording(new Recording(get(0).getRecording().size(), true));
		Console.info("Slave b. buffers: " + get(1).getRecording().size());
		get(1).play(true);
	}

	public void drumtrack() {

		for (Sample s : this) 
			if (s.getName().equals(DrumTrack.NAME)) {
				s.clear();
				MixerPane.getInstance().removeSample(drumtrack);
				drumtrack = null;
				Console.info("drumtrack off");
				return;

			}
		
		drumtrack = new DrumTrack(get(0), JudahZone.getChannels().getDrums());				
		add(drumtrack);
		MixerPane.getInstance().addSample(drumtrack);
		
		Console.info("drumtrack");
	}
	
}