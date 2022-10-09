package net.judah.controllers;

import static net.judah.JudahZone.*;
import static net.judah.controllers.KnobMode.*;

import javax.swing.JLabel;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.api.MidiReceiver;
import net.judah.drumz.DrumType;
import net.judah.drumz.KitView;
import net.judah.effects.Delay;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.ProgChange;
import net.judah.mixer.Channel;
import net.judah.samples.Sample;
import net.judah.synth.SynthEngines;
import net.judah.tracker.Tracker;
import net.judah.tracker.Transpose;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

/** Akai MPKmini, not the new one */
@RequiredArgsConstructor
public class MPKmini implements Controller, MPKTools, Pastels {
	
	@Getter private static KnobMode mode = Clock;
	@Getter private static JLabel label = new JLabel(mode.name(), JLabel.LEFT);
	public static final int MIDDLE_C = 60;
	private static final int JOYSTICK_L = 127;
	private static final int JOYSTICK_R = 0;

	private final JudahMidi sys;
	private final Midi NO_MODULATION = Midi.create(Midi.CONTROL_CHANGE, 0, 1, 0);
	private final byte[] SUSTAIN_ON = Midi.create(Midi.CONTROL_CHANGE, 0, 64, 127).getMessage();
	@SuppressWarnings("unused")
	private final byte[] SUSTAIN_OFF= Midi.create(Midi.CONTROL_CHANGE, 0, 64, 0).getMessage();
	@SuppressWarnings("unused")
	private final int SUSTAIN_LENGTH = SUSTAIN_ON.length;
	
	public static void setMode(KnobMode knobs) {
		mode = knobs;
		MainFrame.setFocus(knobs);
		// MainFrame.update(JudahZone.getTimeSig());
	}
	
	@Override
	public boolean midiProcessed(Midi midi) throws JackException {
		Tracker tracker = getTracker();
		if (Midi.isCC(midi)) {
			if (midi.getData1() == JudahClock.TEMPO_CC && mode== Clock) {
				try { // Send Tempo adjust to external clock
					JackMidi.eventWrite(sys.getTempo(), JudahMidi.ticker(), midi.getMessage(), midi.getLength());
				} catch (Exception e) {
					RTLogger.warn(this, e);
				}
				return true;
			} else
				return checkCC(midi.getData1(), midi.getData2());
		}
		if (Midi.isProgChange(midi)) 
			return doProgChange(midi.getData1(), midi.getData2());
		
		if (midi.getChannel() == 9) {
			int data1 = midi.getData1();
			
			for (int i = 0; i < DRUMS_A.size() ; i++)
				if (DRUMS_A.get(i) == data1) {
					int translate = getDrumMachine().getDrum1().getSamples()[i].getGmDrum().getData1();
					if (tracker.isRecord()) {
						getBeats().record(midi);
					}
					else 
						getDrumMachine().getDrum1().send(
							Midi.create(midi.getCommand(), 9, translate, midi.getData2()), -1);
					return true;
				}
			
			for (int i = 0; i < DRUMS_B.size() ; i++)
				if (DRUMS_B.get(i) == data1) {
					midi = Midi.create(midi.getCommand(), 9, 
							DrumType.values()[i].getDat().getData1(), midi.getData2());
					if (tracker.isRecord()) {
						getBeats().record(midi);
					}
					else {
						getBeats().getDrum2().getMidiOut().send(midi, JudahMidi.ticker());
					}
				}
			return false;
		}
		if (tracker.isRecord() && 
				(midi.getCommand() == Midi.NOTE_ON || midi.getCommand() == Midi.NOTE_OFF)) {
			getNotes().record(midi);
			return false; // pass through?
		}
		
		if (Transpose.isActive() && midi.getCommand() == Midi.NOTE_ON) {
			Transpose.setAmount(midi.getData1() - MIDDLE_C);
			return true; // key press consumed
		}
		return false; 
	}

	private boolean checkCC(int data1, int data2) throws JackException {
		if (KNOBS.contains(data1)) 
			return doKnob(data1, data2);
		if (data1 == JOYSTICK_L)
			return joystickL(data2);
		if (data1 == JOYSTICK_R)
			return joystickR(data2);
		if (PRIMARY_CC.contains(data1)) 
			return cc_pad(data1, data2);
		else if (SAMPLES_CC.contains(data1)) {
			Sample s = getSampler().get(SAMPLES_CC.indexOf(data1));
			getSampler().play(s, data2 > 0);
			return true;
		}
		return false;
	}

	private boolean doKnob(int data1, int data2) {
		switch(mode) {
			case FX1:
				getFxRack().getCurrent().effects1(data1, data2);
				return true;
			case FX2:
				getFxRack().getCurrent().effects2(data1, data2);
				return true;
			case Clock:
				getMidiGui().clockKnobs(data1 - KNOBS.get(0), data2);
				return true;
			case Track:
				getTracker().knob(data1 - KNOBS.get(0), data2);
				return true;
			case Synth: 
				SynthEngines.getCurrent().synthKnobs(data1 - KNOBS.get(0), data2);
				return true;
			case Kit:
				KitView.getCurrent().knob(data1 - KNOBS.get(0), data2);
				return true;
		}
		return false;
	}
	

