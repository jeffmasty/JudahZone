package net.judah;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.jack.ProcessAudio;
import net.judah.looper.Recorder;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.DrumTrack;
import net.judah.util.Console;
import net.judah.util.Icons;
import net.judah.util.JudahException;

/** use {@link #addSample(Sample)} instead of add() */
@RequiredArgsConstructor
public class Looper implements Iterable<Sample> {
	
	private final ArrayList<Sample> loops = new ArrayList<>();
	
	private final List<JackPort> outports;
	@Getter private DrumTrack drumTrack; 
	private LooperGui gui;

	public void add(Sample s) {
		s.setOutputPorts(outports);
		if (gui != null)
			gui.addSample(s);
		synchronized (this) {			
			loops.add(s);
		}
    }
    
	public boolean remove(Object o) {
    	if (o instanceof Sample == false) return false;
    	if (gui != null)
    		gui.removeSample((Sample)o);
    	synchronized (this) {
    		return loops.remove(o);
    	}
	}
    
    public void clear() {
    	drumTrack.clear();
    	
    	
    	
    	if (gui != null)
    		for (Sample s : loops)
    			gui.remove(s.getGui());
    	synchronized (this) {
    		loops.clear();
    	}
	}
    
	public void stopAll() {
		drumTrack.record(false);
		drumTrack.play(false);
		for (Sample s : loops) {
			if (s instanceof Recorder) 
				((Recorder) s).record(false);
			s.play(false);
		}
	}

	public void init() {
		defaultLoops();
		drumTrack = new DrumTrack(loops.get(0), 
				JudahZone.getChannels().getDrums());
		drumTrack.setIcon(Icons.load("Drums.png"));
		drumTrack.toggle();
	}

	public void defaultLoops() {
		// TODO...
		Recorder loopA = new Recorder("A", ProcessAudio.Type.FREE);
		loopA.play(true); // armed;
		add(loopA);
		Recorder loopB = new Recorder("B", ProcessAudio.Type.FREE);
		loopB.play(true);
		add(loopB);
		
	}
	
	/**@return gain level of each sample before muting */
	public  ArrayList<Float> muteAll() {
		ArrayList<Float> result = new ArrayList<Float>();
		result.add(drumTrack.getGain());
		drumTrack.setGain(0f);
		for (Sample sample : loops) {
			result.add(sample.getGain());
			sample.setGain(0f);
		}
		return result;
	}

	public void restoreState(ArrayList<Float> mixerState) throws JudahException {
		
		if (mixerState.size() -1 != loops.size()) 
			throw new JudahException(mixerState.size() + " vs. " + loops.size() + 1);
		drumTrack.setGain(mixerState.get(0));
		for (int i = 0; i < loops.size(); i++) {
			loops.get(i).setGain(mixerState.get(i + 1));
		}
	}
	
	/** in Real-Time thread */
	public void process() {
		// do any recording or playing
		for (Sample sample : loops) {
			sample.process();
		}
		drumTrack.process();
	}

	public void slave() {
		if (loops.get(0).getRecording() == null || loops.get(0).getRecording().isEmpty()) {
			Console.info("nothing to slave"); return; }
		((Recorder)loops.get(0)).record(false);
		loops.get(1).setRecording(new Recording(loops.get(0).getRecording().size(), true));
		Console.info("Slave b. buffers: " + loops.get(1).getRecording().size());
		loops.get(1).play(true);
	}

	public void registerListener(LooperGui looperGui) {
		gui = looperGui;
	}

	@Override
	public Iterator<Sample> iterator() {
		return loops.iterator();
	}
	
	public Sample get(int i) {
		return loops.get(i);
	}
	
	public int size() {
		return loops.size();
	}
	
	public Sample[] toArray() {
		return (loops.toArray(new Sample[loops.size()]));
	}

	public int indexOf(Channel loop) {
		return loops.indexOf(loop);
	}
	
}