package net.judah.drums;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.data.Letter;
import judahzone.data.Postage;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.EQ.EqBand;
import judahzone.fx.Gain;
import judahzone.fx.Gain.GainT;
import judahzone.prism.MidiCmdPack;
import judahzone.util.AudioTools;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.LineIn;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.Drum.NonPitched;
import net.judah.drums.KitDB.KitSetup;
import net.judah.drums.KitDB.OscSetup;
import net.judah.gui.MainFrame;
import net.judah.gui.drums.knobs.KitKnobs;
import net.judah.midi.Actives;
import net.judah.midi.ChannelCC;
import net.judah.mixer.Channels.RegisteredDrums;

public abstract class DrumKit extends LineIn implements Receiver, Consumer<KitSetup>, Supplier<KitSetup> {
	protected static final ShortMessage CHOKE = Midi.create(Midi.NOTE_OFF, DrumType.OHat.getData1(), 1);

	@Getter protected final RegisteredDrums reg; // remove on PHASE II generalization
	public static final int LENGTH = DrumType.values().length;

	protected final DrumMachine drumMachine;
	@Getter protected final Actives actives;
	@Getter private final DrumInit type;
	@Getter @Setter private KitSetup settings;
	protected final ChannelCC cc = new ChannelCC(this);
	/** true if OHat shuts off when CHat plays */
	@Getter private boolean choked = true;
	@Getter @Setter private String kitName; // synth: same as program / legacy: nullable preset
	protected KitKnobs knobs;

	public DrumKit(String name, DrumMachine engine, DrumInit kit) {
		super(name, true);
		this.drumMachine = engine;
		this.type = kit;
		this.reg = typeLookup();
		this.actives = new Actives(engine, kit.ch);
	}

	private RegisteredDrums typeLookup() {
		for (RegisteredDrums t : RegisteredDrums.values())
			if (t.getClazz().equals(getClass()))
				return t;
		throw new InvalidParameterException("Unregistered drum kit class: " + getClass().getSimpleName());
	}

	public Drum get(DrumType type) { return get(type.ordinal()); }

	public abstract Drum get(int idx);

	public String progChange(int data1) { // not final: Wav custom
		KitSetup kit = KitDB.get(data1, reg);
		if (kit == null)
			return null;
		accept(kit);
		return kit.name();
	}

	public boolean progChange(String name) { // not final: Wav custom
		KitSetup kit = KitDB.get(name, reg);
		if (kit == null)
			return false;
		accept(kit);
		return true;
	}

	public String[] getPatches() { // not final: Wav custom
		return KitDB.getPatches(reg);
	}


	public final void save(String name) {
		if (name == null || name.isBlank())
			name = getType().name();
		setKitName(name);
		try {
			KitDB.addOrReplace(get(), reg);
		} catch (IOException e) {
			RTLogger.warn(this, e);
		}
	}

	@Override
	public void accept(KitSetup kit) {
		this.settings = kit;
		setChoked(kit.choke());
		setKitName(kit.name());

		for (int i = 0; i < LENGTH; i++) {
			Drum drum = get(i);
			GainT stage = kit.gains()[i];
			Gain g = drum.getGain();
			g.setGain(stage.gain());
			g.setPan(stage.pan());
			g.setPreamp(gain.getPreamp());
			Postage env = kit.env()[i];
			drum.getEnv().sendLetter(new Letter(env));

			Coord low = kit.lowCut()[i];
			Coord high = kit.hiCut()[i];
			FilterT body = kit.body()[i];

			drum.setHz(EqBand.Bass, low.hz());
			drum.setHz(EqBand.Mid, body.hz());
			drum.setHz(EqBand.High, high.hz());
			drum.setResonance(EqBand.Bass, low.reso());
			drum.setResonance(EqBand.Mid, body.dB());
			drum.setResonance(EqBand.High, high.reso());
			drum.getMid().setBandwidth(body.bandwidth());

		}
		MainFrame.update(this);
	}

	@Override
	public KitSetup get() {

		GainT[] gains = new GainT[LENGTH];
		Postage[] env = new Postage[LENGTH];
		Coord[] loCut = new Coord[LENGTH];
		Coord[] hiCut = new Coord[LENGTH];
		FilterT[] body = new FilterT[LENGTH];

		for (int i= 0; i < LENGTH; i++) {
			Drum drum = get(i);
			gains[i] = drum.getGain().get();
			env[i] = new Postage(drum.getEnv().getAttackMs(), drum.getEnv().getDecayMs());
			Freqs freqs = drum.getFreqs();
			loCut[i] = freqs.lowCut();
			hiCut[i] = freqs.hiCut();
			body[i] = freqs.body();
		}

		return new KitSetup(getKitName(), isChoked(), gains, env, loCut, body, hiCut, null, null, null, null);

	}

	public static OscSetup[] pitch(Drum[] drums) {
		OscSetup[] result = new OscSetup[drums.length];
		for (int i = 0; i < drums.length; i++) {
			if (drums[i] instanceof NonPitched)
				result[i] = new OscSetup(true, 440);
			else
				result[i] = new OscSetup(drums[i].isSync(), drums[i].getFundamental());
		}
		return result;

	}

	public String getProgram() {
		return drumMachine.getTrack(this).getProgram();
	}

	public void setChoked(boolean tOrF) {
		if (tOrF == choked)
			return;
		this.choked = tOrF;
		MainFrame.update(this);
	}

	public int getChannel() {
		return actives.getChannel();
	}

	@Override protected final void processImpl() {
		if (onMute)
			return;
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (int i = 0; i < LENGTH; i++)
			get(i).process(left, right);
		fx();
		// unmixed to master (could be multi-threaded)
	}

	public final KitKnobs getKnobs() {
		if (knobs == null)
			knobs = new KitKnobs(this);
		return knobs;
	}

	@Override
	/** Handle MIDI input: process CCs, trigger notes with choke logic, and update active state. */
	public final void send(MidiMessage msg, long timeStamp) {
		if (msg instanceof MetaMessage)
			return;

		ShortMessage midi = (ShortMessage) msg;
		if (cc.process(midi)) return;

		int data1 = midi.getData1();
		int data2 = midi.getData2();
		int idx = DrumType.index(data1);
		if (idx < 0)
			return;

		Drum drum = get(idx);
		if (Midi.isNoteOn(midi)) {
			drum.trigger(midi);
			if (isChoked() && drum.getType() == DrumType.CHat) {
				Drum ohat = get(DrumType.OHat.ordinal());
				if (actives.isActive(CHOKE.getData1()) || ohat.isPlaying()) ohat.off();
			}
			actives.receivePacked(MidiCmdPack.pack(
					MidiCmdPack.CMD_ADD, data1, data2, actives.getChannel()));
		} else if (Midi.isNoteOff(midi)) {
			drum.off();
			actives.receivePacked(MidiCmdPack.pack(
					MidiCmdPack.CMD_REMOVE, data1, 0, actives.getChannel()));
		}
	}

	public final boolean hasWork() {
		for (int i = 0; i < LENGTH; i++)
			if (get(i).isPlaying())
				return true;
		return false;
	}

	@Override public final void close() {
		for (int i = 0; i < LENGTH; i++)
			get(i).off();
	}


}
