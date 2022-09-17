package net.judah.controllers;

import static net.judah.controllers.KnobMode.*;

import javax.swing.JLabel;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.effects.Delay;
import net.judah.effects.gui.ControlPanel;
import net.judah.effects.gui.EffectsRack;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.ProgChange;
import net.judah.tracker.DrumTrack;
import net.judah.tracker.Track;
import net.judah.tracker.Transpose;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;
import net.judah.util.SettableCombo;

@RequiredArgsConstructor
public class MPK implements Controller, MPKTools, Pastels {
	
	@Getter private static KnobMode mode = Clock;
	@Getter private static JLabel label = new JLabel(mode.name(), JLabel.LEFT);
	public static final int MIDDLE_C = 60;
	private static final int JOYSTICK_L = 127;
	private static final int JOYSTICK_R = 0;

	private final JudahMidi sys;
	private final byte[] NO_MODULATION = Midi.create(Midi.CONTROL_CHANGE, 0, 1, 0).getMessage();
	private final int NO_MOD_LENGTH = NO_MODULATION.length;
	private final byte[] SUSTAIN_ON = Midi.create(Midi.CONTROL_CHANGE, 0, 64, 127).getMessage();
	@SuppressWarnings("unused")
	private final byte[] SUSTAIN_OFF= Midi.create(Midi.CONTROL_CHANGE, 0, 64, 0).getMessage();
	@SuppressWarnings("unused")
	private final int SUSTAIN_LENGTH = SUSTAIN_ON.length;
	
	public static void setMode(KnobMode knobs) {
		mode = knobs;
		MainFrame.setFocus(knobs);
	}
	
