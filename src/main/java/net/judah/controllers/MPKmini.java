package net.judah.controllers;

import static net.judah.JudahZone.*;

import lombok.RequiredArgsConstructor;
import net.judah.fx.Delay;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.widgets.MidiPatch;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.sampler.Sample;
import net.judah.seq.MidiConstants;
import net.judah.seq.Seq;
import net.judah.seq.arp.Mode;
import net.judah.seq.track.DrumTrack;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Akai MPKmini, not the new one */
@RequiredArgsConstructor
public class MPKmini extends MidiPatch implements Controller, MPKTools, Pastels {

	public static final byte[] SUSTAIN_ON = Midi.create(Midi.CONTROL_CHANGE, 0, 64, 127).getMessage();
	public static final byte[] SUSTAIN_OFF = Midi.create(Midi.CONTROL_CHANGE, 0, 64, 0).getMessage();
	private static final int JOYSTICK_L = 127;
	private static final int JOYSTICK_R = 0;
	private final String[] ROMPLER = new String[] {
				"Acoustic Bass", "Vibraphone", "Rock Organ", "Rhodes EP",
				"Tremolo", "Oboe", "Ahh Choir", "Harp"};

	private final JudahMidi sys;
	private final Seq seq;
	
	@Override
	public boolean midiProcessed(Midi midi) {

		if (Midi.isCC(midi)) 
			return checkCC(midi.getData1(), midi.getData2());
		
		if (Midi.isProgChange(midi)) 
			return doProgChange(midi.getData1(), midi.getData2());

		if (midi.getChannel() == MidiConstants.DRUM_CH) { // user playing drum pads
			int data1 = midi.getData1();
			DrumTrack drums = null;
			int i;
			for (i = 0; i < DRUMS_A.size() ; i++) {
				if (DRUMS_A.get(i) == data1) {
					drums = getDrumMachine().getDrum1();
					break;
				}
				if (DRUMS_B.get(i) == data1) {
					drums = getDrumMachine().getDrum2();
					break;
				}
			}
			if (drums != null) {
				int translate = drums.getKit().getSamples()[i].getDrumType().getData1();
				try {
					midi.setMessage(midi.getCommand(), drums.getCh(), translate, (int) (midi.getData2() * track.getAmp()));
				} catch (Exception e) { RTLogger.warn(this, e);}
				drums.send(midi, JudahMidi.ticker());
				getLooper().getSoloTrack().beatboy(); // hot sync
			}
			return true;
		}
		
		if (seq.arpCheck(midi))
			return true;
    	if (Midi.isNote(midi) || Midi.isPitchBend(midi)) {
    		track.send(midi, JudahMidi.ticker());
    		return true;
    	}
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
			sys.synchronize(getBass());
		}
		
		else if (data1 == PRIMARY_CC.get(2) && data2 > 0 && !doubleClick()) // focus MidiGui or LFO
			nextMidiBtn();
		else if (data1 == PRIMARY_CC.get(3) && data2 > 0 && !doubleClick()) {// focus TRACKS
			if (MainFrame.getKnobMode() == KnobMode.Track)
				getSeq().getTracks().next(true);
			else 
				MainFrame.setFocus(KnobMode.Track);
		}
			
		///////// ROW 2 /////////////////
		else if (data1 == PRIMARY_CC.get(4)) 
			track.setRecord(track.isRecord());
		
		else if (data1 == PRIMARY_CC.get(5)) 
			track.getArp().toggle(Mode.MPK);

		else if (data1 == PRIMARY_CC.get(6) && data2 > 0 && !doubleClick()) { // focus Synth1 or Synth2 or Sampler
			if (MainFrame.getKnobMode() == KnobMode.DCO) {
				if (getFrame().getKnobs() == getSynth1().getSynthKnobs()) 
					MainFrame.setFocus(getSynth2().getSynthKnobs());
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

	private long doubleClick = System.currentTimeMillis();
	private boolean doubleClick() {
		if (System.currentTimeMillis() < Constants.DOUBLE_CLICK + doubleClick)
			return true;
		doubleClick = System.currentTimeMillis();
		return false;
	}

	private final KnobMode[] midiBtnSequence = new KnobMode[] 
			{KnobMode.Midi, KnobMode.Setlist, KnobMode.Presets, KnobMode.Tools};
	private void nextMidiBtn() {
		KnobMode mode = MainFrame.getKnobMode();
		int idx = 0;
		for (; idx < midiBtnSequence.length; idx++) {
			if (mode == midiBtnSequence[idx]) {
				if (idx == midiBtnSequence.length - 1)
				  idx = 0;
				else 
					idx++;
				MainFrame.setFocus(midiBtnSequence[idx]);
				return;
			}
		}
		MainFrame.setFocus(KnobMode.Midi);
	}

	private boolean joystickL(int data2) { // delay
		Delay d = ((Channel)track.getMidiOut()).getDelay();
		d.setActive(data2 > 4);
		if (data2 <= 4) 
			return true;
		if (d.getDelay() < Delay.DEFAULT_TIME) 
			d.setDelayTime(Delay.DEFAULT_TIME);
		d.setFeedback(Constants.midiToFloat(data2));
		MainFrame.update(track.getMidiOut());
		return true;
	}
	
	private boolean joystickR(int data2) { // filter
		MainFrame.update(track.getMidiOut());
		int ch = track.getCh();
		track.getMidiOut().send( Midi.create(Midi.CONTROL_CHANGE, ch, 1, 
				data2 > 4 ? data2 : 0), JudahMidi.ticker());  
		
		return true;
	}
	
	private boolean doProgChange(int data1, int data2) {
		// Bank A: Fluid presets
		for (int i = 0; i < PRIMARY_PROG.length; i++) 
			if (data1 == PRIMARY_PROG[i]) {
				getFluid().progChange(ROMPLER[i]);
				return true;
			}
		// Bank B: set current track's pattern #
		for (int i = 0; i < B_PROG.length; i++) {
			if (data1 == B_PROG[i]) {
				seq.getCurrent().toFrame(i);
				return true;
			}
		}
		return false;
	}

	public void record() {
		track.setRecord(!track.isRecord());
		if (frame == null)
			return;
		frame.setBackground(track.isRecord() ? Pastels.RED : null);	
	}

	public void transpose() {
		track.getArp().toggle(Mode.MPK);
		if (frame == null)
			return;
		frame.setBackground(track.getArp().getMode() == Mode.MPK ? Mode.MPK.getColor() : null);	
	}
	
}

