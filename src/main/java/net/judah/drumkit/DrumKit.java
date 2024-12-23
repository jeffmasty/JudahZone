package net.judah.drumkit;

import java.io.File;
import java.nio.FloatBuffer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.Program;
import net.judah.midi.Actives;
import net.judah.midi.Midi;
import net.judah.mixer.LineIn;
import net.judah.omni.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter  
public class DrumKit extends Actives {
	public static final int SAMPLES = 8;

	private final DrumMachine machine;
	private final DrumSample[] samples = new DrumSample[SAMPLES];
	private final KitMode kitMode;
	private final KnobMode knobMode = KnobMode.KITS;
	protected final FloatBuffer left;
    protected final FloatBuffer right;
	private final KitKnobs gui;
	private final ShortMessage CHOKE = Midi.create(Midi.NOTE_OFF, DrumType.OHat.getData1(), 1);

	/** true if OHat shuts off when CHat plays */
	@Setter private boolean choked = true;
	private DrumPreset preset;
	private final LineIn fx; 
	
	public DrumKit(DrumMachine drums, KitMode mode) throws InvalidMidiDataException {
		super(drums, mode.getCh(), SAMPLES);
		
		fx = new LineIn(mode.name(), true) ;
		left = fx.getLeft();
		right = fx.getRight();
		machine = drums;
		kitMode = mode;
		for (int i = 0; i < SAMPLES; i++)
			samples[i] = new DrumSample(DrumType.values()[i], this);
		gui = new KitKnobs(this);
	}
	
	public DrumKit(DrumMachine drums, KitMode mode, String kit) throws Exception {
		this(drums, mode);
		
		for (File folder : Folders.getKits().listFiles()) {
			if (folder.isDirectory()  && folder.getName().equals(kit)) {
				this.preset = new DrumPreset(folder);
				for (int i = 0; i < samples.length; i++)
					if (this.preset.get(i) != null)
						samples[i].setRecording(this.preset.get(i));
				return;
			}
		}
		RTLogger.warn(this, mode.name() + " missing " + kit);
	}
	
	public void send(ShortMessage midi) {
		int data1 = midi.getData1();
		for (DrumSample sample : samples) {
			if (sample.getDrumType().getData1() != data1) continue;
			sample.getTapeCounter().set(0); // rewind();
			sample.setVelocity(Constants.midiToFloat(midi.getData2()));
			sample.getEnvelope().reset();
			sample.play(true);
			if (sample.getDrumType() == DrumType.CHat && choked) {// TODO multi
				if (samples[DrumType.OHat.ordinal()].isPlaying()) 
					noteOff(CHOKE);
			}

			int idx = indexOf(data1);
			if (idx < 0)
				add(midi);
			else
				set(idx, midi);
		}
		MainFrame.update(this);
		
	}

	public boolean hasWork() {
		for (DrumSample drum : samples)
			if (drum.isPlaying()) return true;
		return false;
	}
	
	public void close() {
		for (DrumSample s : samples) 
			s.off();
	}

	public boolean progChange(String name) {
		for (DrumSample s : samples) 
			s.play(false);
		// todo custom kits
		for (File folder : Folders.getKits().listFiles()) {
			if (folder.isDirectory() == false)
				continue;
			if (folder.getName().equals(name)) {
				try {
					preset = new DrumPreset(folder);
					for (int i = 0; i < samples.length; i++)
						if (preset.get(i) != null)
							samples[i].setRecording(preset.get(i));
					MainFrame.update(Program.first(machine, channel)); 
				} catch (Exception e) {
					RTLogger.warn(this, e);
				}
				return true;
			}
		}
		return false;
	}

	public void process() {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumSample drum: samples)  
			drum.process(left, right);
		fx.processStereoFx(1f);
	}

	public DrumSample getSample(DrumType type) {
		for (DrumSample s : samples)
			if (s.getDrumType() == type)
				return s;
		return null;
	}


}
