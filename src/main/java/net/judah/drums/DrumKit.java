package net.judah.drums;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.fx.Gain;
import judahzone.util.AudioTools;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.LineIn;
import net.judah.drums.KitDB.KitSetup;
import net.judah.drums.gui.DrumKnobs;
import net.judah.gui.MainFrame;
import net.judah.midi.Actives;
import net.judah.midi.ChannelCC;

public abstract class DrumKit extends LineIn implements Receiver, Consumer<KitSetup>, Supplier<KitSetup> {
	protected static final ShortMessage CHOKE = Midi.create(Midi.NOTE_OFF, DrumType.OHat.getData1(), 1);

	protected final DrumMachine drumMachine;
	@Getter protected final Actives actives;
	@Getter private final DrumInit type;
	@Getter @Setter private KitSetup settings;
	protected final ChannelCC cc = new ChannelCC(this);
	/** true if OHat shuts off when CHat plays */
	@Getter private boolean choked = true;
	@Getter @Setter private String kitName; // synth: same as program / legacy: nullable preset

	public DrumKit(String name, DrumMachine engine, DrumInit kit) {
		super(name, true);
		this.drumMachine = engine;
		this.type = kit;
		this.actives = new Actives(engine, kit.ch);
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

	public abstract String[] getPatches();

	public int getChannel() {
		return actives.getChannel();
	}

	public abstract Gain getGain(DrumType type);

	public void processKit() {
		if (onMute)
			return;
		AudioTools.silence(left);
		AudioTools.silence(right);

		processImpl();

		fx();
	}

	public abstract boolean progChange(String name);

	public abstract String progChange(int data1);

	public abstract DrumKnobs getKnobs();

	public abstract void save(String name); // no Exception

}
