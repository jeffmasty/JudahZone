package net.judah.controllers;

import static net.judah.JudahZone.*;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.drumz.DrumMachine;
import net.judah.effects.Fader;
import net.judah.effects.LFO.Target;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;

public class Beatstep implements Controller {
	
	public static final String NAME = "Arturia BeatStep";
	// stop = progchange 127
	// play = progchange 126

	public static float TO_100 = 0.7874f; // 127/100

	int[] KNOBS = new int[] {
			10, 74, 71, 76, 77, 93, 73, 75, 
			114, 18, 19, 16, 17, 91, 79, 72 };
	
	int[] PADS = new int[] {
			44, 45, 46, 47, 48, 49, 50, 51,
			36, 37, 38, 39, 40, 41, 42, 43 };

	int LEVEL_KNOB = 7; // CC

	private long tempoLookedUp;
	private int tempo;
	
	@Override
	public boolean midiProcessed(Midi midi) throws JackException {

		if (midi.getChannel() != 0) { // TODO
			if (Midi.isNoteOn(midi) && midi.getChannel() == 8) 
				JudahZone.getFluid().send(Midi.create(midi.getCommand(), 9, midi.getData1(), midi.getData2()), JudahMidi.ticker());
			else if (midi.getCommand() != 240)
				RTLogger.log(this, midi.toString());
			return true;
		}

		int data1 = midi.getData1();
		int data2 = midi.getData2();

		
		
		if (Midi.isCC(midi)) {
			if (data1 == LEVEL_KNOB) {
				if (System.currentTimeMillis() - tempoLookedUp > 5000) // stale
					tempo = (int)JudahZone.getClock().getTempo();
				tempo += (data2 == 127 ? -2 : 2);
				JudahZone.getClock().writeTempo(tempo);
				tempoLookedUp = System.currentTimeMillis();
				return true;
			}

			int idx = indexOf(data1, KNOBS);
			if (idx >= 0) 
				getFxRack().getCurrent().knob(idx, data2 < 64);
			
		}
		else if (Midi.isNoteOn(midi)) {

			if (midi.getChannel() == 8) { // beat step live sequencer
				midi = Midi.create(midi.getCommand(), 9, midi.getData1(), midi.getData2());
				JackMidi.eventWrite(JudahZone.getFluid().getMidiPort().getPort(), JudahMidi.ticker(), midi.getMessage(), midi.getLength());
				RTLogger.log(this, "" + midi);
				return true;
			}

			int idx = indexOf(data1, PADS);
			if (idx < 0)
				return true;
			
			Channel ch = null;
			if (idx < 7)
				ch = getNoizeMakers().get(idx);
			else if (idx == 7)
				ch = getFxRack().getChannel() == getMains() ? getPiano() : getMains();
			else if (idx > 7 && idx < 12)
				ch = getLooper().get(idx - 8);
			else if (idx == 12) { // drums1/drums2
				DrumMachine drums = getDrumMachine();
				ch = getFxRack().getChannel() == drums.getDrum1() ? drums.getDrum2() : drums.getDrum1();
			}
			else if (idx == 13) { // hihat / fills					
				DrumMachine drums = getDrumMachine();
				ch = getFxRack().getChannel() == drums.getHats() ? drums.getFills() : drums.getHats();
			}
			else if (idx == 14) { // FX
				getFxRack().getChannel().toggleFx();
				return true;
			}
			else if (idx == 15) { // Fade out					
				ch = getFxRack().getChannel();
				if (ch.isOnMute() || ch.getGain().getVol() < 5) {
					ch.setOnMute(false);
					Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, 0, 51));
				}
				else 
					Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, ch.getVolume(), 0));
				return true;
			}
			MainFrame.setFocus(ch);
		}
		return true;
	}

	private int indexOf(int data1, int[] array) {
		for (int i = 0; i < array.length; i++)
			if (data1 == array[i])
				return i;
		return -1;
	}

// FADE OUT
//			Channel ch = getFxRack().getChannel();
//			if (ch == null) return true; // ? 
//			if (ch.isOnMute() || ch.getGain().getVol() < 5) {
//				ch.setOnMute(false);
//				Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, 0, 51));
//			}
//			else 
//				Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, ch.getVolume(), 0));
//			break;

	
}
