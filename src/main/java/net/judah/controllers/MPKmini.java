package net.judah.controllers;

import static net.judah.JudahZone.*;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.Sample;
import net.judah.fx.Delay;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.knobs.KnobMode;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.seq.Seq;
import net.judah.seq.arp.Mode;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Akai MPKmini, not the new one */
@RequiredArgsConstructor
public class MPKmini implements Controller, MPKTools, Pastels {
	
	private static final int JOYSTICK_L = 127;
	private static final int JOYSTICK_R = 0;

	private final JudahMidi sys;
	private final Seq seq;
	
	private final byte[] SUSTAIN_ON = Midi.create(Midi.CONTROL_CHANGE, 0, 64, 127).getMessage();
	@SuppressWarnings("unused")
	private final byte[] SUSTAIN_OFF= Midi.create(Midi.CONTROL_CHANGE, 0, 64, 0).getMessage();
	@SuppressWarnings("unused")
	private final int SUSTAIN_LENGTH = SUSTAIN_ON.length;
	
	@Override
	public boolean midiProcessed(Midi midi) {

		if (Midi.isCC(midi)) {
			return checkCC(midi.getData1(), midi.getData2());
		}
		if (Midi.isProgChange(midi)) 
			return doProgChange(midi.getData1(), midi.getData2());

		if (midi.getChannel() == 9) {
			int data1 = midi.getData1();
			for (int i = 0; i < DRUMS_A.size() ; i++)
				if (DRUMS_A.get(i) == data1) {
					int translate = getDrumMachine().getDrum1().getSamples()[i].getGmDrum().getData1();
					try {
						midi.setMessage(midi.getCommand(), 9, translate, midi.getData2());
					} catch (Exception e) { RTLogger.warn(this, e);}
				}
			
			for (int i = 0; i < DRUMS_B.size() ; i++)
				if (DRUMS_B.get(i) == data1) {
					try {
						midi.setMessage(midi.getCommand(), 9, DrumType.values()[i].getData1(), midi.getData2());
					} catch (Exception e) { RTLogger.warn(this, e);}
				}
		}
		
		if (seq.rtCheck(midi)) 
			return true; // track will send

		return false;
	}

	private boolean checkCC(int data1, int data2) {
		if (KNOBS.contains(data1)) {
			MainFrame.update(new KnobData(data1 - KNOBS.get(0), data2));
			return false;
		} 
		if (data1 == JOYSTICK_L)
			return joystickL(data2);
		if (data1 == JOYSTICK_R)
			return joystickR(data2);
		if (PRIMARY_CC.contains(data1)) 
			return cc_pad(data1, data2);
		if (SAMPLES_CC.contains(data1)) {
			Sample s = getSampler().get(SAMPLES_CC.indexOf(data1));
			getSampler().play(s, data2 > 0);
			return true;
		}
		return false;
	}

	private boolean cc_pad(int data1, int data2) {
		///////// ROW 1 /////////////////
		if (data1 == PRIMARY_CC.get(0))  { // Chords, formerly Jamstik
			getChords().toggle();
		}
		else if (data1 == PRIMARY_CC.get(1)) { // sync Crave's internal sequencer
			sys.synchronize(getCrave());
		}
		else if (data1 == PRIMARY_CC.get(2) && data2 > 0) // focus MidiGui or LFO
			if (MainFrame.getKnobMode() == KnobMode.Setlist) 
				MainFrame.setFocus(KnobMode.LFO);
			else if (MainFrame.getKnobMode() == KnobMode.Midi)
				MainFrame.setFocus(KnobMode.Setlist);
			else 
				MainFrame.setFocus(KnobMode.Midi); 
		else if (data1 == PRIMARY_CC.get(3) && data2 > 0) {// focus TRACKS
			if (MainFrame.getKnobMode() == KnobMode.Track)
				getSeq().getTracks().next(true);
			else 
				MainFrame.setFocus(KnobMode.Track);
		}
			
		///////// ROW 2 /////////////////
		else if (data1 == PRIMARY_CC.get(4)) { 
			sys.getKeyboardSynth().getArp().toggle(Mode.MPK);
		}
		else if (data1 == PRIMARY_CC.get(5)) {
			sys.getKeyboardSynth().getArp().toggle(Mode.REC);
		}
		else if (data1 == PRIMARY_CC.get(6) && data2 > 0) { // focus Synth1 or Synth2 or Sampler
			if (MainFrame.getKnobMode() == KnobMode.DCO) {
				if (JudahZone.getFrame().getKnobs() == JudahZone.getSynth1().getSynthKnobs()) 
					MainFrame.setFocus(JudahZone.getSynth2().getSynthKnobs());
				else 
					MainFrame.setFocus(KnobMode.Samples);
				}
			else 
				MainFrame.setFocus(KnobMode.DCO);
		}
		else if (data1 == PRIMARY_CC.get(7) && data2 > 0) { // SET SettableCombo
			MainFrame.set();
		} 
		else 
			return false;
		return true;
	}

