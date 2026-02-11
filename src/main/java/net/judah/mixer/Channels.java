package net.judah.mixer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.swing.JComboBox;

import org.jaudiolibs.jnajack.JackPort;

import com.fasterxml.jackson.core.type.TypeReference;

import judahzone.api.AudioEngine;
import judahzone.api.AudioEngine.IO;
import judahzone.api.AudioEngine.Provider;
import judahzone.api.AudioEngine.Request;
import judahzone.api.AudioEngine.Type;
import judahzone.api.AudioEngine.Wrapper;
import judahzone.api.Custom;
import judahzone.api.Notification.Property;
import judahzone.api.TimeListener;
import judahzone.gui.Gui;
import judahzone.util.Folders;
import judahzone.util.JsonUtil;
import judahzone.util.RTLogger;
import judahzone.util.Services;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.Instrument;
import net.judah.channel.LineIn;
import net.judah.drums.DrumKit;
import net.judah.drums.oldschool.OldSchool;
import net.judah.drums.synth.DrumSynth;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.FxPanel;
import net.judah.looper.Loop;
import net.judah.midi.JudahClock;
import net.judah.midi.MidiInstrument;
import net.judah.song.FxData;
import net.judah.synth.ZoneMidi;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.TacoTruck;

/**
 * Channel Registry: consolidates system and user-registered channels into
 * a unified collection. Provides filtered views (sys vs user) and tracks
 * custom channel registrations for persistence.
 */
public class Channels implements Consumer<LineIn>, TimeListener, Closeable {

	/** Lightweight event emitter for channel lifecycle. */
	public static interface MixBus {
		/** Called when a channel is added to the registry. */
		void channelAdded(LineIn ch);
		/** Called when a channel is removed from the registry. */
		void channelRemoved(LineIn ch);
		/** Called when a channels are re-ordered */
		void reordered();
		/** Called when a channel gets visibility/icon/preamp/filter changes */ // name or port changes = regen
		void update(LineIn ch);
	}

	private final JudahZone zone;
	private JudahClock clock;

	@Getter private final ArrayList<LineIn> audio = new ArrayList<>();
	@Getter private All all;
	/** temporary, bare-bones registrations, awaiting realized ports. */
	private final List<LineIn> registrations = new ArrayList<>();
	private final ChannelEmitter emitter = new ChannelEmitter();
	@Setter @Getter private volatile File userFile;
	@Getter private Instrument guitar;
	@Getter private Instrument mic;
	@Getter private MidiInstrument bass;
	private boolean saveOnCreate;

	public Channels(JudahZone zone, File registry) {
		this.zone = zone;
		this.userFile = registry;
		Services.add(this);
	}

	/** TimeSync tempo updates to all channels (lfo/chorus/delay) */
	public void setClock(JudahClock clock) {
		this.clock = clock;
		clock.addListener(this);
	}

	/** AudioEngine open to create ports only*/
	public void initialize(Type type) throws Exception {

		Provider ports = AudioEngine.getPorts();

		RTLogger.debug(this, "Channel Registrations to process: " + registrations.size());

		for (LineIn line : registrations) { // all CHANNELS need audio ports
			Custom user = line.getUser();
			if (user == null)
				continue; // shouldn't happen
			if (line instanceof Instrument == false)
				continue; // Engine?
			if (type == Type.AUDIO) {
				Instrument audio = (Instrument) line;
		    	String name = user.name();
			    String ourLeft = name + (user.stereo() ? "-left" : "-mono");
			    Wrapper mono = ports.registerNow(Type.AUDIO, IO.IN, ourLeft);
			    if (mono.port() instanceof JackPort jack)
			    	audio.setLeftPort(jack);
			    // else shouldn't happen
		    	if (user.stereo()) {
		    		Wrapper stereo = ports.registerNow(Type.AUDIO, IO.IN, name + "-right");
		    		if (stereo.port() instanceof JackPort jackRight)
		    			audio.setRightPort(jackRight);
		    		// else shouldn't happen
		    	}
			}
			else if (type == Type.MIDI) {
				if (user.midiPort() == null || user.midiPort().isBlank())
					continue;
				if (line instanceof MidiInstrument == false)
					continue; // shouldn't happen
				MidiInstrument midi = (MidiInstrument) line;
		    	String name = user.name();
				Wrapper ourMidi = ports.registerNow(Type.MIDI, IO.OUT, name + "-midi");
				if (ourMidi.port() instanceof JackPort jackMidi)
					midi.setMidiPort(jackMidi);
				// else shouldn't happen
			}
		}
	}

