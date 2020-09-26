package net.judah.mixer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.jack.ProcessAudio;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.mixer.gui.MixerTab;
import net.judah.sequencer.Sequencer;
import net.judah.settings.Command;
import net.judah.settings.Service;


public class Mixer implements Service {

	@Getter private final MixerCommands commands;
	@Getter private final MixerTab gui;
	
	@Getter private final List<Sample> samples = new ArrayList<>();

	public Mixer(Sequencer sequencer) throws JackException {
		commands = new MixerCommands(this);
		
		// init loops
		samples.add(new Recorder("Loop A", ProcessAudio.Type.FREE));
		samples.add(new Recorder("Loop B", ProcessAudio.Type.FREE));
		
		gui = new MixerTab(samples, sequencer.getMetronome());
		
		sequencer.getServices().add(this);
	}
	
	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		commands.execute(cmd, props); 
	}
	
	@Override
	public String getServiceName() {
		return Mixer.class.getSimpleName();
	}

	public void addSample(Sample s) {
		s.setOutputPorts(JudahZone.getOutputPorts());
		samples.add(s);
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

	
	
}

