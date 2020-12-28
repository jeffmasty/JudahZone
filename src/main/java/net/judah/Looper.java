package net.judah;

import java.util.ArrayList;
import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import lombok.RequiredArgsConstructor;
import net.judah.jack.ProcessAudio;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.util.JudahException;

/** use {@link #addSample(Sample)} instead of add() */
@RequiredArgsConstructor
public class Looper extends ArrayList<Sample> {
	
	private final List<JackPort> outports;

    @Override
	public boolean add(Sample s) {
		s.setOutputPorts(outports);
		return super.add(s);
    }
	
	public void stopAll() {
		for (Sample s : this) {
			s.play(false);
			if (s instanceof Recorder) 
				((Recorder) s).record(false);
		}
	}

	public void init() {
		clear();  
		add(new Recorder("Loop A", ProcessAudio.Type.FREE));
		add(new Recorder("Loop B", ProcessAudio.Type.FREE));
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

	
}