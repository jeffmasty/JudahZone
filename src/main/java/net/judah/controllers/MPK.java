package net.judah.controllers;

import static net.judah.JudahZone.getSynth;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.ControlPanel;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.effects.Chorus;
import net.judah.effects.CutFilter;
import net.judah.effects.CutFilter.Type;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.api.Preset;
import net.judah.effects.api.Reverb;
import net.judah.effects.gui.EffectsRack;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.tracker.GMDrum;
import net.judah.tracker.todo.Transpose;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public class MPK implements Controller, MPKTools {
	
	@Getter private static KnobMode mode = KnobMode.Clock;
	public static final int MIDDLE_C = 60;
	public static final int thresholdLo = 1;
	public static final int thresholdHi = 98;
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
	private final Transpose transpose = new Transpose(); // TODO
	
	public static void setMode(KnobMode knobs) {
		mode = knobs;
		MainFrame.setFocus(knobs);
	}
	
	@Override
	public boolean midiProcessed(Midi midi) throws JackException {
		if (Midi.isCC(midi)) 
			return checkCC(midi.getData1(), midi.getData2());
		if (Midi.isProgChange(midi)) 
			return doProgChange(midi.getData1(), midi.getData2());
		if (midi.getChannel() == 9) 
			return doDrumPad(midi);
		if (Transpose.isActive() && midi.getCommand() == Midi.NOTE_ON) {
			Transpose.setAmount(midi.getData2() - MIDDLE_C);
			return true; // key press consumed
		}
			
		
		/*
		 * keys -> synth
		 * 
		 * 
		 * transpose
		 * keys -> sequencer -> synth
		 * 
		 * 
		 */
		
		
		return false;
	}

	private boolean doDrumPad(ShortMessage midi)  {
		if (false == ( (midi.getCommand() == Midi.NOTE_ON || midi.getCommand() == Midi.NOTE_OFF) 
				&& (DRUMS_A.contains(midi.getData1()) || DRUMS_B.contains(midi.getData1()))))
			return false;
		JackPort out = JudahClock.getInstance().getTracks()[0].getMidiOut();
		int data1 = midi.getData1();
		try {
			if (data1 == DRUMS_A.get(0)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.BassDrum.getMidi(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} else if (data1 == DRUMS_A.get(1)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.AcousticSnare.getMidi(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(2)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.Claves.getMidi(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
				
			else if (data1 == DRUMS_A.get(3)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.ChineseCymbal.getMidi(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(4)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.ClosedHiHat.getMidi(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(5)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.OpenHiHat.getMidi(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(6)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.Shaker.getMidi(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
			else if (data1 == DRUMS_A.get(7)) {
				midi.setMessage(midi.getCommand(), midi.getChannel(), GMDrum.LowMidTom.getMidi(), midi.getData2());
				JackMidi.eventWrite(out, 0, midi.getMessage(), midi.getLength());
			} 
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
		return true;
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
		return false;
	}

	private boolean cc_pad(int data1, int data2) throws JackException {
		///////// ROW 1 /////////////////
		if (data1 == PRIMARY_CC.get(0))  { // sync Crave
			JackPort port = sys.getCraveOut();
			ArrayList<JackPort> sync = sys.getSync();
			if (sync.contains(port))
				sync.remove(port);
			else
				sync.add(port);
			MainFrame.update(JudahZone.getChannels().getCrave());
		}
		else if (data1 == PRIMARY_CC.get(1)) { // sync Uno
//			JackPort port = sys.getUnoOut();
//			ArrayList<JackPort> sync = sys.getSync();
//			if (sync.contains(port))
//				sync.remove(port);
//			else
//				sync.add(port);
//			MainFrame.update(JudahZone.getChannels().getUno());
		}
		else if (data1 == PRIMARY_CC.get(2) && data2 > 0) // Clock or Tracks
			if (MPK.getMode() == KnobMode.Clock)
				MPK.setMode(KnobMode.Tracks);
			else 
				MPK.setMode(KnobMode.Clock); 
		else if (data1 == PRIMARY_CC.get(3) && data2 > 0) // EFX . . . []  
			if (MPK.getMode() == KnobMode.Effects1)
				MPK.setMode(KnobMode.Effects2);
			else 
				MPK.setMode(KnobMode.Effects1);
		
		///////// ROW 2 /////////////////

		else if (data1 == PRIMARY_CC.get(4)) { 
			Transpose.toggle();
		}
		else if (data1 == PRIMARY_CC.get(5)) { // Jamstik midi on/off
			Jamstik.toggle();
		}
		else if (data1 == PRIMARY_CC.get(6) && data2 > 0) {
			Jamstik.nextMidiOut();
		}
		else if (data1 == PRIMARY_CC.get(7) && data2 > 0) { // Turn on/off a Preset efx
			EffectsRack current = ControlPanel.getInstance().getCurrent();
			if (current != null) {
				current.getChannel().setPresetActive(!current.getChannel().isPresetActive());
				MainFrame.updateCurrent();
			}
		} 
		else 
			return false;
		return true;
	}

	
	private boolean doKnob(int data1, int data2) {
		switch(mode) {
			case Effects1:
				effects1(data1, data2);
				return true;
			case Effects2:
				effects2(data1, data2);
				return true;
			case Clock:
				sys.getGui().clockKnob(data1 - KNOBS.get(0), data2);
				// clock.getGui().clockKnob(data1 - KNOBS.get(0), data2);
				return true;
			case Tracks:
				trackKnobs(data1, data2);
		}
		return false;
	}
	
	private void effects1(int data1, int data2) {
		Channel channel = ControlPanel.getInstance().getChannel();

		if (data1 == MPKTools.KNOBS.get(0)) { 
			channel.getReverb().set(Reverb.Settings.Wet.ordinal(), data2);
			channel.getReverb().setActive(data2 > 0);
		}

        else if (data1 == MPKTools.KNOBS.get(1)) {
			channel.getReverb().set(Reverb.Settings.Room.ordinal(), data2); 
			channel.getReverb().setActive(data2 > thresholdLo);
		}

        else if (data1 == MPKTools.KNOBS.get(2)) {
        	channel.getReverb().set(Reverb.Settings.Damp.ordinal(), data2);
        }
        else if (data1 == MPKTools.KNOBS.get(3)) {
        	channel.getGain().setVol(data2);
        }
        
        
        if (data1 == MPKTools.KNOBS.get(4)) {
        	channel.getChorus().set(Chorus.Settings.Rate.ordinal(), data2);
        	channel.getChorus().setActive(data2 < thresholdHi);
        }
        if (data1 == MPKTools.KNOBS.get(5)) {
        	channel.getChorus().set(Chorus.Settings.Depth.ordinal(), data2);
        	channel.getChorus().setActive(data2 > thresholdLo);
        }
        else if (data1 == KNOBS.get(6)) {
        	channel.getChorus().set(Chorus.Settings.Feedback.ordinal(), data2);
        	channel.getChorus().setActive(data2 > thresholdLo);
        }
        else if (data1 == KNOBS.get(7)) {
        	channel.getCutFilter().setActive(data2 < thresholdHi);
        	if (!channel.getCutFilter().isActive()) return;
        	CutFilter party = channel.getCutFilter();
        	party.setFilterType(Type.pArTy);
        	party.setFrequency(CutFilter.knobToFrequency(data2));
        }
	}

	private void effects2(int data1, int data2) {
		Channel channel = ControlPanel.getInstance().getChannel();

		if (data1 == MPKTools.KNOBS.get(0)) { 
			channel.getEq().eqGain(EQ.EqBand.Bass, data2);
			channel.getEq().setActive(data2 > thresholdLo);
		}

        else if (data1 == MPKTools.KNOBS.get(1)) {
			channel.getEq().eqGain(EQ.EqBand.Mid, data2);
			channel.getEq().setActive(data2 > thresholdLo);
		}

        else if (data1 == MPKTools.KNOBS.get(2)) {
			channel.getEq().eqGain(EQ.EqBand.High, data2);
			channel.getEq().setActive(data2 > thresholdLo);
        }
        else if (data1 == MPKTools.KNOBS.get(3)) {
        	channel.getDelay().set(Delay.Settings.DelayTime.ordinal(), data2);
        	channel.getDelay().setActive(data2 < thresholdHi);
        }
        
        if (data1 == MPKTools.KNOBS.get(4)) {
        	channel.getOverdrive().setDrive(Constants.logarithmic(data2, 0, 1));
            channel.getOverdrive().setActive(data2 > 0);
        }
        if (data1 == MPKTools.KNOBS.get(5)) {
        	channel.getGain().setPan(data2);
        }
        else if (data1 == KNOBS.get(6)) {
        	Preset preset = (Preset)Constants.ratio(data2, JudahZone.getPresets());
        	channel.setPreset(preset);
        }
        else if (data1 == KNOBS.get(7)) {
        	channel.getDelay().set(Delay.Settings.Feedback.ordinal(), data2);
        	channel.getDelay().setActive(data2 > 0);
        }
	}
	
	
	private void trackKnobs(int data1, int data2) {
		for (int i = 0; i < MPKTools.KNOBS.size(); i++) {
			if (data1 == MPKTools.KNOBS.get(i))
				JudahMidi.getClock().getTracker().knob(i, data2);
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
            if (data1 == PRIMARY_PROG[3]) { // up instrument
                new Thread() { @Override public void run() {
                    getSynth().instUp(0, true);
                }}.start();
                return true;
            }
            if (data1 == PRIMARY_PROG[7]) { // down instrument
                new Thread(()->{getSynth().instUp(0, false);}).start();
                return true;
            }

            JackPort fluidOut = JudahMidi.getInstance().getFluidOut();
            FluidSynth synth = FluidSynth.getInstance();
            if (data1 == PRIMARY_PROG[0]) { // I want bass
                JudahMidi.queue(synth.progChange(0, 32), fluidOut);
                return true;
            }
            if (data1 == PRIMARY_PROG[1]) { // harp
                JudahMidi.queue(synth.progChange(0, 46), fluidOut);
                return true;
            }
            if (data1 == PRIMARY_PROG[2]) { // elec piano
                JudahMidi.queue(synth.progChange(0, 4), fluidOut);
                return true;
            }
            if (data1 == PRIMARY_PROG[4]) { // strings
                JudahMidi.queue(synth.progChange(0, 44), fluidOut);
                return true;
            }
            if (data1 == PRIMARY_PROG[5]) { // vibraphone
                JudahMidi.queue(synth.progChange(0, 11), fluidOut);
                return true;
            }
            if (data1 == PRIMARY_PROG[6]) { // rock organ
                JudahMidi.queue(synth.progChange(0, 18), fluidOut);
                return true;
            }
		
            // B BANK
            if (data1 == B_PROG[0]) { // sitar
            	JudahMidi.queue(synth.progChange(0, 104), fluidOut);
            	RTLogger.log(this, "B Bank!");
            }
            
            if (data1 == B_PROG[4]) { // honky tonk piano
            	JudahMidi.queue(synth.progChange(0, 3), fluidOut);
            	RTLogger.log(this, "B Bank!");
            }
            
		return false;
	}

		
}
