package net.judah.drums.synth;

import java.io.IOException;
import java.util.List;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Stage;
import judahzone.fx.EQ.EqBand;
import judahzone.fx.Gain;
import judahzone.util.AudioTools;
import judahzone.util.RTLogger;
import net.judah.drums.DrumInit;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumMachine;
import net.judah.drums.DrumType;
import net.judah.drums.KitDB;
import net.judah.drums.KitDB.KitSetup;
import net.judah.drums.KitDB.SynthCommon;
import net.judah.drums.KitDB.SynthKit;
import net.judah.drums.gui.DrumKnobs;
import net.judah.drums.gui.ZoneDrums;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.gui.MainFrame;

public class DrumSynth extends DrumKit {

	public static final String TOKEN = "_JudahZone_DrumSynth_";

	private final Kick kick;
	private final Snare snare;
	private final Stick stick;
	private final Clap clap;
	private final CHat chat;
	private final OHat ohat;
	private final Ride ride;
	private final Bongo bongo;

	private final List<DrumOsc> drums;
	private ZoneDrums knobs;

	public DrumSynth(DrumMachine engine, DrumInit type) {
		super(type.name(), engine, type);
		kick = new Kick(actives);
		snare = new Snare(actives);
		stick = new Stick(actives);
		clap = new Clap(actives);
		chat = new CHat(actives);
		ohat = new OHat(actives);
		ride = new Ride(actives);
		bongo = new Bongo(actives);
		drums = List.of(kick, snare, stick, clap, chat, ohat, ride, bongo);
		gain.setPreamp(0.25f);
	}

//	@Override
//	public void accept(KitSetup kit) {
//		setChoked(kit.choke());
//		setKitName(kit.name());
//		for (int i = 0; i < drums.size(); i++) {
//			DrumOsc drum = drums.get(i);
//			Stage stage = kit.gains()[i];
//			Gain g = drum.getGain();
//			g.set(Gain.VOLUME, (int)(stage.vol() * 100));
//			g.set(Gain.PAN, stage.pan());
//			Env env = kit.env()[i];
//			drum.setAttack(env.attack());
//			drum.setDecay(env.decay());
//		}
//
//		kick.accept(kit.synth().kick());
//		snare.accept(kit.synth().snare());
//		stick.accept(kit.synth().stick());
//		clap.accept(kit.synth().clap());
//		chat.accept(kit.synth().chat());
//		ohat.accept(kit.synth().ohat());
//		ride.accept(kit.synth().ride());
//		bongo.accept(kit.synth().bongo());
//
//		if (knobs != null)
//			MainFrame.update(knobs);
//		// TODO OneDrumView updates?
//	}
	@Override
	public void accept(KitSetup kit) {
		setChoked(kit.choke());
		setKitName(kit.name());
		SynthCommon common = kit.synth().common();
		Filter[] lo = common.lowCut();
		Filter[] hi = common.hiCut();
		Filter[] pitch = common.pitch();

		for (int i = 0; i < drums.size(); i++) {
			DrumOsc drum = drums.get(i);
			Stage stage = kit.gains()[i];
			Gain g = drum.getGain();
			g.set(Gain.VOLUME, (int)(stage.vol() * 100));
			g.set(Gain.PAN, stage.pan());
			Env env = kit.env()[i];
			drum.setAttack(env.attack());
			drum.setDecay(env.decay());

			drum.setHz(EqBand.Bass, lo[i].hz());
			drum.setHz(EqBand.Mid, pitch[i].hz());
			drum.setHz(EqBand.High, hi[i].hz());
			drum.setResonance(EqBand.Bass, lo[i].reso());
			drum.setResonance(EqBand.Mid, pitch[i].reso());
			drum.setResonance(EqBand.High, hi[i].reso());

		}

		kick.accept(kit.synth().kick());
		snare.accept(kit.synth().snare());
		stick.accept(kit.synth().stick());
		clap.accept(kit.synth().clap());
		chat.accept(kit.synth().chat());
		ohat.accept(kit.synth().ohat());
		ride.accept(kit.synth().ride());
		bongo.accept(kit.synth().bongo());

		if (knobs != null)
			MainFrame.update(knobs);
	}



