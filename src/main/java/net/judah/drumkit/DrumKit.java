package net.judah.drumkit;

import static net.judah.util.Constants.midiToFloat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import net.judah.midi.Midi;
import net.judah.midi.MidiPort;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter
public class DrumKit extends LineIn implements Engine, Knobs {
	public static final int SAMPLES = 8;

	/** true if OHat shuts off when CHat plays */
	@Setter private boolean choked = true; 
	@Setter private MidiPort midiPort; // final
	private final DrumSample[] samples = new DrumSample[SAMPLES];
	private DrumPreset kit;
	private final KitMode kitMode;
	private final KnobMode knobMode = KnobMode.Kits;
	private final List<Integer> actives = new ArrayList<>();
	private final int channel = 9;
	@Override public boolean isMono() { return false; }
	
	public DrumKit(KitMode mode) {
		this(mode, "Drums.png");
	}

	public DrumKit(KitMode mode, String iconName) {
		super(mode.name(), true);
		kitMode = mode;
		setIcon(Icons.get(iconName));
		midiPort = new MidiPort(this);
		for (int i = 0; i < SAMPLES; i++)
			samples[i] = new DrumSample(DrumType.values()[i]);
	}

	public void setKit(String name) {
		for (DrumSample s : samples) {
			s.setActive(false);
			s.setTapeCounter(0);
		}
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
			}
		}
	}
	
	public void play(DrumSample s, boolean on, int velocity) {
		if (on) {
			s.setTapeCounter(0);
			s.setVelocity(midiToFloat(velocity));
			s.getEnvelope().reset();
			s.setActive(true);
			if (choked && s.getDrumType() == DrumType.CHat) {// TODO multi
				DrumSample ohat = samples[DrumType.OHat.ordinal()];
				if (ohat.isActive()) {
					ohat.setActive(false);
					MainFrame.update(ohat);
				}
			}
		}
		else {
			s.setActive(false);
		}
		MainFrame.update(s); 
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage == false) return;
		ShortMessage midi = (ShortMessage)message;
		if (false == Midi.isNoteOn(midi))
			return;
		int data1 = midi.getData1();
		
		for (DrumSample drum : samples) {
			if (drum.getGmDrum().getData1() == data1)
				play(drum, true, midi.getData2());
		}
	}

	@Override
	public boolean hasWork() {
		for (DrumSample drum : samples)
			if (drum.isActive()) return true;
		return false;
	}
	
	@Override
	public void close() {
		for (DrumSample s : samples) 
			play(s, false, 0);
	}

	@Override
	public void progChange(String preset, int channel) {
		progChange(preset);
	}

	@Override
	public void progChange(String preset) {
		try {
			setKit(preset);
		} catch (Exception e) {
			RTLogger.log(this, e.getMessage());
		}
	}

	@Override
	public void process() {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumSample drum: samples)  
			drum.process(left, right);
		
		processStereoFx(gain.getGain());
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

}