	public void makeConnections(Type type) throws Exception {
		Provider ports = AudioEngine.getPorts();

		for (LineIn line : registrations) {
			if (line.getUser() == null)
				continue; // shouldn't happen
			Custom user = line.getUser();
			if (line instanceof Instrument == false)
				continue; // Engine?

			if (type == Type.AUDIO) {
				Instrument audio = (Instrument) line;
				ports.connectNow(audio.getLeftPort(), Type.AUDIO, user.leftPort());
				if (user.stereo())
					ports.connectNow(audio.getRightPort(), Type.AUDIO, user.rightPort());
			}
			else if (type == Type.MIDI) {
				if (user.midiPort() == null || user.midiPort().isBlank())
					continue;
				if (line instanceof MidiInstrument == false)
					continue; // shouldn't happen
				MidiInstrument midi = (MidiInstrument) line;
				ports.connectNow(midi.getMidiPort(), Type.MIDI, user.midiPort());
				if (user.clocked())
					ports.connectNow(zone.getMidi().getClockOut(), type, user.midiPort());
			}
		}
	}

	public void gui(LineIn... system) {
		all = new All();
		all.add(zone.getMains());
		for (Loop l : zone.getLooper())
			all.add(l);
		all.addAll(zone.getLooper());
		for (DrumKit k : zone.getDrumMachine().getKits())
			all.add(k);

		subscribe(all);
		for (LineIn ch : system)
			accept(ch);

		for (LineIn user : new ArrayList<LineIn>(registrations))
			accept(user);

		// helpers
		scanGuitar();
		scanMic();
		scanBass();
	}

	/** Load user-defined registrations from JSON; spawn ChannelHelpers. */
	public void load() {
		if (!userFile.exists()) {
			synchronized (this) {
				registrations.clear();
			}
			return;
		}
		try {
			List<Custom> parsed = JsonUtil.MAPPER.readValue(userFile,
					new TypeReference<List<Custom>>() {});
			synchronized (this) {
				registrations.clear();
				for (Custom user : parsed)
					registrations.add(factory(user));
			}
		} catch (IOException e) {
			RTLogger.warn(this, "User Channels: " + e.getMessage());
		}
	}

	/** Persist user channels to JSON. */
	public void save() throws IOException {
		if (userFile == null)
			userFile = Folders.getUserChannels();
		synchronized (this) {

			ArrayList<Custom> users = new ArrayList<>();
			for (LineIn user : getUserChannels())
				if (user.getUser() != null)
					users.add(user.getUser());
			JsonUtil.writeJson(users, userFile);
		}
	}

	/**
	 * Called by ChannelHelper when LineIn runtime is ready. Installs into all,
	 * adds to mixer if configured, notifies combo.
	 */
	@Override
	public void accept(LineIn line) {
		if (line == null)
			return;

		registrations.remove(line);
		synchronized (this) {
			audio.add(line);
		}

		emitter.emitAdded(line);

		if (saveOnCreate) {
			try {
				save();
			} catch (IOException e) {
				RTLogger.warn(this, "Saving user channels: " + e.getMessage());
			}
			saveOnCreate = false;
		}
	}

	public void remove(Custom selectedCustom) {
		for (LineIn ch : audio) {
			if (ch instanceof LineIn line) {
				Custom user = line.getUser();
				if (user != null && user.equals(selectedCustom)) {
					remove(ch);
					return;
				}
			}
		}
	}


	/** Remove a channel from all and mixer. */
	public void remove(LineIn ch) {
		if (ch.isSys())
			throw new InvalidParameterException(ch.getName());
		synchronized (this) {
			audio.remove(ch);
		}

		if (ch instanceof Instrument external) {
			Provider ports = AudioEngine.getPorts();
			// start with mono
			Wrapper wrapper = new Wrapper(external.getLeftPort().getName(), external.getLeftPort());
			Request req = new Request(ch.getUser(), wrapper.name(), Type.AUDIO, IO.IN, null);
			ports.unregister(req, wrapper);

			if (ch.isStereo()) {
				wrapper = new Wrapper(external.getRightPort().getName(), external.getRightPort());
				req = new Request(ch.getUser(), wrapper.name(), Type.AUDIO, IO.IN, null);
				ports.unregister(req, wrapper);
			}
			if (ch instanceof MidiInstrument midi) {
				wrapper = new Wrapper(midi.getMidiPort().getName(), midi.getMidiPort());
				req = new Request(ch.getUser(), wrapper.name(), Type.MIDI, IO.OUT, null);
				ports.unregister(req, wrapper);
			}
		}

		emitter.emitRemoved(ch);


	}