	@Override public KitSetup get() {
		Stage[] gains = new Stage[drums.size()];
		Env[] env = new Env[drums.size()];

		Filter[] loCut = new Filter[drums.size()];
		Filter[] hiCut = new Filter[drums.size()];
		Filter[] body = new Filter[drums.size()];

		for (int i= 0; i < drums.size(); i++) {
			DrumOsc drum = drums.get(i);
			Gain g = drum.getGain();
			gains[i] = new Stage(g.getGain(), g.get(Gain.PAN));
			env[i] = new Env(drum.getAttack(), drum.getDecay());
			Freqs freqs = drum.getFreqs();
			loCut[i] = freqs.lowCut();
			hiCut[i] = freqs.hiCut();
			body[i] = freqs.body();
		}

		SynthCommon common = new SynthCommon(loCut, hiCut, body);

		return new KitSetup(getKitName(), isChoked(), gains, env,
				new SynthKit(common, kick.get(), snare.get(), stick.get(), clap.get(),
						chat.get(), ohat.get(), ride.get(), bongo.get()));
	}



	public DrumOsc getDrum(DrumType type) {
		switch (type) {
		case Kick: return kick;
		case Snare: return snare;
		case Stick: return stick;
		case Clap: return clap;
		case CHat: return chat;
		case OHat: return ohat;
		case Ride: return ride;
		case Bongo: return bongo;
		default: return null;
		}
	}

	@Override public Gain getGain(DrumType type) {
		switch (type) {
		case Kick: return kick.getGain();
		case Snare: return snare.getGain();
		case Stick: return stick.getGain();
		case Clap: return clap.getGain();
		case CHat: return chat.getGain();
		case OHat: return ohat.getGain();
		case Ride: return ride.getGain();
		case Bongo: return bongo.getGain();
		default: return null;
		}

	}

	// trigger a drum oscillator
	@Override public void send(MidiMessage msg, long timeStamp) {
		if (msg instanceof MetaMessage)
			return; // TODO

		ShortMessage midi = (ShortMessage)msg;
		if (cc.process(midi))
			return;

		if (!Midi.isNoteOn(midi))
			return; // ignore note-off, ignore progChange

		int idx = DrumType.index(midi.getData1()); // alt?
		if (idx < 0)
			return; // not handled

		getDrum(DrumType.values()[idx]).trigger(midi.getData2());

		idx = actives.indexOf(midi.getData1());
		if (idx < 0)
			actives.add(midi); // TODO remove after decay
		else
			actives.set(idx, midi);
		MainFrame.update(actives);

	}

	@Override public void close() {
		drums.forEach(DrumOsc::off);
	}

	@Override public void processImpl() {
		AudioTools.silence(left);
		AudioTools.silence(right);

		kick.process(left, right);
		snare.process(left, right);
		stick.process(left, right);
		clap.process(left, right);
		chat.process(left, right);
		ohat.process(left, right);
		ride.process(left, right);
		bongo.process(left, right);

	}

	@Override public String progChange(int data1) {
		KitSetup kit = KitDB.get(data1, true);
		if (kit == null)
			return null;
		accept(kit);
		return kit.name();
	}

	@Override public boolean progChange(String name) {
		KitSetup kit = KitDB.get(name, true);
		if (kit == null)
			return false;
		accept(kit);
		return true;
	}

	@Override public DrumKnobs getKnobs() {
		if (knobs == null)
			knobs = new ZoneDrums(this);
		return knobs;
	}


	@Override public void save(String name) {
		if (name == null || name.isBlank())
			name = getType().name();
		setKitName(name);
		try {
			KitDB.addOrReplace(get(), true);
//			if (name.equals(getProgram()) == false)
//				progChange(name);
		} catch (IOException e) {
			RTLogger.warn(this, e);
		}
	}

	@Override public String[] getPatches() {
		return drumMachine.getSynthPresets();
	}



}
