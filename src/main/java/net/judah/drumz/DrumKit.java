package net.judah.drumz;

import static net.judah.util.Constants.reverseVelocity;

import java.io.File;
import java.nio.FloatBuffer;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.api.Engine;
import net.judah.api.Midi;
import net.judah.controllers.KnobMode;
import net.judah.controllers.Knobs;
import net.judah.midi.MidiPort;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Icons;
import net.judah.util.RTLogger;

@Getter
public class DrumKit extends LineIn implements Engine, Knobs {
	public static final int TRACKS = 8;

	/** true if OHat shuts off when CHat plays */
	@Setter private boolean choked = true; 
	@Setter private MidiPort midiPort; // final
	private final DrumSample[] samples = new DrumSample[TRACKS];
	private DrumPreset kit;
	@Getter private final KnobMode knobMode;
	
	protected final FloatBuffer[] buffer = new FloatBuffer[] 
			{FloatBuffer.allocate(bufSize), FloatBuffer.allocate(bufSize)};

	private final int channel = 9;
	
	public DrumKit(KnobMode mode) {
		this(mode, "Drums.png");
	}

	public DrumKit(KnobMode mode, String iconName) {
		super(mode.name(), true);
		knobMode = mode;
		setIcon(Icons.load(iconName));
		midiPort = new MidiPort(this);
		for (int i = 0; i < TRACKS; i++)
			samples[i] = new DrumSample(DrumType.values()[i]);
	}

	public void setKit(String name) {
		for (DrumSample s : samples) {
			s.setActive(false);
			s.setTapeCounter(0);
		}
		// todo custom kits
		for (File folder : Constants.KITS.listFiles()) {
			if (folder.isDirectory() == false)
				continue;
			if (folder.getName().equals(name)) {
				try {
					kit = new DrumPreset(folder);
					for (int i = 0; i < samples.length; i++)
						if (kit.get(i) != null)
							samples[i].setRecording(kit.get(i));
					KitzView.getInstance().update(this);
					
				} catch (Exception e) {
					RTLogger.warn(this, e);
				}
			}
		}
	}
	
	public void play(DrumSample s, boolean on, int velocity) {
		if (on) {
			s.setTapeCounter(0);
			s.setVelocity(reverseVelocity(velocity));
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

	public void process(FloatBuffer[] output) {
		AudioTools.silence(buffer);
		for (DrumSample drum: samples) {
			drum.process(buffer);
			AudioTools.mix(drum.getBuffer()[0], buffer[0]);
			AudioTools.mix(drum.getBuffer()[1], buffer[1]);
		}
		processFx(buffer[0], buffer[1], gain.getGain());
	}

	@Override
	public String[] getPatches() {
		return DrumDB.getKits().toArray(new String[DrumDB.getKits().size()]);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int getProg(int ch) {
		return DrumDB.indexOf(kit);
	}
	
}
