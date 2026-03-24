package net.judah.drums;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.DrumType;
import judahzone.api.Midi;
import judahzone.api.MidiOut.MidiDrums;
import judahzone.api.MidiUpdate;
import judahzone.data.ProgChange;
import judahzone.drums.Drum;
import judahzone.drums.DrumDB;
import judahzone.drums.DrumJson;
import judahzone.drums.DrumsetJson;
import judahzone.drums.ZoneDrums;
import judahzone.drums.fm.FMOsc;
import judahzone.drums.noise.NoiseOsc;
import judahzone.drums.pluck.PluckOsc;
import judahzone.drums.wav.WavDrum;
import judahzone.util.AudioTools;
import judahzone.util.Flags.BOOL8;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.LineIn;
import net.judah.midi.ChannelCC;

public final class Drumset extends LineIn implements MidiDrums, Consumer<DrumsetJson>, Supplier<DrumsetJson> {

	public static final int LENGTH = DrumType.values().length; // todo 48 in General Midi (6 drumsets)
	protected static final ShortMessage CHOKE = Midi.create(Midi.NOTE_OFF, DrumType.OHat.getData1(), 1);

	@Getter private DrumsetJson settings;

	private final DrumDB db;
	private final ChannelCC cc = new ChannelCC(this); // move to Channel Wrapper, filtered Pedal?
	/** true if OHat shuts off when CHat plays */
	@Getter private boolean choked = true;
	@Setter private ProgChange program; // live truth

	// MidiOut.getProgram() contract returns String
	@Override public String getProgram() {
		return program == null ? "" : program.name();
	}

	private final Drum[] drums = new Drum[LENGTH];
	@Getter private BOOL8 actives = new BOOL8();
	@Setter private MidiUpdate ping; // UI callback

	// public Drumset(String name, DrumsetJson settings, MidiUpdate ping) {
	public Drumset(String name, DrumDB drumDB, int i, MidiUpdate ping) {
		super(name, true);
		this.ping = ping;
		this.db = drumDB;
		DrumsetJson settings = drumDB.get(i);
		accept(settings);
	}


	@Override
	public int getProgChange() {
		return program == null ? -1 : program.idx();
	}

	public int[] getActiveSnapshot() {
		BOOL8 cas = new BOOL8(actives.toByte());
		int[] result = new int[cas.count()];
		int idx = 0;
		for (int i = 0; i < LENGTH; i++)
			if (cas.test(i))
				result[idx++] = DrumType.values()[i].getData1();
		return result;
	}

	public Drum get(DrumType type) { return get(type.ordinal()); }

	public Drum get(int idx) { return drums[idx]; }

	@Override
	public String progChange(int data1) { // not final: Wav custom
		DrumsetJson kit = db.get(data1);
		if (kit == null)
			return null;
		accept(kit);
		return kit.program().name();
	}

	@Override
	public int progChange(String name) { // not final: Wav custom
		DrumsetJson kit = db.get(name);
		if (kit == null)
			return -1;
		accept(kit);
		return db.indexOf(name);
	}

	public final void save(ProgChange p) {
		program = p;
		try {
			db.addOrReplace(get(), true);
		} catch (IOException e) {
			RTLogger.warn(this, e);
		}
	}

	@Override
	public void accept(DrumsetJson kit) {
		this.settings = kit;
		choked = kit.choke();
		program = kit.program();

		for (int i = 0; i < LENGTH; i++) {
			DrumJson json = settings.drums()[i];
			if (drums[i] == null || ZoneDrums.ofDrum(drums[i]) != json.reg())
				drums[i] = factory(json);
			drums[i].accept(json);
		}

	}

	private Drum factory(DrumJson json) {
		return switch (json.reg()) {
			case FM -> new FMOsc(json, this, ping);
			case Pluck -> new PluckOsc(json, this, ping);
			case Noise -> NoiseOsc.factory(json, this, ping);
			case Wav -> new WavDrum(json, this, ping);
			default -> {
				RTLogger.warn(this, "Unknown drum type: " + json.reg());
				yield new WavDrum(json, this, ping);
			}
		};

	}


	@Override
	public DrumsetJson get() {
		DrumJson[] mine = new DrumJson[LENGTH];
		for (int i= 0; i < LENGTH; i++) {
			Drum drum = get(i);
			mine[i] = drum.get();
		}
		return new DrumsetJson(
				program, choked,
				mine);
	}

	public void setChoked(boolean tOrF) {
		if (tOrF == choked)
			return;
		this.choked = tOrF;
	}

	@Override public final void processImpl() {
		if (onMute)
			return;
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (int i = 0; i < LENGTH; i++)
			get(i).process(left, right);
		fx();
	}

	@Override
	/** Handle MIDI input: process CCs, trigger notes with choke logic, and update active state. */
	public final void send(MidiMessage msg, long timeStamp) {
		if (msg instanceof MetaMessage)
			return;

		ShortMessage midi = (ShortMessage) msg;
		if (cc.process(midi)) return;

		int data1 = midi.getData1();
		int idx = DrumType.index(data1);
		if (idx < 0)
			return;

		Drum drum = get(idx);
		if (Midi.isNoteOn(midi)) {
			drum.trigger(midi); // send callback here,
			if (isChoked() && drum.getType() == DrumType.CHat) {
				Drum ohat = get(DrumType.OHat.ordinal());
				if (ohat.isPlaying())
					ohat.off();
			}
		} else if (Midi.isNoteOff(midi)) {
			drum.off();
		}
	}

	@Override
	public void off(int data1) {
		int idx = DrumType.index(data1);
		if (idx < 0 || idx >= LENGTH) {
			RTLogger.warn(this, "Invalid drum MIDI data1: " + data1);
			return;
		}
		actives.clear(idx);
		if (ping != null) // test
			ping.updateMidi(this);
	}

	@Override
	public void on(int data1) {
		int idx = DrumType.index(data1);
		if (idx < 0 || idx >= LENGTH) {
			RTLogger.warn(this, "Invalid drum MIDI data1: " + data1);
			return;
		}
		actives.set(idx);
		if (ping != null) // test
			ping.updateMidi(this);
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


	@Override public int[] actives() {
		// CAS
		BOOL8 snapshot = new BOOL8(actives.toByte());
		int[] result = new int[snapshot.count()];
		int idx = 0;
		for (int i = 0; i < LENGTH; i++)
			if (snapshot.test(i))
				result[idx++] = DrumType.values()[i].getData1();
		return result;
	}

	@Override
	public String[] getPatches() {
		return db.getPatches();
	}

	public void replace(DrumJson next) {
		int idx = next.type().ordinal();
		drums[idx].release();
		drums[idx].offOff();
		drums[idx] = factory(next); // discontinuities
		drums[idx].accept(next);
	}



}
