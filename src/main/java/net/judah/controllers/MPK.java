package net.judah.controllers;

import static net.judah.JudahZone.getSynth;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.ControlPanel;
import net.judah.api.Midi;
import net.judah.beatbox.GMDrum;
import net.judah.clock.JudahClock;
import net.judah.clock.JudahClock.Mode;
import net.judah.effects.Chorus;
import net.judah.effects.Compression;
import net.judah.effects.CutFilter;
import net.judah.effects.CutFilter.Type;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.api.Reverb;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;

public class MPK extends MPKTools implements Controller {
	
	@Getter private static KnobMode mode = KnobMode.Clock;
	public static final int thresholdLo = 1;
	public static final int thresholdHi = 98;
	
	public static void setMode(KnobMode knobs) {
		mode = knobs;
		MainFrame.updateCurrent();
		//RTLogger.log(instance, "Knobs*: " + mode.toString());
	}
	
	@Override
	public boolean midiProcessed(Midi midi) {
		if (midi.isCC()) return checkCC(midi.getData1(), midi.getData2());
		if (midi.isProgChange()) return doProgChange(midi.getData1(), midi.getData2());
		if (midi.getChannel() == 9 && (midi.getCommand() == Midi.NOTE_ON || midi.getCommand() == Midi.NOTE_OFF) 
				&& (DRUMS_A.contains(midi.getData1()) || DRUMS_B.contains(midi.getData1())))
			try {
				return doDrumPad(midi);
			} catch (Throwable t) {
				RTLogger.warn(this,  t.getMessage());
			}
		return false;
	}

	private boolean doDrumPad(ShortMessage midi) throws InvalidMidiDataException, JackException {
		JackPort out = JudahClock.getInstance().getSequencer(0).getMidiOut();
		int data1 = midi.getData1();
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
			
			return true;
	}

	private boolean checkCC(int data1, int data2) {
		if (KNOBS.contains(data1)) 
			return doKnob(data1, data2);
		if (data1 == JOYSTICK_UP_CC) 
			return joystick(true, data2);
		if (data1 == JOYSTICK_DOWN_CC)
			return joystick(false, data2);
		if (PRIMARY_CC.contains(data1)) 
			return cc_pad(data1, data2);
		return false;
	}

