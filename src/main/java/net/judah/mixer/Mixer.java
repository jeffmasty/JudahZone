package net.judah.mixer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Service;
import net.judah.jack.ProcessAudio;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.mixer.gui.MixerGui;
import net.judah.sequencer.Sequencer;
import net.judah.util.JudahException;


public class Mixer implements Service {

	@Getter private final MixerCommands commands;
	@Getter private final MixerGui gui;
	
	@Getter private final List<Sample> samples = new ArrayList<>();

	public Mixer(Sequencer sequencer) throws JackException {
		commands = new MixerCommands(this);
		
		// init loops  
		samples.add(new Recorder("Loop A", ProcessAudio.Type.FREE));
		samples.add(new Recorder("Loop B", ProcessAudio.Type.FREE));
		
		gui = new MixerGui(samples, JudahZone.getMetronome());
		
		sequencer.getServices().add(this);
	}
	
	@Override
	public String getServiceName() {
		return Mixer.class.getSimpleName();
	}

	/**@return the sample's index number */
	public int addSample(Sample s) {
		s.setOutputPorts(JudahZone.getOutputPorts());
		samples.add(s);
		return samples.size() - 1;
	}
	
	public void removeSample(int idx) {
		samples.remove(idx);
	}
	
	public void removeSample(Sample sample) {
		samples.remove(sample);
	}
	
	public void stopAll() {
		for (Sample s : samples) {
			s.play(false);
			if (s instanceof Recorder) 
				((Recorder) s).record(false);
		}
	}

	@Override
	public void close() {
		stopAll();
		for(Sample s : samples) 
			s.clear();
	}

	/** in Real-Time thread */
	public void process(int nframes) {
		// do any recording or playing
		for (Sample sample : samples) {
			sample.process(nframes);
		}
	}

	/**@return gain level of each sample before muting */
	public  ArrayList<Float> muteAll() {
		ArrayList<Float> result = new ArrayList<Float>();
		for (Sample sample : samples) {
			result.add(sample.getGain());
			sample.setGain(0f);
		}
		return result;
	}

	public void restoreState(ArrayList<Float> mixerState) throws JudahException {
		if (mixerState.size() != samples.size()) 
			throw new JudahException(mixerState.size() + " vs. " + samples.size());
		for (int i = 0; i < samples.size(); i++) {
			samples.get(i).setGain(mixerState.get(i));
		}
	}

	@Override
	public void properties(HashMap<String, Object> props) {
		// TODO Auto-generated method stub
		
	}

	
}

