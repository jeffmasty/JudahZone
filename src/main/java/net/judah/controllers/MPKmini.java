
package net.judah.controllers;

import static net.judah.JudahZone.*;
import static net.judah.controllers.MPKTools.*;
import static net.judah.gui.knobs.KnobMode.*;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import lombok.Getter;
import net.judah.fx.Delay;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.gui.Updateable;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.mixer.Channel;
import net.judah.sampler.Sample;
import net.judah.seq.track.PianoTrack;
import net.judah.util.Constants;
import net.judah.util.Debounce;


/** Akai MPKmini, not the new one */
public class MPKmini extends JComboBox<PianoTrack> implements Updateable, Controller {

	public static final MPKmini instance = new MPKmini();
	private MPKmini() {}

	public static final String NAME = "MPKmini2"; // midi port
	private static final String[] ROMPLER = new String[] {
				"Acoustic Bass", "Vibraphone", "Rock Organ", "Rhodes EP",
				"Tremolo", "Oboe", "Ahh Choir", "Harp"};

	// Where to route live MPK keys (and Jamstik)
	@Getter private PianoTrack midiOut;

	@Override
	public boolean midiProcessed(Midi midi) {

		if (Midi.isCC(midi))
			return checkCC(midi.getData1(), midi.getData2());

		if (Midi.isProgChange(midi))
			return doProgChange(midi.getData1(), midi.getData2());

		if (getSeq().captured(midi))  // recording or transposing or drums
			return true;

    	if (Midi.isNote(midi) || Midi.isPitchBend(midi)) {
    		midiOut.send(midi, JudahMidi.ticker());
    		return true;
    	}
		return false;
	}

	public MPKmini fill(PianoTrack[] items, PianoTrack selected) {
		setRenderer(new BasicComboBoxRenderer() {
			@Override public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list,
					Object value, int index, boolean isSelected, boolean cellHasFocus) {
				setHorizontalAlignment(SwingConstants.CENTER);

				if (value instanceof PianoTrack p) {
					if (p == items[0])
						setText("Taco");
					else
						setText(p.getMidiOut().getName());
				}
				else
					setText("?");
				return this;
			}
		});

		for (PianoTrack out : items) {
			addItem(out);
			if (out == selected) {
				setSelectedItem(out);
				midiOut = out;
			}
		}
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e))
					TabZone.edit((PianoTrack)getSelectedItem());
			}
		});
		addActionListener(e->setMidiOut((PianoTrack)getSelectedItem()));
		return this;
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
			Sample s = getSampler().getSamples().get(SAMPLES_CC.indexOf(data1));
			getSampler().play(s, data2 > 0);
			return true;
		}
		return false;
	}

	private boolean cc_pad(int data1, int data2) {
		//1   rec   mpk  midiGui  track
		//2   pdl  chords  lfo     set

		///////// ROW 1 /////////////////
		if (data1 == PRIMARY_CC.get(0))  {
			getMidiGui().getToggler().capture(midiOut, data2 > 0);
		}
		else if (data1 == PRIMARY_CC.get(1)) {
			getMidiGui().getToggler().mpk(midiOut, data2 > 0);
		}

		else if (data1 == PRIMARY_CC.get(2) && data2 > 0 && !flooding())
			nextMidiBtn(); // focus MidiGui or...
		else if (data1 == PRIMARY_CC.get(3) && data2 > 0 && !flooding()) {
			if (MainFrame.getKnobMode() == Track) // focus TRACKS
				getSeq().getTracks().next(true);
			else
				MainFrame.setFocus(Track);
		}

		///////// ROW 2 /////////////////
		else if (data1 == PRIMARY_CC.get(4)) {
			midiOut.getPedal().setPressed(data2 > 0);
		}
		else if (data1 == PRIMARY_CC.get(5)) {
			getChords().toggle();
		}

		else if (data1 == PRIMARY_CC.get(6) && data2 > 0 ) {
			if (MainFrame.getKnobMode() == KnobMode.LFO)
				((LFOKnobs)MainFrame.getKnobs()).upperLower();
			else
				MainFrame.setFocus(KnobMode.LFO);
		}
		else if (data1 == PRIMARY_CC.get(7) && data2 > 0) { // SET SettableCombo
			MainFrame.set();
		}

		else
			return false;
		return true;
	}

	// replacing knobs panel sometimes freezes Swing/AWT
	private static final long FLOODING = Debounce.DOUBLE_CLICK / 3;
	private long doubleClick = System.currentTimeMillis();
	private boolean flooding() {
		if (System.currentTimeMillis() < FLOODING + doubleClick)
			return true;
		doubleClick = System.currentTimeMillis();
		return false;
	}

	private final KnobMode[] midiBtnSequence = new KnobMode[]
			{ MIDI, Setlist, Sample, Presets, Wavez, Log };

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
		MainFrame.setFocus(MIDI);
	}

	private boolean joystickL(int data2) { // delay
		Delay d = ((Channel)midiOut.getMidiOut()).getDelay();
		d.setActive(data2 > 4);
		if (data2 <= 4)
			return true;
		if (d.getDelay() < Delay.DEFAULT_TIME)
			d.setDelayTime(Delay.DEFAULT_TIME);
		d.setFeedback(Constants.midiToFloat(data2));
		MainFrame.update(midiOut);
		return true;
	}

	private boolean joystickR(int data2) { // filter
		MainFrame.update(midiOut);
		midiOut.send( Midi.create(Midi.CONTROL_CHANGE, 0, 1,
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
				getSeq().getCurrent().toFrame(i);
				return true;
			}
		}
		return false;
	}

	public void setMidiOut(PianoTrack out) {
		if (out == null)
			return;
		if (midiOut != null)
			new Panic(midiOut);
		midiOut = out;
		MainFrame.update(this);
	}

	@Override public void update() {
		if (midiOut != getSelectedItem())
			setSelectedItem(midiOut);
	}

}