	private boolean cc_pad(int data1, int data2) throws JackException {
		///////// ROW 1 /////////////////
		if (data1 == PRIMARY_CC.get(0))  { // Jamstik
			getJamstik().toggle();
		}
		else if (data1 == PRIMARY_CC.get(1)) { // sync Crave's internal sequencer
			sys.synchronize(getCrave());
		}
		else if (data1 == PRIMARY_CC.get(2) && data2 > 0) // Clock or Tracks
			if (MPKmini.getMode() == Clock)
				MPKmini.setMode(KnobMode.Track);
			else 
				MPKmini.setMode(Clock); 
		else if (data1 == PRIMARY_CC.get(3) && data2 > 0) // EFX . . . []  
			if (MPKmini.getMode() == FX1)
				MPKmini.setMode(FX2);
			else 
				MPKmini.setMode(FX1);
		
		///////// ROW 2 /////////////////
		else if (data1 == PRIMARY_CC.get(4)) { 
			Transpose.toggle();
			//// not drums			
			//			tracker.getCurrent().setLatch(!tracker.getCurrent().isLatch());
			//			Tracker.checkLatch();
			//			return true;
		}
		else if (data1 == PRIMARY_CC.get(5)) { 
			// record toggle
		}
		else if (data1 == PRIMARY_CC.get(6) && data2 > 0) { // focus fluid/synth2
			Channel current = JudahZone.getFxRack().getChannel(); 
			if (current == JudahZone.getFluid())
				MainFrame.setFocus(JudahZone.getSynth2());
			else MainFrame.setFocus(JudahZone.getFluid());
		}
		else if (data1 == PRIMARY_CC.get(7) && data2 > 0) { // focus mains/crave
			Channel current = JudahZone.getFxRack().getChannel(); 
			if (current == JudahZone.getMains())
				MainFrame.setFocus(JudahZone.getCrave());
			else MainFrame.setFocus(JudahZone.getMains());
		} 
		else 
			return false;
		return true;
	}

	private boolean joystickL(int data2) throws JackException {
		Delay d = getFxRack().getChannel().getDelay();
		d.setActive(data2 > 4);
		if (data2 <= 4) 
			return true;
		if (d.getDelay() < Delay.DEFAULT_TIME) 
			d.setDelay(Delay.DEFAULT_TIME);
		// data2 / 127 = 0 to max delay
		d.setFeedback(data2 * 0.00787f);
		return true;
	}
	
	private boolean joystickR(int data2) throws JackException {
		if (data2 > 4) {
			Midi modWheel = Midi.create(Midi.CONTROL_CHANGE, 0, 1, data2);
			sys.getKeyboardSynth().send(modWheel, JudahMidi.ticker());
		}
		else
			sys.getKeyboardSynth().send(NO_MODULATION, JudahMidi.ticker());
		return true;
	}
	
	
	private boolean doProgChange(int data1, int data2) {

			// ProgChange pads
            //  upInst   upDrum   upSheet   upSong
            // downInst downDrum downSheet downSong

			MidiReceiver fluid = getFluid();
            
            if (data1 == PRIMARY_PROG[0]) // up fluid inst patch
            	ProgChange.next(true, fluid, 9);
            else if (data1 == PRIMARY_PROG[1]) { // up gm drum patch
            	ProgChange.next(true, fluid, 9);
            } 
            else if (data1 == PRIMARY_PROG[2]) { // up sheetMusic
            	getFrame().sheetMusic(true);
            } else if (data1 == PRIMARY_PROG[3]) { // up song
            	nextSong();
            } 
            
            else if (data1 == PRIMARY_PROG[4]) { // down fluid inst patch
                ProgChange.next(false, fluid, 0);
            } else if (data1 == PRIMARY_PROG[5]) { // down gm drum patch
            	ProgChange.next(false, fluid, 0);
            } else if (data1 == PRIMARY_PROG[6]) { // down sheet music
            	getFrame().sheetMusic(false);
            } else if (data1 == PRIMARY_PROG[7]) { // reset stage
            	getMidiGui().getSetlistCombo().setSelectedItem(0);
            	loadSong();
            }

            
            // B BANK 
            else if (data1 == B_PROG[0]) // I want bass
            	ProgChange.progChange(32, fluid, 0);
            else if (data1 == B_PROG[1]) { // sitar
            	ProgChange.progChange(104, fluid, 0);
            } else if (data1 == B_PROG[2]) { // harp
            	ProgChange.progChange(46, fluid, 0);
            } else if (data1 == B_PROG[3]) { // elec piano
            	ProgChange.progChange(4, fluid, 0);
            }

            else if (data1 == B_PROG[4]) { // strings
            	ProgChange.progChange(44, fluid, 0);
            } else if (data1 == B_PROG[5]) { // vibraphone
            	ProgChange.progChange(11, fluid, 0);
            } else if (data1 == B_PROG[6]) // rock organ
            	ProgChange.progChange(18, fluid, 0);
            else if (data1 == B_PROG[7]) { // honky tonk piano
            	ProgChange.progChange(3, fluid, 0);
            } else 
            	return false;
            
		return true;
	}

		
}

/*
	private boolean doDrumPad(ShortMessage midi)  {
		if (false == ( (midi.getCommand() == Midi.NOTE_ON || midi.getCommand() == Midi.NOTE_OFF) 
				&& (DRUMS_A.contains(midi.getData1()) || DRUMS_B.contains(midi.getData1()))))
			return false;
		JackPort out = JudahClock.getTracks()[0].getMidiOut();
		int data1 = midi.getData1();
		try {
			if (data1 == DRUMS_A.get(0)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.BassDrum.getData1(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} else if (data1 == DRUMS_A.get(1)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.AcousticSnare.getData1(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(2)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.Claves.getData1(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
				
			else if (data1 == DRUMS_A.get(3)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.ChineseCymbal.getData1(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(4)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.ClosedHiHat.getData1(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(5)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.OpenHiHat.getData1(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(6)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.Shaker.getData1(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(7)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.LowMidTom.getData1(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
		return true;
	}
*/