	/** Register a new custom channel registration; spawn ChannelHelper. */
	public void createChannel(Custom user, boolean save) {
		LineIn newbie = factory(user);
		registrations.add(newbie);
		new ChannelHelper(this, newbie);

		if (save)
			saveOnCreate();
	}

	/** Query channel by name. */
	public Channel byName(String channel) {
		for (Channel ch : audio)
			if (ch.getName().equals(channel))
				return ch;
		return null;
	}

	/** Get all LineIn channels (system and user). */
	public List<LineIn> getInputs() {
		synchronized (this) {
			return audio.stream()
				.filter(ch -> ch instanceof LineIn)
				.map(ch -> ch)
				.toList();
		}
	}

	/** Get user-defined (non-system) input channels. */
	public List<LineIn> getUserChannels() {
		synchronized (this) {
			return audio.stream()
				.filter(ch -> ch instanceof LineIn && !ch.isSys())
				.toList();
		}
	}

//	/** Get system channels only. */
//	public List<Channel> getSys() {
//			return all.stream()
//				.filter(ch -> ch.isSys())
//				.toList();
//	}

	/** Load FX presets by channel name from saved song data. */
	public void loadFx(List<FxData> data) {
		if (data == null) return;
		for (FxData fx : data) {
			Channel ch = byName(fx.getChannel());
			if (ch != null)
				ch.setPreset(fx.getPreset());
		}
	}

	// Default recording mutes
	public void mutes() {
		Instrument gtr = getGuitar();
		if (gtr != null)
			gtr.setMuteRecord(false);
		zone.getTaco().setMuteRecord(false);
		zone.getDrumMachine().setMuteRecord(false); // individual kits can capture
	}

	public void swap(LineIn userA, LineIn userB) {
		if (userA.isSys() || userB.isSys())
			throw new InvalidParameterException("Cannot swap system channels");
		int idxA = audio.indexOf(userA);
		int idxB = audio.indexOf(userB);
		if (idxA == -1 || idxB == -1)
			return;
		audio.set(idxA, userB);
		audio.set(idxB, userA);
		emitter.shuffle();
	}

	public void swap(Custom before, Custom after, boolean save) {

		for (LineIn ch : audio) {
			if (ch instanceof LineIn line) {
				Custom user = line.getUser();
				if (user != null && user.equals(before)) {
					line.setUser(after);
					// let's just set filter and preamp, not sure if changed or not
					if (after.preamp() != null)
						line.getGain().setPreamp(after.preamp());
					if (line.isStereo() == false && line instanceof Instrument ins) {
						if (ins.getHp() != null && after.lowCutHz() != null)
							ins.getHp().setFrequency(after.lowCutHz());
						if (ins.getLp() != null && after.highCutHz() != null)
							ins.getLp().setFrequency(after.highCutHz());
					}

					emitter.update(line);
					if (save)
						try {
							save();
						} catch (IOException e) { RTLogger.warn(this, e); }
				}
			}
		}
	}

	/** Apply tempo change to all channels (from clock). */
	@Override
	public void update(Property prop, Object value) {
		if (prop != Property.TEMPO) return;
		float tempo = (float) value;
		for (Channel ch : audio)
			ch.tempo(tempo, clock.syncUnit());
	}

	public void subscribe(MixBus listener) {
		if (emitter.contains(listener))
			return;
		emitter.add(listener);
	}
	public void unsubscribe(MixBus listener) {
		emitter.remove(listener);
	}

	/** Manages ChannelListener subscriptions. */
	public class ChannelEmitter extends CopyOnWriteArrayList<MixBus> {
		/** Notify all listeners that a channel was added. */
		public void emitAdded(LineIn ch) {
			for (MixBus listener : this)
				listener.channelAdded(ch);
		}
		public void shuffle() {
			for (MixBus listener : this)
				listener.reordered();
		}
		/** Notify all listeners that a channel was removed. */
		public void emitRemoved(LineIn ch) {
			for (MixBus listener : this)
				listener.channelRemoved(ch);
		}
		public void update(LineIn line) {
			for (MixBus listener : this)
				listener.update(line);
		}

	}

	public static class All extends CopyOnWriteArrayList<Channel> implements MixBus {
		private final JComboBox<Channel> combo = new JComboBox<>();
		boolean comboOverride = false;

