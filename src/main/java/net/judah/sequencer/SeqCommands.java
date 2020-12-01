package net.judah.sequencer;

import static net.judah.settings.CMD.SequencerLbls.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import net.judah.mixer.MixerCommands;
import net.judah.settings.Command;
import net.judah.settings.CommandPair;

public class SeqCommands extends ArrayList<Command> {

	final Command trigger, end, external, internal, dropBeat, qPlay, qRecord, reload;
	
	SeqCommands(final Sequencer seq) {
		trigger = new Command(TRIGGER.name, TRIGGER.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.trigger();}};
		end = new Command(END.name, END.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.stop();}};
		external = new Command(EXTERNAL.name, EXTERNAL.desc, externalParams()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.externalControl(props);}};
		internal = new Command(INTERNAL.name, INTERNAL.desc, internalParams()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) {
				seq.internal("" + props.get(Sequencer.PARAM_PATCH));}};
		dropBeat = new Command(DROPBEAT.name, DROPBEAT.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.dropDaBeat(new CommandPair(this, null));}};
		qPlay = new Command(QPLAY.name, QPLAY.desc, MixerCommands.loopProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				props.put(ACTIVE_PARAM, midiData2 > 0);
				seq.qPlay(props);}};
		qRecord = new Command(QREC.name, QREC.desc, MixerCommands.loopProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				props.put(ACTIVE_PARAM, midiData2 > 0);
				seq.qRecord(props);}};
		reload = new Command(RELOAD.name, RELOAD.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.getPage().reload();}};

		addAll(Arrays.asList(new Command[] {
				trigger, end, external, internal, dropBeat, qPlay, qRecord, reload,	
		}));
	}

	private HashMap<String, Class<?>> internalParams() {
		HashMap<String, Class<?>> result = new HashMap<>(2);
		result.put(Sequencer.PARAM_PATCH, String.class);
		return result;
	}
	
	private HashMap<String, Class<?>> externalParams() {
		HashMap<String, Class<?>> result = new HashMap<>(2);
		result.put(Sequencer.PARAM_LOOP, Integer.class);
		result.put(Sequencer.PARAM_UNIT, Integer.class);
		return result;
	}

}

/*	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		if (cmd == trigger) 
			trigger();
		if (cmd == end) 
			stop();
		if (cmd == externalControl) 
			externalControl(props);
		if (cmd == dropBeatCmd) {
			mixerState = mixer.muteAll();
			metronome.mute();
			queue.push(new CommandPair(dropBeatCmd, null));
		}
		if (cmd == queuePlay) 
			if (control == ControlMode.INTERNAL) {
				int candidate = count + 1;
				while (candidate % measure != 0) 
					candidate++;
				props.put(PARAM_SEQ_INTERNAL, candidate);
			}
			queue.push(new CommandPair(mixer.getCommands().getPlayCmd(), props));
		if (cmd == queueRecord)
			if (control == ControlMode.INTERNAL) {
				int candidate = count + 1;
				while (candidate % measure != 0) 
					candidate++;
				props.put(PARAM_SEQ_INTERNAL, candidate);
			}
			queue.push(new CommandPair(mixer.getCommands().getRecordCmd(), props));
	}

*/