	@Override
	public boolean midiProcessed(Midi midi) throws JackException {
		if (Midi.isCC(midi)) {
			if (midi.getData1() == JudahClock.TEMPO_CC && mode== Clock) {
				try { // Send Tempo adjust to external clock
					JackMidi.eventWrite(sys.getTempo(), sys.ticker(), midi.getMessage(), midi.getLength());
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
			Track t = JudahClock.getTracker().getDrum1();
			for (int i = 0; i < DRUMS_A.size() ; i++)
				if (DRUMS_A.get(i) == data1) {
					midi = Midi.create(midi.getCommand(), 9, ((DrumTrack)t).getKit().get(i).getData1(), midi.getData2());
					if (JudahClock.getTracker().isRecord()) {
						JudahClock.getTracker().record(midi);
					}
					else {
						JackMidi.eventWrite(JudahClock.getTracker().getDrum1().getMidiOut(), 9, midi.getMessage(), midi.getLength());
					}
				}
			return false;
		}
		if (JudahClock.getTracker().isRecord() && 
				(midi.getCommand() == Midi.NOTE_ON || midi.getCommand() == Midi.NOTE_OFF)) {
			JudahClock.getTracker().record(midi);
			return false; // pass through?
		}
		
		// TODO update MidiGui MPK drop down
		if (midi.isNote() && JudahZone.getSynth().isMPK()) {
			JudahZone.getSynth().midiProcessed(midi);
			return true;
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
			JudahZone.getSampler().play(SAMPLES_CC.indexOf(data1), data2 > 0);
			return true;
		}
		return false;
	}


	private boolean cc_pad(int data1, int data2) throws JackException {
		///////// ROW 1 /////////////////
		if (data1 == PRIMARY_CC.get(0))  { // TODO
		
		}
		else if (data1 == PRIMARY_CC.get(1)) { // sync Crave's internal sequencer
			sys.synchronize(sys.getCraveOut());
		}
		else if (data1 == PRIMARY_CC.get(2) && data2 > 0) // Clock or Tracks
			if (MPK.getMode() == Clock)
				MPK.setMode(KnobMode.Track);
			else 
				MPK.setMode(Clock); 
		else if (data1 == PRIMARY_CC.get(3) && data2 > 0) // EFX . . . []  
			if (MPK.getMode() == FX1)
				MPK.setMode(FX2);
			else 
				MPK.setMode(FX1);
		
		///////// ROW 2 /////////////////

		else if (data1 == PRIMARY_CC.get(4)) { 
			JudahZone.getSynth().setMPK(!JudahZone.getSynth().isMPK());
			// Transpose.toggle();
		}
		else if (data1 == PRIMARY_CC.get(5)) { // Jamstik midi on/off 
			Jamstik.toggle();
		}
		else if (data1 == PRIMARY_CC.get(6) && data2 > 0) { // FX on/off // TODO focus samples?
			EffectsRack current = ControlPanel.getInstance().getCurrent();
			if (current != null) {
				current.getChannel().setPresetActive(!current.getChannel().isPresetActive());
				MainFrame.updateCurrent();
			}
		}
		else if (data1 == PRIMARY_CC.get(7) && data2 > 0) { // Turn on/off a Preset efx // TODO
			SettableCombo.set();
		} 
		else 
			return false;
		return true;
	}

	
	private boolean doKnob(int data1, int data2) {
		switch(mode) {
			case FX1:
				ControlPanel.getInstance().getCurrent().effects1(data1, data2);
				return true;
			case FX2:
				ControlPanel.getInstance().getChannel().getGui().effects2(data1, data2);
				return true;
			case Clock:
				sys.getGui().clockKnobs(data1 - KNOBS.get(0), data2);
				return true;
			case Track:
				trackKnobs(data1, data2);
		}
		return false;
	}
	
	private void trackKnobs(int data1, int data2) {
		for (int i = 0; i < MPKTools.KNOBS.size(); i++) {
			if (data1 == MPKTools.KNOBS.get(i))
				JudahClock.getTracker().knob(i, data2);
		}
	}
	
	private boolean joystickL(int data2) throws JackException {
		Delay d = sys.getPath(sys.getKeyboardSynth()).getChannel().getDelay();
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
			byte[] modWheel = Midi.create(Midi.CONTROL_CHANGE, 0, 1, data2).getMessage();
			JackMidi.eventWrite(sys.getKeyboardSynth(), sys.ticker(), modWheel, modWheel.length);
		}
		else
			JackMidi.eventWrite(sys.getKeyboardSynth(), sys.ticker(), NO_MODULATION, NO_MOD_LENGTH);
		return true;
	}
	
	
	private boolean doProgChange(int data1, int data2) {

			// ProgChange pads
            //  upInst   upDrum   upSheet   upSong
            // downInst downDrum downSheet downSong

            JackPort fluidOut = JudahMidi.getInstance().getFluidOut();
            JackPort calfOut = JudahMidi.getInstance().getCalfOut();
            
            if (data1 == PRIMARY_PROG[0]) // up fluid inst patch
            	ProgChange.next(true, fluidOut, 0);
            else if (data1 == PRIMARY_PROG[1]) { // up calf drum patch
            	ProgChange.next(true, calfOut, 9);
            } else if (data1 == PRIMARY_PROG[2]) { // up sheetMusic
            	MainFrame.get().sheetMusic(true);
            } else if (data1 == PRIMARY_PROG[3]) { // up song
            	JudahZone.nextSong();
            } 
            
            else if (data1 == PRIMARY_PROG[4]) { // down fluid inst patch
                ProgChange.next(false, fluidOut, 0);
            } else if (data1 == PRIMARY_PROG[5]) { // down calf drum patch
            	ProgChange.next(false, calfOut, 9);
            } else if (data1 == PRIMARY_PROG[6]) { // down sheet music
            	MainFrame.get().sheetMusic(false);
            } else if (data1 == PRIMARY_PROG[7]) { // reset stage
            	JudahMidi.getInstance().getGui().getSetlist().setSelectedItem(0);
            	JudahZone.loadSong();
            }

            
            // B BANK 
            else if (data1 == B_PROG[0]) // I want bass
            	ProgChange.progChange(32, fluidOut, 0);
            else if (data1 == B_PROG[1]) { // sitar
            	ProgChange.progChange(104, fluidOut, 0);
            } else if (data1 == B_PROG[2]) { // harp
            	ProgChange.progChange(46, fluidOut, 0);
            } else if (data1 == B_PROG[3]) { // elec piano
            	ProgChange.progChange(4, fluidOut, 0);
            }

            else if (data1 == B_PROG[4]) { // strings
            	ProgChange.progChange(44, fluidOut, 0);
            } else if (data1 == B_PROG[5]) { // vibraphone
            	ProgChange.progChange(11, fluidOut, 0);
            } else if (data1 == B_PROG[6]) // rock organ
            	ProgChange.progChange(18, fluidOut, 0);
            else if (data1 == B_PROG[7]) { // honky tonk piano
            	ProgChange.progChange(3, fluidOut, 0);
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