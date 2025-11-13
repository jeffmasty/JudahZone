package net.judah.drumkit;

import java.io.File;
import java.nio.FloatBuffer;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.Program;
import net.judah.midi.Actives;
import net.judah.midi.ChannelCC;
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
	private final KnobMode knobMode = KnobMode.Kitz;
	private final Actives actives;
	private final Trax type;
	private final DrumSample[] samples = new DrumSample[SAMPLES];
	private final ShortMessage CHOKE = Midi.create(Midi.NOTE_OFF, DrumType.OHat.getData1(), 1);
	private ChannelCC cc = new ChannelCC(this);

	/** true if OHat shuts off when CHat plays */
	@Setter private boolean choked = true;
	private DrumPreset program;

	public DrumKit(DrumMachine engine, Trax type) {

		super(type.name(), Constants.STEREO);
		this.type = type;
		actives = new Actives(engine, type.getCh());
		for (int i = 0; i < SAMPLES; i++)
			samples[i] = new DrumSample(DrumType.values()[i], actives, engine.getSettings());
		progChange(type.getProgram());
	}

	@Override
	public void send(MidiMessage msg, long timeStamp) {
		if (msg instanceof MetaMessage)
			return; // TODO

		ShortMessage midi = (ShortMessage)msg;
		if (cc.process(midi))
			return;

		if (!Midi.isNoteOn(midi))
			return; // ignore note-off, ignore progChange

		for (DrumSample sample : samples) {
			if (sample.getDrumType().getData1() != midi.getData1()) continue;
			sample.getTapeCounter().set(0); // rewind();
			sample.setVelocity(Constants.midiToFloat(midi.getData2()));
			sample.getEnvelope().reset();
			sample.play(true);
			if (sample.getDrumType() == DrumType.CHat && choked) {
				if (actives.indexOf(CHOKE.getData1()) >= 0 || samples[DrumType.OHat.ordinal()].isPlaying())
					samples[DrumType.OHat.ordinal()].off();
			}

			int idx = actives.indexOf(midi.getData1());
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
			if (folder.isDirectory() && folder.getName().equals(name)) {
				try {
					program = new DrumPreset(folder);
					for (int i = 0; i < samples.length; i++)
						if (program.get(i) != null)
							samples[i].setRecording(program.get(i));
					if (JudahZone.isInitialized())
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
	public void process(FloatBuffer outLeft, FloatBuffer outRight) {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumSample drum: samples)
			drum.process(left, right);
		fx();
		if (onMute)
			return;
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}

	public DrumSample getSample(DrumType type) {
		for (DrumSample s : samples)
			if (s.getDrumType() == type)
				return s;
		return null;
	}

}
