package net.judah.mixer;

import static net.judah.settings.CMD.MixerLbls.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.jack.RecordAudio;
import net.judah.looper.Recorder;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.settings.Command;
import net.judah.util.JudahException;

@Log4j
public class MixerCommands extends ArrayList<Command> {
	
	protected final Mixer mixer;
	
	public static final String GAIN_PROP = "Gain";
	public static final String INDEX = "Index";
	public static final String IS_INPUT = "isInput";
	public static final String CHANNEL_PROP = "Channel";
	public static final String PLUGIN_PROP = "Plugin Name";
	public static final String PLUGIN_COMMAND = "Load Plugin";
	public static final String LOOP_PARAM = "Loop";
	public static final String CHANNEL_PARAM = "Channel";
	public static final String SOURCE_LOOP = "source.loop";
	public static final String DESTINATION_LOOP = "destination.loop";
	public static final String SOURCE_FILE = "source.file";
	public static final String LOOP_DUPLICATE = "loop.duplicate";
	public static final int ALL = -1;
	
	@Getter protected final Command playCmd;
	@Getter protected final Command recordCmd;
	protected final Command clearCommand;
	protected final Command loadSample;
	protected final Command muteCommand;
	protected final Command gainCommand;
	
	MixerCommands(Mixer mixer) {
		this.mixer = mixer;

		recordCmd = new Command(TOGGLE_RECORD.name, TOGGLE_RECORD.desc, loopProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = midiData2 > 0;
				try { active = Boolean.parseBoolean(props.get(Command.ACTIVE_PARAM).toString());
				} catch (Throwable t) {  }
				int idx = getLoopNum(props); 
				Sample s = mixer.getSamples().get(idx);
				if (false == s instanceof RecordAudio) 
					throw new JudahException("Sample " + idx + " (" + s.getName() + ") does not record audio.");
				((RecordAudio)s).record(active);
			}};
		playCmd = new Command(TOGGLE_PLAY.name, TOGGLE_PLAY.desc, loopProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = midiData2 > 0;
				try { active = Boolean.parseBoolean(props.get(Command.ACTIVE_PARAM).toString());
				} catch (Throwable t) {  }
				int idx = getLoopNum(props);
				if (idx == ALL)
					for (Sample loop : mixer.getSamples()) 
						loop.play(active);
				else 
					mixer.getSamples().get(idx).play(active);
			}};
			
		muteCommand = new Command(CHANNEL.name, CHANNEL.desc, muteProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean mute = Boolean.parseBoolean(props.get("mute").toString());
				String channel = props.get(CHANNEL_PARAM).toString();
				int loopNum = getLoopNum(props);
				if (loopNum == ALL)
					for (Sample loop : mixer.getSamples()) 
						((Recorder)loop).mute(channel, mute);
				else 
					((Recorder)mixer.getSamples().get(loopNum)).mute(channel, mute);
				return;
			}};
		clearCommand = new Command(CLEAR.name, CLEAR.desc, loopProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				int idx = getLoopNum(props);
				if (idx == ALL)
					mixer.getSamples().forEach(loop -> loop.clear());
				else 
					mixer.getSamples().get(idx).clear();
			}};
			
		gainCommand = new Command(GAIN.name, GAIN.desc, gainProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				float gain = ((midiData2))* 0.01f;
				boolean isInput = Boolean.parseBoolean(props.get(IS_INPUT).toString());
				int idx = Integer.parseInt(props.get(INDEX).toString());
				log.debug((isInput ? JudahZone.getChannels().get(idx).getName() : mixer.getSamples().get(idx).getName()) 
						+ " gain: " + gain);
				if (isInput) 
					JudahZone.getChannels().get(idx).setGain(gain);
				else
					mixer.getSamples().get(idx).setGain(gain);
				mixer.getGui().update();
			}};

		loadSample = new Command(LOAD_SAMPLE.name, LOAD_SAMPLE.desc, sampleProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				loadSample(props);}};
		
		addAll(Arrays.asList(new Command[] {
			recordCmd, playCmd, clearCommand, loadSample, gainCommand, muteCommand
		}));
				
	}

	public static HashMap<String, Class<?>> loopProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(LOOP_PARAM, Integer.class);
		commandProps.put(Command.ACTIVE_PARAM, Boolean.class);
		return commandProps;
	}
	
	public static HashMap<String, Class<?>> gainProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(IS_INPUT, Boolean.class);
		commandProps.put(INDEX, Integer.class);
		commandProps.put(GAIN_PROP, Float.class);
		return commandProps;
	}

	public static HashMap<String, Class<?>> muteProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(LOOP_PARAM, Integer.class);
		commandProps.put(Command.ACTIVE_PARAM, Boolean.class);
		commandProps.put(CHANNEL_PARAM, Integer.class);
		return commandProps;
	}

	public static HashMap<String, Class<?>> sampleProps() {
		HashMap<String, Class<?>> commandProps = loopProps();
		commandProps.put(LOOP_PARAM, Integer.class);
		commandProps.put(SOURCE_LOOP, Integer.class);
		commandProps.put(SOURCE_FILE, Integer.class);
		commandProps.put(LOOP_DUPLICATE, Boolean.class);
		return commandProps;
	}

	private int getLoopNum(HashMap<String, Object> props) {
		int idx = ALL;
		try {
			idx = Integer.parseInt(props.get(LOOP_PARAM).toString());
			if (idx < 0 || idx >= mixer.getSamples().size()) throw new Exception();
		} catch (Throwable t) { }
		return idx;
	}
	
	private void loadSample(HashMap<String, Object> props) throws JudahException {
		 
		Object file = props.get(SOURCE_FILE);
		Object loop = props.get(LOOP_PARAM);
		if (file != null && file.toString().length() > 0) {
			try {
				mixer.getSamples().get(Integer.parseInt(loop.toString()))
					.setRecording(Recording.readAudio(file.toString()));
			} catch (Throwable t) {
				throw new JudahException(t);
			}
			return;
		}
		else {
			// TODO, quick implement an empty loop[1] based on size of loop[0] for now
			Object sauce = props.get(SOURCE_LOOP);
			Sample source = mixer.getSamples().get(0);
			if (sauce != null && StringUtils.isNumeric(sauce.toString()))
				source = mixer.getSamples().get(Integer.parseInt(sauce.toString()));
			Sample destination = mixer.getSamples().get(0);
			if (LOOP_PARAM != null && StringUtils.isNumeric(loop.toString()))
				destination = mixer.getSamples().get(Integer.parseInt(loop.toString()));
			if (!source.hasRecording()) {
				log.error("No recording in Loop " + source.getName());
				return;
			}
			if (destination.equals(source)) {
				// DUPLICATE source
				Recording result = new Recording(source.getRecording().size(), true);
				result.addAll(source.getRecording());
				result.addAll(source.getRecording());
				destination.setRecording(result);
				// destination.setRecording(new Recording(source.getRecording().size() * 2, true));
			}
			else { // create blank recording of source's size in destination
				destination.setRecording(new Recording(source.getRecording().size(), true));
			}
			return;
		}
	}
	
}

//TODO

//toggleLoopRecord = new Command(TOGGLE_LOOP.name, TOGGLE_LOOP.desc, new HashMap<>()) {
//	@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
//		// TODO
//	}};

//	final Command pluginCommand;
//	final Command undoCommand;
//	final Command redoCommand;
//	private float masterGain = 1f;  
//props.put(CHANNEL_PROP, Integer.class); // -1 = main
//props.put(PLUGIN_PROP, String.class); // plugin name;
//pluginCommand = new Command(PLUGIN_COMMAND, this, "Load the given plugin on the given channel");
//commands.add(pluginCommand);
//	new Command("Pan", this, "Adjust L/R pan, pan parameter between -1 and 1"); 
//commandProps.put("Active", Boolean.class);
//cmdToggleMode = new Command("Toggle Mode", this, commandProps, "Play either loop 1 or 2, not at the same time. (verse/chorus)");
//cmdReset = new Command("Reset", this, "Clear all recordings");
//cmdUndo = new Command("Undo", this, "Undo the last record or overdub");
