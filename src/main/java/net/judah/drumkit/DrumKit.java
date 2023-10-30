package net.judah.drumkit;

import java.io.File;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Engine;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.Knobs;
import net.judah.gui.settable.Program;
import net.judah.midi.Actives;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.seq.track.DrumTrack;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter
public class DrumKit extends Engine implements Knobs {
	public static final int SAMPLES = 8;

	/** true if OHat shuts off when CHat plays */
	@Setter private boolean choked = true; 
	private final DrumSample[] samples = new DrumSample[SAMPLES];
	private DrumPreset kit;
	private final KitMode kitMode;
	private final KnobMode knobMode = KnobMode.Kits;
	private final DrumTrack drumtrack;
	private final Actives actives;
	
	public DrumKit(KitMode mode, JudahClock clock) throws InvalidMidiDataException {
		super(mode.name(), true);
		kitMode = mode;
		icon = Icons.get("Drums.png");
		drumtrack = new DrumTrack(this, clock);
		getTracks().add(drumtrack);
		actives = drumtrack.getActives();
		for (int i = 0; i < SAMPLES; i++)
			samples[i] = new DrumSample(DrumType.values()[i], actives);
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage == false) return;
		ShortMessage midi = Midi.copy((ShortMessage)message);
		if (false == Midi.isNoteOn(midi))
			return;
		int data1 = midi.getData1();
		
		for (DrumSample sample : samples) {
			if (sample.getDrumType().getData1() != data1) continue;
			sample.rewind();
			sample.setVelocity(Constants.midiToFloat(midi.getData2()));
			sample.getEnvelope().reset();
			sample.play(true);
			if (sample.getDrumType() == DrumType.CHat && choked) {// TODO multi
				DrumSample ohat = samples[DrumType.OHat.ordinal()];
				if (ohat.isPlaying()) {
					actives.off(ohat.getDrumType().getData1());
				}
			}

			int idx = actives.indexOf(data1);
			if (idx < 0)
				actives.add(midi);
			else
				actives.set(idx, midi);
		}
		MainFrame.update(actives);
		
	}

	private void off(DrumSample s) {
		s.play(false);
		int idx = actives.indexOf(s.getDrumType().getData1());
		if (idx >= 0) 
			actives.remove(idx);
		MainFrame.update(actives);
	}
	
	public boolean hasWork() {
		for (DrumSample drum : samples)
			if (drum.isPlaying()) return true;
		return false;
	}
	
	@Override
	public void close() {
		for (DrumSample s : samples) 
			off(s);
	}

	@Override
	public boolean progChange(String name) {
		for (DrumSample s : samples) 
			s.play(false);
		// todo custom kits
		for (File folder : Folders.getKits().listFiles()) {
			if (folder.isDirectory() == false)
				continue;
			if (folder.getName().equals(name)) {
				try {
					kit = new DrumPreset(folder);
					for (int i = 0; i < samples.length; i++)
						if (kit.get(i) != null)
							samples[i].setRecording(kit.get(i));
					MainFrame.update(Program.first(this, 9)); 
				} catch (Exception e) {
					RTLogger.warn(this, e);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean progChange(String preset, int channel) {
		return progChange(preset);
	}

	@Override
	public String[] getPatches() {
		return DrumDB.getKits().toArray(new String[DrumDB.getKits().size()]);
	}

	@Override
	public String getProg(int ch) {
		if (kit == null)
			return "?";
		return kit.getFolder().getName();
	}

	@Override
	public void process() {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumSample drum: samples)  
			drum.process(left, right);
		
		processStereoFx(gain.getGain());
	}

}
