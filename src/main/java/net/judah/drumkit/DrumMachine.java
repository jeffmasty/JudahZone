package net.judah.drumkit;

import java.io.File;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Engine;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.omni.AudioTools;
import net.judah.omni.Icons;
import net.judah.seq.Trax;
import net.judah.seq.track.Cue;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;

@Getter
public class DrumMachine extends Engine {

	private final Channel mains;
	private final DrumTrack drum1, drum2, hats, fills;

	/** current midi controller input and view */
	@Setter private DrumTrack current;
	private KitKnobs focus;
	private final KnobMode knobMode = KnobMode.KITS;

	public DrumMachine(JackPort outL, JackPort outR, JudahClock clock, Channel mains) throws Exception {
		super("Drums", true);
		this.mains = mains;
		icon = Icons.get("DrumMachine.png");
		leftPort = outL;
		rightPort = outR;

		drum1 = new DrumTrack(new DrumKit(this, Trax.D1, "Pearl"), clock);
		drum2 = new DrumTrack(new DrumKit(this, Trax.D2, "808"), clock);
		hats = new DrumTrack(new DrumKit(this, Trax.H1, "Hats"), clock);
		fills = new DrumTrack(new DrumKit(this, Trax.H2, "VCO"), clock);
		tracks.add(drum1);
		tracks.add(drum2);
		tracks.add(hats);
		tracks.add(fills);
		focus = drum1.getKit().getGui();
		current = drum1;
		setPreamp(0.3f);
	}

	public void init() {
		setPreset("Drumz");
        drum1.load(new File(drum1.getFolder(), "Rock1"));
        drum2.load(new File(drum2.getFolder(), "Bossa1"));
        hats.load(new File(hats.getFolder(), "Hats1"));
        fills.load(new File(fills.getFolder(), "Fills1"));
        fills.setCue(Cue.Hot);
	}

	public KitKnobs getKnobs() {
		return current.getKit().getGui();
	}

	public KitKnobs getKnobs(Trax mode) {
		for (int i = 0; i < tracks.size(); i++) {
			if (((DrumTrack)tracks.get(i)).getKit().getKitMode() == mode)
				return ((DrumTrack)tracks.get(i)).getKit().getGui();
		}
		return drum1.getKit().getGui(); // error
	}

	@Override
	public void reset() {
		super.reset();
		for(MidiTrack t : tracks)
			for (DrumSample s : ((DrumTrack)t).getKit().getSamples())
				s.reset();
	}

	public void increment() {
		int idx = tracks.indexOf(current) + 1;
		if (idx>= tracks.size())
			idx = 0;
		current = (DrumTrack)tracks.get(idx);
		MainFrame.setFocus(current.getKit().getFx());
		MainFrame.setFocus(current.getKit().getGui());
	}

	@Override
	public boolean progChange(String preset, int channel) {
		return getChannel(channel).progChange(preset);
	}

	@Override
	public String[] getPatches() {
		return DrumDB.getKits().toArray(new String[DrumDB.getKits().size()]);
	}

	@Override
	public String getProg(int ch) {
		for (int i = 0; i < tracks.size(); i++)
			if (tracks.get(i).getCh() == ch)
				return ((DrumTrack)tracks.get(i)).getKit().getPreset().getFolder().getName();
			return "?";
	}

	@Override
	public boolean progChange(String preset) {
		return drum1.getKit().progChange(preset);
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (onMute || mains.isOnMute())
			return; // discard
		if (false == Midi.isNoteOn(message))
			return;
		ShortMessage midi = Midi.copy((ShortMessage)message);
		getChannel(midi.getChannel()).send(midi);
	}

	public DrumKit getChannel(int channel) {
		for (MidiTrack t : tracks)
			if (t.getCh() == channel)
				return ((DrumTrack)t).getKit();
		return null;
	}

	public void focus (Trax mode) {
		if (MainFrame.getKnobMode() != knobMode)
			MainFrame.setFocus(knobMode);
		KitKnobs target = getKnobs(mode);
		if (focus == target)
			return;
		focus = target;
		MainFrame.setFocus(target);
	}


	@Override
	public void close() {
		reset();
	}

	///////////////////////////////////////////////
	// process + mix each drumkit, process this channel's fx, place on mains
	@Override
	public void process() {
		AudioTools.silence(left);
		AudioTools.silence(right);
		DrumKit kit;
		if (onMute) {
			for (MidiTrack track : tracks)
				((DrumTrack)track).getKit().process();
		}
		else {
			for (MidiTrack track : tracks) {
				kit = ((DrumTrack)track).getKit();
				kit.process();
				AudioTools.mix(kit.getLeft(), left);
				AudioTools.mix(kit.getRight(), right);
			}
			processStereoFx(gain.getGain() * preamp);
		}
		AudioTools.mix(left, leftPort.getFloatBuffer());
		AudioTools.mix(right, rightPort.getFloatBuffer());
	}

}
