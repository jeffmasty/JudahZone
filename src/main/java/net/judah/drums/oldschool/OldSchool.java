package net.judah.drums.oldschool;

import java.io.IOException;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.data.Env;
import judahzone.data.Recording;
import judahzone.data.Stage;
import judahzone.fx.Gain;
import judahzone.gui.Icons;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import net.judah.drums.DrumInit;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumMachine;
import net.judah.drums.DrumType;
import net.judah.drums.KitDB;
import net.judah.drums.KitDB.KitSetup;
import net.judah.drums.gui.DrumKnobs;
import net.judah.drums.gui.SampleDrums;
import net.judah.gui.MainFrame;
import net.judah.seq.track.Computer;

public class OldSchool extends DrumKit {
	public static final int SAMPLES = DrumType.values().length;
	private final DrumSample[] samples = new DrumSample[SAMPLES];

	private SampleDrums knobs;

	public OldSchool(DrumMachine engine, DrumInit type) {
		super(type.name(), engine, type);
		icon = Icons.get("DrumMachine.png");
		for (int i = 0; i < SAMPLES; i++)
			samples[i] = new DrumSample(DrumType.values()[i], actives);
	}

	@Override public void accept(KitSetup t) {
		setChoked(t.choke());
		setKitName(t.name());
		for (int i = 0; i < samples.length; i++) { // preamp not used in legacy kits
			samples[i].getGain().set(Gain.VOLUME, (int)(100 * t.gains()[i].vol()));
			samples[i].getGain().set(Gain.PAN, t.gains()[i].pan());
			samples[i].setAttack(t.env()[i].attack());
			samples[i].setDecay(t.env()[i].decay());
		}
		if (knobs != null)
			MainFrame.update(knobs);
	}

	@Override public KitSetup get() {
		Stage[]	gains = new Stage[SAMPLES];
		Env[] envs = new Env[SAMPLES];

		for (int i= 0; i < SAMPLES; i++) {
			DrumSample s = samples[i];
			gains[i] = new Stage(s.getGain().get(Gain.VOLUME) * 0.01f, s.getGain().get(Gain.PAN));
			envs[i] = new Env(s.getAttack(), s.getDecay());
		}

		return new KitSetup(getKitName(), isChoked(), gains, envs, null);
	}

	@Override public Gain getGain(DrumType type) {
		for (DrumSample sample : samples) {
			if (sample.getDrumType() == type)
				return sample.getGain();
		}
		return null;
	}

	@Override public void send(MidiMessage msg, long timeStamp) {
		if (msg instanceof MetaMessage)
			return; // TODO

		ShortMessage midi = (ShortMessage)msg;
		if (cc.process(midi))
			return;

		if (!Midi.isNoteOn(midi))
			return; // ignore note-off, ignore progChange

		for (DrumSample sample : samples) {
			if (sample.getDrumType().getData1() != midi.getData1()) continue;
			sample.setVelocity(Constants.midiToFloat(midi.getData2()));
			sample.reset();
			sample.play(true);
			if (sample.getDrumType() == DrumType.CHat && isChoked())
				if (actives.indexOf(CHOKE.getData1()) >= 0 || samples[DrumType.OHat.ordinal()].isPlaying())
					samples[DrumType.OHat.ordinal()].off();


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

	@Override public void close() {
		for (DrumSample s : samples)
			s.off();
	}

	@Override public String progChange(int data1) {
		for (DrumSample s : samples)
			s.play(false);
		String[] kits = DrumDB.getPatches();
		if (data1 < 0 || data1 >= kits.length)
			return null;
		if (progChange(kits[data1]));
			return kits[data1];
	}

	@Override public boolean progChange(String name) {
		for (DrumSample s : samples)
			s.play(false);

		Recording[] pads = DrumDB.getKits().get(name);
		if (pads != null) {
			for (int i = 0; i < SAMPLES; i++)
				if (pads[i] != null)
					samples[i].setRecording(pads[i]);
			MainFrame.updateTrack(Computer.Update.PROGRAM, drumMachine.getTrack(this));
			return true;
		}
		return false;
	}

	@Override public int getChannel() {
		return actives.getChannel();
	}

	public DrumSample getSample(DrumType type) {
		for (int i = 0; i < SAMPLES; i++)
			if (samples[i].getDrumType() == type)
				return samples[i];
		return null;
	}

	@Override public DrumKnobs getKnobs() {
		if (knobs == null)
			knobs = new SampleDrums(this);
		return knobs;
	}

	@Override public void save(String name) {
		if (name == null || name.isBlank())
			name = getProgram();
		setKitName(name);
		try {
			KitDB.addOrReplace(get(), false);
		} catch (IOException e) {
			RTLogger.warn(e);
		}
	}

	@Override protected void processImpl() {
		for (int i = 0; i < SAMPLES; i++)
			samples[i].process(left, right);
	}

	@Override public String[] getPatches() {
		return drumMachine.getSamplePresets();
	}

}
