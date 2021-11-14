package net.judah.controllers;

import static net.judah.JudahZone.getSynth;

import lombok.Getter;
import net.judah.MixerPane;
import net.judah.api.Midi;
import net.judah.effects.CutFilter;
import net.judah.effects.CutFilter.Type;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.LFO.Target;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.util.Console;

public class MPK extends MPKTools implements Controller {

	@Getter private static KnobMode mode = KnobMode.Effects1;
	public static final int threshholdLo = 3;
	public static final int thresholdHi = 95;
	
	@Override
	public boolean midiProcessed(Midi midi) {
		if (midi.isCC()) return checkCC(midi.getData1(), midi.getData2());
		if (midi.isProgChange()) return checkProg(midi.getData1(), midi.getData2());
		return false;
	}

	private boolean checkProg(int data1, int data2) {

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
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 33));
                return true;
            }
            if (data1 == PRIMARY_PROG[1]) { // harp
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 46));
                return true;
            }
            if (data1 == PRIMARY_PROG[2]) { // piano
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 0));
                return true;
            }
            if (data1 == PRIMARY_PROG[4]) { // strings
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 44));
                return true;
            }
            if (data1 == PRIMARY_PROG[5]) { // church organ
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 19));
                return true;
            }
            if (data1 == PRIMARY_PROG[6]) { // electric piano
                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 5));
                return true;
            }
		
		return false;
	}

	private boolean checkCC(int data1, int data2) {
		if (KNOBS.contains(data1)) 
			return doKnob(data1, data2);
		if (data1 == JOYSTICK_UP_CC) 
			return joystick(true, data2);
		if (data1 == JOYSTICK_DOWN_CC)
			return joystick(false, data2);
		
		return false;
	}

	private boolean joystick(boolean up, int data2) {
		Channel channel = MixerPane.getInstance().getChannel();
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
	
	
	/*	room   	d.time 	dist.	c.depth	 	
		dry    	d.fback pArTyEQ	c.rate		 */
	private boolean doKnob(int data1, int data2) {
		switch(mode) {
			case Effects1:
				effects1(data1, data2);
				return true;
			case Effects2:
				effects2(data1, data2);
				return true;
		}
		return false;
	}
	
	private void effects1(int data1, int data2) {
		Channel channel = MixerPane.getInstance().getChannel();

		if (data1 == MPKTools.KNOBS.get(0)) { 
			channel.getReverb().setRoomSize(data2 * 0.01f);
			channel.getReverb().setActive(data2 > threshholdLo);
		}

        else if (data1 == MPKTools.KNOBS.get(1)) {
			channel.getDelay().setDelay(data2 * 0.02f); // 2 seconds max
		}

        else if (data1 == MPKTools.KNOBS.get(2)) {
            channel.getOverdrive().setDrive(data2);
            channel.getOverdrive().setActive(data2 > threshholdLo);
        }
        else if (data1 == MPKTools.KNOBS.get(3)) {
        	channel.getLfo().setFrequency((data2 + 1) * 28);
        	channel.getLfo().setActive(data2 < thresholdHi);
        }
        
        
		
        if (data1 == MPKTools.KNOBS.get(4)) {
        	channel.getReverb().setDamp(data2 * 0.01f);
        }
        if (data1 == MPKTools.KNOBS.get(5)) {
        	channel.getDelay().setFeedback(data2 * 0.01f);
        	channel.getDelay().setActive(data2 > 0);
        }
        else if (data1 == KNOBS.get(6)) {
        	channel.getCutFilter().setFilterType(Type.pArTy);
        	channel.getCutFilter().setFrequency(CutFilter.knobToFrequency(data2));
        	channel.getCutFilter().setActive(data2 < thresholdHi);
        	if (!channel.getCutFilter().isActive()) return;
        	float res = (data2 > 50) ? 
        		6 + (data2 - 50) * 0.33f :
        		6 + (50 - data2) * 0.33f;
        	channel.getCutFilter().setResonance(res);
        }
        else if (data1 == KNOBS.get(7)) {
        	Target target = lfoLookup(data2);
        	if (target != null) 
        		channel.getLfo().setTarget(target);
        	channel.getLfo().setActive(target != null);
        }
	}

	private Target lfoLookup(int data2) {
		for (int ordinal = 0; ordinal < Target.values().length; ordinal++) 
			if (data2 - 20 / 20 == ordinal) 
				return Target.values()[ordinal];
		return null;
	}
	
	private void effects2(int data1, int data2) {
		Channel channel = MixerPane.getInstance().getChannel();

		if (data1 == MPKTools.KNOBS.get(0)) { 
			channel.getEq().eqGain(EQ.EqBand.BASS, data2);
			channel.getEq().setActive(data2 > threshholdLo);
		}

        else if (data1 == MPKTools.KNOBS.get(1)) {
			channel.getEq().eqGain(EQ.EqBand.MID, data2);
			channel.getEq().setActive(data2 > threshholdLo);
		}

        else if (data1 == MPKTools.KNOBS.get(2)) {
			channel.getEq().eqGain(EQ.EqBand.TREBLE, data2);
			channel.getEq().setActive(data2 > threshholdLo);
        }
        else if (data1 == MPKTools.KNOBS.get(3)) {
        	channel.setPan(data2 * 0.01f);
        }
        
        
        if (data1 == MPKTools.KNOBS.get(4)) {
        	channel.getChorus().setRate(data2 * 0.3f);
        	channel.getChorus().setActive(data2 < thresholdHi);
        }
        if (data1 == MPKTools.KNOBS.get(5)) {
        	channel.getChorus().setDepth(data2 * 0.01f);
        	channel.getChorus().setActive(data2 > threshholdLo);
        }
        else if (data1 == KNOBS.get(6)) {
        	channel.getChorus().setFeedback(data2 * 0.01f);
        	channel.getChorus().setActive(data2 > threshholdLo);
        }
        else if (data1 == KNOBS.get(7)) {
        	channel.getCompression().setThreshold((data2 - 99) / 2.5f);
        	channel.getCompression().setActive(data2 > threshholdLo);
        }
	}
	
	public static void nextMode() {
		int ordinal = mode.ordinal() + 1;
		if (ordinal == KnobMode.values().length)
			ordinal = 0;
		mode = KnobMode.values()[ordinal];
		Console.info("Knobs: " + mode.toString());
	}

}