	private boolean joystickL(int data2) { // delay
		Delay d = ((Channel)sys.getKeyboardSynth().getMidiOut()).getDelay();
		d.setActive(data2 > 4);
		if (data2 <= 4) 
			return true;
		if (d.getDelay() < Delay.DEFAULT_TIME) 
			d.setDelayTime(Delay.DEFAULT_TIME);
		d.setFeedback(Constants.midiToFloat(data2));
		MainFrame.update(sys.getKeyboardSynth().getMidiOut());
		return true;
	}
	
	private boolean joystickR(int data2) { // filter
		MainFrame.update(sys.getKeyboardSynth().getMidiOut());
		int ch = sys.getKeyboardSynth().getCh();
		sys.getKeyboardSynth().getMidiOut().send( Midi.create(Midi.CONTROL_CHANGE, ch, 1, 
				data2 > 4 ? data2 : 0), JudahMidi.ticker());  
		
		return true;
	}
	
	private boolean doProgChange(int data1, int data2) {
		MidiReceiver fluid = getFluid();
        //  upInst   upDrum   upSheet   upSong
        // downInst downDrum downSheet downSong
		if (data1 == PRIMARY_PROG[0]) {// up fluid inst patch
//            	ProgChange.next(true, fluid, 9);
//            else if (data1 == PRIMARY_PROG[1]) { // up gm drum patch
//            	ProgChange.next(true, fluid, 9);
		} 
		else if (data1 == PRIMARY_PROG[2]) { // up sheetMusic
//            	getFrame().sheetMusic(true);
//            } else if (data1 == PRIMARY_PROG[3]) { // up song
//            	nextSong();
		} 
		else if (data1 == PRIMARY_PROG[4]) { // down fluid inst patch
//                ProgChange.next(false, fluid, 0);
//            } else if (data1 == PRIMARY_PROG[5]) { // down gm drum patch
//            	ProgChange.next(false, fluid, 0);
		} else if (data1 == PRIMARY_PROG[6]) { // down sheet music
			getFrame().sheetMusic(false);
		} else if (data1 == PRIMARY_PROG[7]) { // reset stage
        	getMidiGui().getSongsCombo().setSelectedItem(0);
        	loadSong(getSong().getFile());
        }

        // B BANK 
        else if (data1 == B_PROG[0]) // I want bass
        	fluid.progChange("Acoustic Bass");
        else if (data1 == B_PROG[1]) { 
        	fluid.progChange("Sitar");
        } else if (data1 == B_PROG[2]) { 
        	fluid.progChange("Harp");
        } else if (data1 == B_PROG[3]) { 
        	fluid.progChange("Rhodes EP");
        }
        else if (data1 == B_PROG[4]) { // strings
        	fluid.progChange("Tremolo");
        } else if (data1 == B_PROG[5]) { 
        	fluid.progChange("Vibraphone");
        } else if (data1 == B_PROG[6]) 
        	fluid.progChange("Rock Organ");
        else if (data1 == B_PROG[7]) { 
        	fluid.progChange("Honky Tonk");
        } else 
        	return false;
	return true;
	}
		
}