		/** Get or create JComboBox for channel selection (LFO knobs, etc). */
		public JComboBox<Channel> getCombo(Channel selected) {
			combo.setFont(Gui.BOLD13);
			combo.setSelectedItem(selected);
			combo.addActionListener(e -> {
				if (!comboOverride)
					MainFrame.setFocus(combo.getSelectedItem()); });
			return combo;
		}

		/** Refresh combo box after channel add/remove. */
		void updateCombo() {
			if (combo == null) return;
			comboOverride = true;
			combo.removeAllItems();
			for (Channel ch : this)
				combo.addItem(ch);
			FxPanel fx = JudahZone.getInstance().getFxRack();
			if (fx != null && !fx.getSelected().isEmpty())
				combo.setSelectedItem(fx.getSelected().getFirst());
			comboOverride = false;
		}


		@Override
		public boolean add(Channel ch) {
			combo.addItem(ch);
			return super.add(ch);
		}

		@Override
		public boolean remove(Object o) {
			combo.removeItem(o);
			return super.remove(o);
		}

		@Override public void channelAdded(LineIn ch) {
			this.add(ch);
		}
		@Override public void channelRemoved(LineIn ch) {
			this.remove(ch);
		}
		@Override public void reordered() {
			// TODO
		}
		@Override public void update(LineIn ch) { // filter/preamp/icon/visibility changes
		}
	}

	@RequiredArgsConstructor @Getter
	public static enum RegisteredSynths {
		Taco(TacoTruck.class), Fluid(FluidSynth.class), External(MidiInstrument.class);
		public final Class<? extends ZoneMidi> clazz;
	}

	@RequiredArgsConstructor @Getter
	public static enum RegisteredDrums {
		OldSkool(OldSchool.class), Synth(DrumSynth.class);
		private final Class<? extends DrumKit> clazz;
	}

	@Override
	public void close() throws IOException {
		for (Channel ch : audio)
			if (ch instanceof Closeable close)
				close.close();
	}

	public void saveOnCreate() {
		saveOnCreate = true;
	}

	/** First non-fluid user midi-insturment - janky*/
	public void scanBass() {
		for (LineIn ch : getUserChannels())
			if (ch instanceof MidiInstrument midi && midi instanceof FluidSynth == false
				&& ch.getName().equalsIgnoreCase("Bass")) {
				bass = midi;
				return;
			}
	}

	/** Instrument lower-name = "guitar" or "gtr" - janky*/
	public void scanGuitar() {
		for (LineIn ch : audio)
			if (ch instanceof Instrument inst) {
				String name = inst.getName().toLowerCase();
				if (name.equals("guitar") || name.equals("gtr")) {
					guitar = inst;
					return;
				}
			}
	}

	public void scanMic() {
		for (LineIn ch : audio)
			if (ch instanceof Instrument inst) {
				String name = inst.getName().toLowerCase();
				if (name.equals("mic") || name.equals("vocals")) {
					mic = inst;
					zone.getLooper().getSoloTrack().setSoloTrack(mic);
					return;
				}
			}
	}

	public static LineIn factory(Custom user) {
		boolean midi = user.midiPort() != null && user.midiPort().isBlank() == false;
		if (user.engine() != null && Channels.RegisteredSynths.valueOf(user.engine()) != null) {
			// TODO lookup AddTrack logic
			RTLogger.warn("ChannelFactory", "Engine-backed synths not yet supported in factory(): " + user.engine());
			return null;
		}

		if (midi)
			return new MidiInstrument(user);

		else
			return new Instrument(user);

	}

}

//public boolean nextChannel(boolean toRight) { // qwerty arrow keys?
//	Looper looper = JudahZone.getInstance().getLooper();
//    Channel bus = JudahZone.getInstance().getFxRack().getChannel();
//    if (bus instanceof Instrument) {
//        int i = indexOf(bus);
//        if (toRight) {
//            if (i == size() -1) {
//                setFocus(looper.get(0));
//                return true;
//            }
//            setFocus(get(i + 1));
//            return true;
//        } // else toLeft
//        if (i == 0) {
//            setFocus(looper.get(looper.size()-1));
//            return true;
//        }
//        setFocus(get(i - 1));
//        return true;
//    }
//    // else instanceof Sample
//    int i = looper.indexOf(bus);
//    if (toRight) {
//        if (i == looper.size() - 1) {
//            setFocus(get(0));
//            return true;
//        }
//        setFocus(looper.get(i + 1));
//        return true;
//    } // else toLeft
//    if (i == 0) {
//        setFocus(get(size() - 1));
//        return true;
//    }
//    setFocus(looper.get(i - 1));
//    return true;
//}

