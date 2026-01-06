package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.sampler.Sampler;

/**	•  SampleTrack: trigger/stop samples in a Sampler via MIDI CC. *
	•  - default mapping: CC baseController .. baseController + STANDARD.length - 1
	•  map to Sampler.STANDARD names.
	•  - CC value > 64 -> play(true), value == 0 -> play(false) (simple threshold).
	•  - Does not allocate in the audio callback path; runs on MIDI thread.
	•  This approach expects Sample.getName() to exist (common pattern where Sample constructed with a name). If the method name differs, adapt accordingly.
	•  Base CC mapping gives a compact way to bind CCs to samples; change baseController when constructing SampleTrack.
	•  Keep CC-to-sample mapping small and deterministic to avoid runtime allocations. */
@Getter public class SampleTrack extends MidiTrack {

private final Sampler sampler;
private final int baseController;

/**
 * @param name track name
 * @param ch midi channel for this track
 * @param sampler sampler instance to control (must be created elsewhere)
 * @param baseController first CC controller used for mapping (e.g. 20)
 */
public SampleTrack(String name, int ch, Sampler sampler, int baseController) throws InvalidMidiDataException {
    super(name, ch);
    this.sampler = sampler;
    this.baseController = baseController;
}

@Override
public void send(MidiMessage midi, long ticker) {
    if (midi instanceof ShortMessage sm && Midi.isCC(midi)) {
        int controller = sm.getData1();
        int value = sm.getData2();
        int idx = controller - baseController;
        if (idx >= 0 && idx < Sampler.STANDARD.length) {
            String sampleName = Sampler.STANDARD[idx];
            try {
                if (value > 64)
                    sampler.playByName(sampleName, true);
                else if (value == 0)
                    sampler.playByName(sampleName, false);
            } catch (Exception e) {
                RTLogger.warn(this, "SampleTrack play failed: " + e.getMessage());
            }
            return; // consume CC for this mapping
        }
    }
    super.send(midi, ticker); // fallback to default handling
}

@Override public boolean capture(judahzone.api.Midi midi) { return false; }  // oh, this can be nice. (automation)
@Override protected void processNote(ShortMessage m) { /* not used */ }
@Override protected void parse(javax.sound.midi.Track incoming) { /* no-op for CC-driven track */ } // TODO
@Override public String[] getPatches() { return new String[0]; }
@Override public String progChange(int data1) { return null; }
@Override public boolean progChange(String name) { return false; }
@Override public net.judah.channel.Channel getChannel() { return sampler; }

}