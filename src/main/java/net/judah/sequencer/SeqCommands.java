package net.judah.sequencer;

import static net.judah.settings.Commands.OtherLbls.MIDIGAIN;
import static net.judah.settings.Commands.OtherLbls.MIDIPLAY;
import static net.judah.settings.Commands.OtherLbls.MIDIRECORD;
import static net.judah.settings.Commands.OtherLbls.TRANSPOSE;
import static net.judah.settings.Commands.OtherLbls.transposeTemplate;
import static net.judah.settings.Commands.SequencerLbls.ACTIVATE;
import static net.judah.settings.Commands.SequencerLbls.CLICKTRACK;
import static net.judah.settings.Commands.SequencerLbls.INTERNAL;
import static net.judah.settings.Commands.SequencerLbls.NEXT;
import static net.judah.settings.Commands.SequencerLbls.QUEUE;
import static net.judah.settings.Commands.SequencerLbls.RELOAD;
import static net.judah.settings.Commands.SequencerLbls.SETUP;
import static net.judah.settings.Commands.SequencerLbls.TRANSPORT;
import static net.judah.settings.Commands.SequencerLbls.TRIGGER;
import static net.judah.settings.Commands.SequencerLbls.VOLUME;
import static net.judah.util.Constants.Param.ACTIVE;
import static net.judah.util.Constants.Param.BPM;
import static net.judah.util.Constants.Param.FILE;
import static net.judah.util.Constants.Param.GAIN;
import static net.judah.util.Constants.Param.INDEX;
import static net.judah.util.Constants.Param.LOOP;
import static net.judah.util.Constants.Param.MEASURE;
import static net.judah.util.Constants.Param.NAME;
import static net.judah.util.Constants.Param.STEPS;
import static net.judah.util.Constants.Param.activeTemplate;
import static net.judah.util.Constants.Param.singleTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.jaudiolibs.jnajack.JackTransportState;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.api.Command;
import net.judah.api.TimeListener.Property;
import net.judah.looper.Recorder;
import net.judah.mixer.MixCommands;
import net.judah.sequencer.Sequencer.ControlMode;
import net.judah.util.CommandWrapper;
import net.judah.util.Console;
import net.judah.util.JudahException;
import net.judah.util.RTLogger;

public class SeqCommands extends ArrayList<Command> {

	/** beats before Transport starts */
	public static final String PARAM_INTRO = "intro.beats";
	/** total beats to play */
	public static final String PARAM_DURATION = "duration.beats";
	/** note_on note, first beat of bar (optional) */
	public static final String PARAM_DOWNBEAT = "midi.downbeat";
	/** note_on note (optional) */
	public static final String PARAM_BEAT = "midi.beat";
    /** name of internal routine to run */
	public static final String PARAM_PATCH = "patch";

	final Command transport;

	private final LoopCallback callback = new LoopCallback();


	public SeqCommands() {

		transport = new Command(TRANSPORT.name, TRANSPORT.desc, activeTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = false;
				if (midiData2 < 0)
					try { active = Boolean.parseBoolean("" + props.get(ACTIVE));
					} catch (Throwable t) { Console.warn(t.getMessage(), t);}
				else if (midiData2 > 0)
					active = true;
				if (active) seq.update(Property.TRANSPORT, JackTransportState.JackTransportStarting);
				else seq.update(Property.TRANSPORT, JackTransportState.JackTransportStopped);}};
		add(transport);

