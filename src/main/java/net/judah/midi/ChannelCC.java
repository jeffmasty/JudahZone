package net.judah.midi;

import static net.judah.fx.Chorus.Settings.*;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.api.Midi;
import net.judah.fx.Chorus;
import net.judah.fx.Delay;
import net.judah.fx.Filter;
import net.judah.fx.Gain;
import net.judah.fx.LFO;
import net.judah.fx.MonoFilter;
import net.judah.fx.Reverb;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.seq.MidiConstants;
import net.judah.seq.automation.ControlChange;
import net.judah.util.Constants;

@RequiredArgsConstructor  // TacoSynthTruck, DrumKit, MidiInstrument
public class ChannelCC {

	private final Channel ch;
	@Getter @Setter private boolean active = true;

	public boolean process(ShortMessage msg) {
			if (!active) return false;
			if (!Midi.isCC(msg))return false;
			ControlChange type = ControlChange.find(msg.getData1());
			if (type == null)
				return false;
			int val = (int) (msg.getData2() * Constants.TO_100);
			LFO lfo = ch.getLfo2();
			Filter filter = ch.getLoCut();

			switch(type) {
			case BALANCE:
			case PAN:
				ch.getGain().set(Gain.PAN, val);
				break;
			case BRIGHT:
	        	filter.set(MonoFilter.Settings.Resonance.ordinal(), val);
	        	ch.setActive(filter, val < MidiConstants.THOLD_HI);
				break;
			case CHORUS:
				ch.getChorus().set(Chorus.Settings.Feedback.ordinal(), val);

				ch.setActive(ch.getChorus(), val > MidiConstants.THOLD_LO);
				break;
			case ECHO:
				ch.getDelay().set(Delay.Settings.DelayTime.ordinal(), val);
				ch.setActive(ch.getDelay(), val > MidiConstants.THOLD_LO);
				break;
			case ECHO_FB:
				ch.getDelay().set(Delay.Settings.Feedback.ordinal(), val);
				ch.setActive(ch.getDelay(), val > MidiConstants.THOLD_LO);
				break;
			case DEPTH:
	        	ch.getChorus().set(Chorus.Settings.Depth.ordinal(), val);
	        	ch.setActive(ch.getChorus(), val > MidiConstants.THOLD_LO);
				break;
			case DRIVE:
				ch.getOverdrive().set(0, val);
				ch.setActive(ch.getOverdrive(), val > MidiConstants.THOLD_LO);
				break;
			case HZ:
	        	filter.set(MonoFilter.Settings.Frequency.ordinal(), val);
	        	ch.setActive(filter, val < MidiConstants.THOLD_HI);
				break;
			case LFO:
				lfo.set(LFO.Settings.MSec.ordinal(), val);
				break;
			case LOCUT:
				filter.set(Filter.Settings.Type.ordinal(), val);
				break;
			case PHASER:
				phaser(); // chorus preset
				lfo.phaser(val);
				break;
			case RESET:
				ch.reset();
				break;
			case REVERB:
				ch.getReverb().set(Reverb.Settings.Wet.ordinal(), val);
				ch.setActive(ch.getReverb(), val > 0);
				break;
			case ROOM:
				ch.getReverb().set(Reverb.Settings.Room.ordinal(), val);
				break;
			case RATE:
	        	ch.getChorus().set(Chorus.Settings.Rate.ordinal(), val);
	        	ch.setActive(ch.getChorus(), val < MidiConstants.THOLD_HI);
				break;
			case TREMELO:
				lfo.tremelo(val);
				break;
			case ZIP:
				ch.setActive(ch.getCompression(), val > 49);
				break;
			default: return false;
			}

			MainFrame.update(ch);
		return true;
	}

	private void phaser() {
		// Set a Chorus Preset (10/64/64/0/1/36),
		// let LFO alter the Phase
		Chorus choir = ch.getChorus();
		choir.set(Rate.ordinal(), 10);
		choir.set(Depth.ordinal(), 64);
		choir.set(Feedback.ordinal(), 64);
		choir.set(Type.ordinal(), 0);
		choir.set(Sync.ordinal(), 1);
		ch.setActive(choir, true);
	}

}
