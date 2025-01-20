package net.judah.drumkit;

import java.io.File;
import java.nio.FloatBuffer;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.Program;
import net.judah.midi.Actives;
import net.judah.midi.Midi;
import net.judah.mixer.LineIn;
import net.judah.omni.AudioTools;
import net.judah.seq.Trax;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter
public class DrumKit extends LineIn implements Receiver {
	public static final int SAMPLES = DrumType.values().length;
	private final KnobMode knobMode = KnobMode.Kits;
	private final Actives actives;
	private final Trax type;
	private final DrumSample[] samples = new DrumSample[SAMPLES];
	private final KitKnobs knobs;
	private final ShortMessage CHOKE = Midi.create(Midi.NOTE_OFF, DrumType.OHat.getData1(), 1);

	/** true if OHat shuts off when CHat plays */
	@Setter private boolean choked = true;
	private DrumPreset program;

	public DrumKit(ZoneMidi engine, Trax type) throws Exception {

		super(type.name(), Constants.STEREO);
		this.type = type;
		actives = new Actives(engine, type.getCh(), SAMPLES);
		for (int i = 0; i < SAMPLES; i++)
			samples[i] = new DrumSample(DrumType.values()[i], actives);
		String progChange = type.getProgram();
		for (File folder : Folders.getKits().listFiles()) {
			if (folder.isDirectory()  && folder.getName().equals(progChange)) {
				this.program = new DrumPreset(folder);
				for (int i = 0; i < samples.length; i++)
					if (this.program.get(i) != null)
						samples[i].setRecording(this.program.get(i));
				break;
			}
		}
		knobs = new KitKnobs(this);
	}

	@Override
	public void send(MidiMessage msg, long timeStamp) {
		ShortMessage midi = (ShortMessage)msg;
		int data1 = midi.getData1();
		for (DrumSample sample : samples) {
			if (sample.getDrumType().getData1() != data1) continue;
			sample.getTapeCounter().set(0); // rewind();
			sample.setVelocity(Constants.midiToFloat(midi.getData2()));
			sample.getEnvelope().reset();
			sample.play(true);
			if (sample.getDrumType() == DrumType.CHat && choked) {// TODO multi

				if (actives.indexOf(CHOKE.getData1()) >= 0 || samples[DrumType.OHat.ordinal()].isPlaying()) {
					samples[DrumType.OHat.ordinal()].off();
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

	public boolean hasWork() {
		for (DrumSample drum : samples)
			if (drum.isPlaying()) return true;
		return false;
	}

	@Override
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
					program = new DrumPreset(folder);
					for (int i = 0; i < samples.length; i++)
						if (program.get(i) != null)
							samples[i].setRecording(program.get(i));
					MainFrame.update(Program.first(JudahZone.getDrumMachine(), actives.getChannel()));
				} catch (Exception e) {
					RTLogger.warn(this, e);
				}
				return true;
			}
		}
		return false;
	}

	public int getChannel() {
		return actives.getChannel();
	}

	@Override
	public void process(FloatBuffer left, FloatBuffer right) {
		// TODO Auto-generated method stub

	}

	public void process() {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumSample drum: samples)
			drum.process(left, right);
		fx();
	}

	public DrumSample getSample(DrumType type) {
		for (DrumSample s : samples)
			if (s.getDrumType() == type)
				return s;
		return null;
	}



}