		add(new Command(TRIGGER.name, TRIGGER.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				if (midiData2 == 0)
					seq.songTrigger();
			}});

		add(new Command(CLICKTRACK.name, CLICKTRACK.desc, clicktrackTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {

				Integer intro = null;
				Integer duration = null;
				try { intro = Integer.parseInt(props.get(PARAM_INTRO).toString());
				} catch (Throwable e) {
					Console.warn(e.getMessage() + " " + PARAM_INTRO + " = " + props.get(PARAM_INTRO), e);
				}
				try { duration = Integer.parseInt(props.get(PARAM_DURATION).toString());
				} catch (Throwable e) {
					Console.warn(e.getMessage() + " " + PARAM_DURATION + " = " + props.get(PARAM_DURATION), e);
				}
				//		int down = 34;int up = 33;
				//		try {down = Integer.parseInt(props.get(PARAM_DOWNBEAT).toString());
				//			up = Integer.parseInt(props.get(PARAM_BEAT).toString());
				//		} catch (Throwable e) { }
				if (intro != null && duration != null) {
					SeqClock clock = new SeqClock(seq, intro, duration); // up/down beats
					seq.setClock(clock);
				}
				//	void setup(Integer intro, Integer duration) throws InvalidMidiDataException, OSCSerializeException, IOException {
				//	seq.setup(intro, duration);
			}});

		add(new Command(SETUP.name, SETUP.desc, settings()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.setClock(new SeqClock(seq, props));
			}
		});

		add(new Command(INTERNAL.name, INTERNAL.desc, internalParams()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) {
				seq.internal("" + props.get(PARAM_PATCH), "" + props.get("param"));}});

		add(new SeqCmd());

		add(new Command(ACTIVATE.name, ACTIVATE.desc, nameTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = false;
				if (midiData2 >= 0)
					active = midiData2 > 0;
				else
					active = Boolean.parseBoolean("" + props.get(ACTIVE));
				String[] names =  props.get(NAME).toString().split(",");

				for (String name : names) {
					Step data = ((SeqClock)seq.getClock()).getSequence(name);
					if (data == null)
						throw new JudahException("Unknown named sequence: " + props.get(NAME) + " in " +
								Arrays.toString(((SeqClock)seq.getClock()).getSequences().toArray()));
					else
						data.setActive(active);
				}}});

		add(new Command(VOLUME.name, VOLUME.desc, gainTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				float gain = 0f;
				if (midiData2 >= 0)
					gain = midiData2 / 100f;
				else
					gain = Float.parseFloat("" + props.get(GAIN));
				String[] names =  props.get(NAME).toString().split(",");
				for (String name : names) {
					Step data = ((SeqClock)seq.getClock()).getSequence(name);
					if (data == null)
						throw new JudahException("Unknown named sequence: " + props.get(NAME) + " in " +
								Arrays.toString(((SeqClock)seq.getClock()).getSequences().toArray()));
					else
						data.setVolume(gain);
				}}});

		add(new Command(QUEUE.name, QUEUE.desc, queueTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {

				if (ControlMode.INTERNAL == seq.getControl()) {
	                Command target = JudahZone.getCommands().find("" + props.get("command"));
	                if (target == null) throw new NullPointerException(
	                        "command " + props.get("command"));
				    int seqInternal = seq.getCount() + 1;
					while (seqInternal % seq.getClock().getMeasure() != 0)
						seqInternal++;
					seq.queue(new CommandWrapper(target, props, seqInternal));
				}
				else { // looper/external controlled
	                Recorder loopA = JudahZone.getLooper().getLoopA();
	                Recorder loopB = JudahZone.getLooper().getLoopB();
	                if (loopA.isPlaying() == AudioMode.RUNNING)
	                    callback.configure(loopA, loopB);
	                else if (loopB.isPlaying() == AudioMode.RUNNING)
	                    callback.configure(loopB, loopA);
				}}});

		add(new Command(RELOAD.name, RELOAD.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.getPage().reload();}});
		add(new Command(NEXT.name, NEXT.desc, singleTemplate(FILE, String.class)) {
			@Override public void execute(HashMap<String,Object> props, int midiData2) throws Exception {
				File song = new File(props.get(FILE).toString());
				try {
					new Sequencer(song);
					MainFrame.get().closeTab(seq.getPage());
				} catch (Exception e) {
					RTLogger.log(this, song.getName() + " -- " + e.getMessage() + " " + song.getAbsoluteFile());
				}

			};
		});

		add(new Command(MIDIRECORD.name, MIDIRECORD.desc, recordTemplate()) {
			@Override public void execute(HashMap<String,Object> props, int midiData2) throws Exception {
				boolean active = false;

				if (midiData2 >= 0)
					active = midiData2 > 0;
				else
					active = Boolean.parseBoolean(props.get(ACTIVE).toString());

				boolean onLoop = true;
				try {
					onLoop = Boolean.parseBoolean(props.get(LOOP).toString());
				} catch(Throwable t) { }
				((SeqClock)seq.getClock()).record(active, onLoop);
			}});

		add(new Command(MIDIPLAY.name, MIDIPLAY.desc, indexTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = false;
				if (midiData2 >= 0)
					active = midiData2 > 0;
				else
					active = Boolean.parseBoolean(props.get(ACTIVE).toString());
				((SeqClock)seq.getClock()).play(Integer.parseInt("" + props.get(INDEX)), active);
			}});
		add(new Command(TRANSPOSE.name, TRANSPOSE.desc, transposeTemplate()) {

			@Override
			public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				int steps = 0;
				if (midiData2 >= 0) {
					if (props.containsKey(STEPS) && props.get(STEPS) != null) {
						 if (midiData2 > 0) {
							 steps = Integer.parseInt(props.get(STEPS).toString());
							 ((SeqClock)seq.getClock()).getTracks().setTranspose(steps);
						 }
						 else {
						     ((SeqClock)seq.getClock()).getTracks().setTranspose(0);
						 }
						 return;
					}

					steps = Math.round((midiData2 - 50) / 5f);
				}
				else {
					steps = Integer.parseInt(props.get(STEPS).toString());
				}
				((SeqClock)seq.getClock()).getTracks().setTranspose(steps);
			}});
		add(new Arpeggiate());

		add(new Command(MIDIGAIN.name, MIDIGAIN.desc, indexGain()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				float gain = 0f;
				if (midiData2 >= 0)
					gain = midiData2 / 100f;
				else
					gain = Float.parseFloat("" + props.get(GAIN));
				int idx = Integer.parseInt("" + props.get(INDEX));
				if (seq.getClock() instanceof SeqClock || ((SeqClock)seq.getClock()).getTracks().size() <= idx)
					throw new JudahException("no midi track");
				((SeqClock)seq.getClock()).getTracks().setGain(idx, gain);
				}});
	}

	private HashMap<String, Class<?>> recordTemplate() {
		HashMap<String, Class<?>> result = activeTemplate();
		result.put(LOOP, Boolean.class);
		return result;
	}

	private HashMap<String, Class<?>> indexTemplate() {
		HashMap<String, Class<?>> result = activeTemplate();
		result.put(INDEX, Integer.class);
		return result;
	}

	private HashMap<String, Class<?>> gainTemplate() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(NAME, String.class);
		result.put(GAIN, Float.class);
		return result;
	}

	private HashMap<String, Class<?>> indexGain() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(INDEX, String.class);
		result.put(GAIN, Float.class);
		return result;
	}


	private HashMap<String, Class<?>> nameTemplate() {
		HashMap<String, Class<?>> result = activeTemplate();
		result.put(NAME, String.class);
		return result;
	}

	// tempo // bpb // type // intro // pulse
	public static HashMap<String, Class<?>> settings() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(BPM, Float.class);
		result.put(MEASURE, Integer.class);
		result.put(PARAM_INTRO, Integer.class);
		result.put(Sequencer.PARAM_PULSE, Integer.class);
		return result;
	}

	public static HashMap<String, Class<?>> queueTemplate() {
		HashMap<String, Class<?>> result = MixCommands.loopProps();
		result.put("command", String.class);
		return result;
	}

	public static HashMap<String, Class<?>> internalParams() {
		HashMap<String, Class<?>> result = new HashMap<>(2);
		result.put(PARAM_PATCH, String.class);
		result.put("param", String.class);
		return result;
	}

	public static HashMap<String, Class<?>> externalParams() {
		HashMap<String, Class<?>> result = new HashMap<>(2);
		result.put(LOOP, Integer.class);
		result.put(Sequencer.PARAM_PULSE, Integer.class);
		return result;
	}



	public static HashMap<String, Class<?>> clicktrackTemplate() {
		HashMap<String, Class<?>> params = new HashMap<>();

		params.put(PARAM_INTRO, Integer.class);
		params.put(PARAM_DURATION, Integer.class);
		// params.put(PARAM_DOWNBEAT, Integer.class);
		// params.put(PARAM_BEAT, Integer.class);
		//params.put(Constants.PARAM_CHANNEL, Integer.class);
		//params.put(PARAM_MIDIFILE, String.class);
		return params;
	}


}

//	add(new Command(EXTERNAL.name, EXTERNAL.desc, externalParams()) {
//	@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
//		seq.externalControl(props);}});
//	dropBeat = new Command(DROPBEAT.name, DROPBEAT.desc) {
//	@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
//		seq.dropDaBeat(new CommandPair(this, null));}};
//unit = new Command(UNIT.name, UNIT.desc, unitParams()) {
//@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
//	seq.setUnit(Sequencer.TimeBase.valueOf("" + props.get("timeUnit")));}};

/*public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
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
	}*/