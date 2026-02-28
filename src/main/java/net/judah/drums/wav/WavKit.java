package net.judah.drums.wav;

import judahzone.data.Letter;
import judahzone.data.Recording;
import judahzone.gui.Icons;
import net.judah.drums.Drum;
import net.judah.drums.DrumInit;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumMachine;
import net.judah.drums.DrumType;
import net.judah.drums.KitDB.KitSetup;
import net.judah.gui.MainFrame;
import net.judah.seq.track.Computer;

public class WavKit extends DrumKit {
	public static final int SAMPLES = DrumType.values().length;
	private final WavDrum[] samples = new WavDrum[SAMPLES];

	// TODO cut filter setup

	public WavKit(DrumMachine engine, DrumInit type) {
		super(type.name(), engine, type);
		icon = Icons.get("DrumMachine.png");
		for (int i = 0; i < SAMPLES; i++)
			samples[i] = new WavDrum(DrumType.values()[i], actives);
		gain.setPreamp(0.5f); // TODO
	}

	@Override public void accept(KitSetup t) {
		setChoked(t.choke());
		setKitName(t.name());
		for (int i = 0; i < samples.length; i++) {
			samples[i].getGain().setGain(t.gains()[i].gain());
			samples[i].getGain().setPan(t.gains()[i].pan());
			// preamp?
			samples[i].getEnv().sendLetter(new Letter(t.env()[i]));
		}
		if (knobs != null)
			MainFrame.update(knobs);
	}

//	@Override public void send(MidiMessage msg, long timeStamp) {
//	    if (msg instanceof MetaMessage)
//	        return;
//	    ShortMessage midi = (ShortMessage) msg;
//	    if (cc.process(midi))
//	        return;
//
//	    int data1 = midi.getData1();
//	    int data2 = midi.getData2();
//
//	    if (Midi.isNoteOn(midi)) {
//	        for (DrumSample sample : samples) {
//	            if (sample.getType().getData1() != data1)
//	                continue;
//	            sample.trigger(data2);
//	            if (sample.getType() == DrumType.CHat && isChoked())
//	                if (actives.isActive(CHOKE.getData1()) || samples[DrumType.OHat.ordinal()].isPlaying())
//	                    samples[DrumType.OHat.ordinal()].off();
//	            break;
//	        }
//
//
//
//	        actives.receivePacked(MidiCmdPack.pack(MidiCmdPack.CMD_ADD, data1, data2, actives.getChannel()));
// 	        MainFrame.update(actives);
//	    } else if (Midi.isNoteOff(midi)) {
//	        for (DrumSample sample : samples) {
//	            if (sample.getType().getData1() == data1) {
//	                sample.off();
//	                break;
//	            }
//	        }
//	        actives.receivePacked(MidiCmdPack.pack(MidiCmdPack.CMD_REMOVE, data1, 0, actives.getChannel()));
//	        MainFrame.update(actives);
//	    }
//	}

	@Override public String progChange(int data1) {
		for (WavDrum s : samples)
			s.play(false);
		String[] kits = WavDrumDB.getPatches();
		if (data1 < 0 || data1 >= kits.length)
			return null;
		if (progChange(kits[data1]))
			return kits[data1];
		return null;
	}

	@Override public boolean progChange(String name) {
		for (WavDrum s : samples)
			s.play(false);

		Recording[] pads = WavDrumDB.getKits().get(name);
		if (pads != null) {
			for (int i = 0; i < SAMPLES; i++)
				if (pads[i] != null)
					samples[i].setRecording(pads[i]);
			MainFrame.updateTrack(Computer.Update.PROGRAM, drumMachine.getTrack(this));
			return true;
		}
		return false;
	}

	@Override public int getChannel() {
		return actives.getChannel();
	}

	public WavDrum getSample(DrumType type) {
		for (int i = 0; i < SAMPLES; i++)
			if (samples[i].getType() == type)
				return samples[i];
		return null;
	}

//	@Override public DrumKnobs getKnobs() {
//		if (knobs == null)
//			knobs = new SampleDrums(this);
//		return knobs;
//	}

//	@Override public void save(String name) {
//		if (name == null || name.isBlank())
//			name = getProgram();
//		setKitName(name);
//		try {
//			KitDB.addOrReplace(get(), false);
//		} catch (IOException e) {
//			RTLogger.warn(e);
//		}
//	}

	@Override public String[] getPatches() {
		return drumMachine.getSamplePresets();
	}

	@Override
	public Drum get(int idx) {
		return samples[idx];
	}

//	@Override
//	public int size() {
//		return samples.length;
//	}

}