	private boolean cc_pad(int data1, int data2) {
		if (data1 == PRIMARY_CC.get(0))
			// toggle clock
			JudahClock.setMode(data2 > 0 ? Mode.Internal : Mode.Midi24);
		else if (data1 == PRIMARY_CC.get(1)) {
			// toggle korg pads
			;
		}
		else if (data1 == PRIMARY_CC.get(2) && data2 > 0) 
			MPK.setMode(KnobMode.Clock); // clock knobs
		else if (data1 == PRIMARY_CC.get(3) && data2 > 0)  
			MPK.setMode(KnobMode.Effects1); // fx1 knobs
//		else if (data1 == PRIMARY_CC.get(4))
//			// toggle keys out
//			;
//		else if (data1 == PRIMARY_CC.get(5))
//			// freeze
//			;
		else if (data1 == PRIMARY_CC.get(6)) {
			// lfo knobs
			;
		}
		else if (data1 == PRIMARY_CC.get(7) && data2 > 0) 
			MPK.setMode(KnobMode.Effects2); // fx2 knobs
		else 
			return false;
		return true;
	}

	
	/*	room dry pan distortion
	chorus1 2 3  partyEq
	
	  bass mid treb t/hold
	d.time d.fback noise1 noise2 */
	private boolean doKnob(int data1, int data2) {
		switch(mode) {
			case Effects1:
				effects1(data1, data2);
				return true;
			case Effects2:
				effects2(data1, data2);
				return true;
			case Clock:
				clockKnobs(data1, data2);
				return true;
		}
		return false;
	}
		// TODO
    // channel.getLfo().setFrequency((data2 + 1) * 33);
    // channel.getLfo().setActive(data2 < thresholdHi);
	//	Target target = lfoLookup(data2);
	//	if (target != null) 
	//		channel.getLfo().setTarget(target); 
	//	if (target == null) channel.getLfo().setActive(false);
//	private Target lfoLookup(int data2) {
//		if (data2 / 20 < Target.values().length)
//			return Target.values()[data2 / 20];
//		return null;
//	}
	
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
        	channel.getOverdrive().set(0, data2);
            channel.getOverdrive().setActive(data2 > 0);
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
        	float res = (data2 > 50) ? 
        		8 + (data2 - 50) * 0.2f :
        		8 + (50 - data2) * 0.2f;
        	party.setResonance(res);
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
        	channel.getGain().setVol(data2);
        }
        if (data1 == MPKTools.KNOBS.get(5)) {
        	channel.getGain().setPan(data2);
        }
        else if (data1 == KNOBS.get(6)) {
        	channel.getCompression().setActive(data2 < thresholdHi);
        	channel.getCompression().set(Compression.Settings.Threshold.ordinal(), data2);
        }
        else if (data1 == KNOBS.get(7)) {
        	channel.getDelay().set(Delay.Settings.Feedback.ordinal(), data2);
        	channel.getDelay().setActive(data2 > 0);
        }
	}
	
	/** <pre>
	 * tempo    latch       songType/trans velocity
	 * midiOut  instrument  folder         title
	 </pre>*/
	private void clockKnobs(int data1, int data2) {
		JudahClock clock = JudahClock.getInstance();
		if (data1 == MPKTools.KNOBS.get(0)) { 
			clock.setTempo( (data2 + 40) * 1.25f);
		}
        else if (data1 == MPKTools.KNOBS.get(1)) {
        	if (data2 == 0) 
        		clock.setLength(1);
        	else 
        		clock.setLength(JudahClock.LENGTHS[ (int)((data2 - 1) / (100 / (float)JudahClock.LENGTHS.length))]);
		}
        else if (data1 == MPKTools.KNOBS.get(2)) {
        }
        else if (data1 == MPKTools.KNOBS.get(3)) {
        }
        if (data1 == MPKTools.KNOBS.get(4)) {
        }
        if (data1 == MPKTools.KNOBS.get(5)) {
        }
        else if (data1 == KNOBS.get(6)) {
        }
        else if (data1 == KNOBS.get(7)) {
        }
	}
	
	private boolean joystick(boolean up, int data2) {
		Channel channel = ControlPanel.getInstance().getChannel();
		if (up) // delay
			if (data2 < 77)
				channel.getDelay().reset();
			else {
				Delay d = channel.getDelay();
				d.setActive(true);
				if (d.getDelay() < Delay.DEF_TIME) d.setDelay(Delay.DEF_TIME);
				d.setFeedback((data2 - 77) / 50f); // 77 to 127;
			}
		else { // distortion and chorus
			if (data2 < 20) {
                channel.getOverdrive().setActive(false);
                channel.getChorus().setActive(false);
            }
            else {
                channel.getOverdrive().setActive(true);
                channel.getOverdrive().setDrive((data2 - 28) / 100f);
                channel.getChorus().setActive(true);
                channel.getChorus().setFeedback((data2 - 50) / 100f);
            }
            return true;
		}
		return false;
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

            if (data1 == PRIMARY_PROG[0]) { // I want bass
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 32));
                return true;
            }
            if (data1 == PRIMARY_PROG[1]) { // harp
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 46));
                return true;
            }
            if (data1 == PRIMARY_PROG[2]) { // elec piano
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 5));
                return true;
            }
            if (data1 == PRIMARY_PROG[4]) { // strings
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 44));
                return true;
            }
            if (data1 == PRIMARY_PROG[5]) { // vibraphone
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 11));
                return true;
            }
            if (data1 == PRIMARY_PROG[6]) { // rock organ
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 18));
                return true;
            }
		
            // B BANK
            if (data1 == B_PROG[0]) { // sitar
            	JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 104));
            	RTLogger.log(this, "B Bank!");
            }
            
            if (data1 == B_PROG[4]) { // honky tonk piano
            	JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 3));
            	RTLogger.log(this, "B Bank!");
            }
            
		return false;
	}

		
}
