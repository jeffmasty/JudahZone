package net.judah.channel;

import judahzone.api.Custom;
import judahzone.api.MidiUpdate;
import judahzone.fx.op.Kernel;
import judahzone.gui.Icons;
import judahzone.synth.Synth;
import judahzone.synth.Synth.SynthType;
import judahzone.synth.SynthDB;
import judahzone.synth.SynthJson;
import judahzone.synth.WavSynth;
import lombok.Getter;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.synth.SynthKnobs;
import net.judah.seq.chords.Chords;
import net.judah.seq.track.PianoTrack;

/** An Generator is a zone synth source with pass filters */
@Getter
public class Generator extends LineIn {

	public static final SynthJson GENERIC = new SynthJson("Mine", Kernel.GENERIC, SynthType.Wav, WavSynth.ALGO);

    private final Synth synth;
    private final PianoTrack track;
    private KnobPanel knobs;

    public Generator(Custom user, SynthJson synthJson, MidiUpdate ping, Chords chords) {
    	super(user.name(), user.stereo());
    	synth = Synth.factory(SynthDB.get(0), ping);
    	track = new PianoTrack(user.name(), 0, this, synth, chords);
    	setUser(user);
    }

    @Override
	public void setUser(Custom user) {
    	super.setUser(user);
    	if (user.iconName() == null	)
			icon = Icons.get("Gear.png");
		else
			icon = Icons.get(user.iconName());
//    	if (user.highCutHz() != null)
//    		synth.getHiCut().setFrequency(user.highCutHz());
//    	if (user.lowCutHz() != null)
//    		synth.getLowCut().setFrequency(user.lowCutHz());
    }

	@Override protected final void processImpl() {
		if (isOnMute())
			return;
		// get raw data
		synth.process(left, right);
		gain.preamp(left, right);
		hotSwap(); // activate FX
		for (int i = 0, n = active.size(); i < n; i++)
			active.get(i).process(left, right);
		gain.post(left, right);
	}

	public KnobPanel getKnobs() {
		if (knobs == null) // lazy
			knobs = new SynthKnobs((WavSynth)synth, this);
		return knobs;
	}
}
