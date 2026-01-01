package net.judah.controllers;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import judahzone.api.Controller;
import judahzone.api.Midi;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.LineIn;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.SynthKnobs;
import net.judah.looper.Loop;
import net.judah.midi.JudahClock;
import net.judah.mixer.Fader;
import net.judah.seq.SynthRack;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.TacoSynth;
import net.judah.synth.taco.TacoTruck;

@RequiredArgsConstructor
public class Beatstep implements Controller {

	public static final String NAME = "Arturia BeatStep"; // ALSA prefix

	private static final int TEMPO_KNOB = 7; // CC
	private static final int[] KNOBS = new int[] {
			10, 74, 71, 76, 77, 93, 73, 75,
			114, 18, 19, 16, 17, 91, 79, 72 };

	private static final int GUITAR = 44;
	private static final int TACO = 47;
	private static final int TACO2 = 48;
	private static final int LOOPA = 36;
	private static final int MAINS = 51;
	private static final int RECORD = 40;
	private static final int FLUID2 = 41;
	private static final int KITS = 42;
	private static final int FADER = 43;

	private final MultiSelect channels = new MultiSelect();

	private final JudahZone zone;
	private final JudahClock clock;

	@Override
	public boolean midiProcessed(Midi midi) {

		if (midi.getChannel() != 0) // ignore sequencer
			return true;

		int data1 = midi.getData1();
		if (Midi.isNote(midi)) { // Pads
			if (data1 == KITS) {
				if (Midi.isNoteOn(midi))
					MainFrame.kit();
				return true;

			}
			else if (data1 == TACO) {
				taco1(midi.getCommand());
				return true;
			}
			else if (data1 == TACO2) {
				taco2(midi.getCommand());
				return true;
			}
			else if (data1 == FLUID2) {
				if (Midi.isNoteOn(midi))
					fluid2();
				return true;
			}
			else if (data1 == RECORD) {
				if (!Midi.isNoteOn(midi)) return true;
				for (Channel current : zone.getFxRack().getSelected())
					if (current instanceof LineIn)
						((LineIn)current).setMuteRecord(!((LineIn)current).isMuteRecord());
					else
						current.setOnMute(!current.isOnMute());
			}

			else if (data1 == FADER) {
				if (!Midi.isNoteOn(midi)) return true;
				for (Channel ch : zone.getFxRack().getSelected()) {
					if (ch.isOnMute() || ch.getGain().getGain() <= 0.05f) {
						ch.setOnMute(false);
						Fader.execute(new Fader(ch, Fader.DEFAULT_FADE, 0, 51));
					}
					else
						Fader.execute(new Fader(ch, Fader.DEFAULT_FADE, ch.getVolume(), 0));
				}
			}
			else if (data1 >= LOOPA && data1 <= MAINS) {
				multiSelect(midi.getCommand(), data1);
			}
			return true;
		}

		if (Midi.isCC(midi)) {
			int data2 = midi.getData2();
			if (data1 == TEMPO_KNOB) {
				float tempo = clock.getTempo() + (data2 > 64 ? -1 : 1); // wonky?
				clock.setTempo(tempo);
				return true;
			}

			for (int i = 0; i < KNOBS.length; i++)
				if (KNOBS[i] == data1) {
					for (Channel ch : zone.getFxRack().getSelected()) {
						ch.getGui().knob(i, data2 < 64);
					}
					return true;
				}
		}
		else if (midi.getCommand() == 240 && data1 == 127) { // start/stop btns to clock
			if (clock.isActive()) {
				for (Loop l : zone.getLooper())
					if (l.isPlaying())
						l.setOnMute(true);
				clock.end();
			}
			else {
				clock.begin();
				zone.getLooper().rewind();
			}
		}
		return true;
	}

	private void multiSelect(int command, int data1) {
		Channel ch = channel(data1);
		if (ch == null)
			return;
		if (command == ShortMessage.NOTE_OFF) {
			channels.remove(ch);
		}
		else {
			channels.add(ch);
			MainFrame.setFocus(channels.size() == 1 ? ch : channels);
		}
	}

	private Channel channel(int data1) {
		if (data1 < LOOPA || data1 > MAINS)
			return null;
		if (data1 == MAINS)
			return zone.getMains();
		if (data1 == MAINS - 1)
			return zone.getDrumMachine();
		if (data1 == MAINS - 2)
			return zone.getFluid();
		if (data1 == MAINS - 3)
			return zone.getTk2();
		if (data1 >= GUITAR)
			return zone.getInstruments().get(data1 - GUITAR);
		else
			return zone.getLooper().get(data1 - LOOPA);
	}

	private void taco1(int command) {
		TacoTruck taco = SynthRack.getTacos()[0];
		if (channels.contains(taco)) {
			if (taco.getTracks().size() < 2) {
				multiSelect(command, TACO);
				return;
			}

			channels.clear();
			channels.add(taco);
			if (MainFrame.getKnobMode() == KnobMode.Taco && command == Midi.NOTE_ON) {
				SynthKnobs knobs = (SynthKnobs) MainFrame.getKnobs();
				TacoSynth s = knobs.getSynth();
				int idx = 1 + taco.getTracks().indexOf(s);
				if (idx >= taco.getTracks().size())
					idx = 0;
				MainFrame.setFocus(taco.getTracks().get(idx).getKnobs());
			}
		}
		else {
			multiSelect(command, TACO);
		}


	}

	private void taco2(int command) { // rotary focus additional synths
		TacoTruck[] engines = SynthRack.getTacos();
		if (engines.length < 2) {// no taco 4 u
			multiSelect(command, TACO2);
			return;
		}
		channels.clear();
		if (command == Midi.NOTE_OFF)
			return;

		Channel current = JudahZone.getInstance().getFxRack().getChannel();
		int target = 1;
		if (false == current instanceof TacoTruck) {
			MainFrame.setFocus(engines[target]);
			return;
		}

		ArrayList<TacoSynth> synths = new ArrayList<TacoSynth>();

		for (int i = 1; i < engines.length; i++) {
			synths.addAll(engines[i].getTracks());
		}

		int idx = 0;
		if(MainFrame.getKnobMode() != KnobMode.Taco) {
			channels.clear();
			channels.add(engines[target]);
			MainFrame.setFocus(engines[target]);
			return;
		}
		TacoSynth knobs = ((SynthKnobs) MainFrame.getKnobs()).getSynth();
		for (int i = 0; i < synths.size(); i++)
			if (synths.get(i) == knobs) {
				idx = i + 1;
			}
		if (idx == synths.size())
			idx = 0;

		TacoSynth next = synths.get(idx);
		if (current != next.getMidiOut()) {
			MainFrame.setFocus(next.getMidiOut());
			channels.clear();
			channels.add((TacoTruck)next.getMidiOut());
		}
		MainFrame.setFocus(next.getKnobs());
	}

	private void fluid2() { // synths not on mixer strip

		FluidSynth[] engines = SynthRack.getFluids();
		if (engines.length < 2) // drink something
			return;
		channels.clear();

		int target = 1;
		Channel current = zone.getFxRack().getChannel();
		if (false == current instanceof FluidSynth) {
			MainFrame.setFocus(engines[target]);
			return;
		}
		for (int i = 0; i < engines.length; i++) {
			if (current == engines[i]) {
				target = i + 1;
				break;
			}
		}
		if (target >= engines.length)
			target = 1;
		MainFrame.setFocus(engines[target]);
	}

}
