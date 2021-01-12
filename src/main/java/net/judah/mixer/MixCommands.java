package net.judah.mixer;
import static net.judah.settings.Commands.MixerLbls.*;
import static net.judah.util.Constants.Param.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.Command;
import net.judah.jack.RecordAudio;
import net.judah.looper.AudioPlay;
import net.judah.looper.Recorder;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.plugin.LineType;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahException;

@Log4j
public class MixCommands extends ArrayList<Command> {
	
	
	public static final String IS_INPUT = "isInput";
	public static final String CHANNEL_PROP = "channel";
	public static final String PLUGIN_PROP = "Plugin Name";
	public static final String PLUGIN_COMMAND = "Load Plugin";
	public static final String SOURCE_LOOP = "source.loop";
	public static final String DESTINATION_LOOP = "destination.loop";
	public static final String LOOP_DUPLICATE = "loop.duplicate";
	public static final int ALL = -1;
	
	@Getter protected final Command playCmd;
	@Getter protected final Command recordCmd;
	
	public MixCommands() {
		
		add(new Command(DRUMTRACK.name, DRUMTRACK.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				JudahZone.getLooper().getDrumTrack().toggle();
			}
		});
		
		recordCmd = new Command(TOGGLE_RECORD.name, TOGGLE_RECORD.desc, loopProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = false;
				if (midiData2 < 0)
					try { 
						active = Boolean.parseBoolean(props.get(ACTIVE).toString());
					} catch (Throwable t) {Console.warn(ACTIVE + " not set. " + Constants.prettyPrint(props), t);  }
				else 
					active = midiData2 > 0;
				int idx = getLoopNum(props); 
				Sample s = JudahZone.getLooper().get(idx);
				if (false == s instanceof RecordAudio) 
					throw new JudahException("Sample " + idx + " (" + s.getName() + ") does not record audio.");
				((RecordAudio)s).record(active);
			}};
		add(recordCmd);
		
		playCmd = new Command(TOGGLE_PLAY.name, TOGGLE_PLAY.desc, loopProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = false;
				if (midiData2 < 0)
					try { active = Boolean.parseBoolean(props.get(ACTIVE).toString());
					} catch (Throwable t) {Console.warn(ACTIVE + " not set. " + 
							Constants.prettyPrint(props), t);}
				else 
					active = midiData2 > 0;
				int idx = getLoopNum(props);
				if (idx == ALL)
					for (Sample loop : JudahZone.getLooper()) 
						loop.play(active);
				else 
					JudahZone.getLooper().get(idx).play(active);
			}};
		add(playCmd);
		
		add(new Command(MUTE.name, MUTE.desc, muteProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				// String channel = props.get(CHANNEL).toString();
				int loopNum = getLoopNum(props);
				
				boolean mute = false;
				if (midiData2 >= 0)
					mute = midiData2 > 0;
				else mute = parseActive(props); 
					
				if (loopNum == ALL)
					for (Sample loop : JudahZone.getLooper()) 
						((Recorder)loop).setOnMute(mute);
				else 
					((Recorder)JudahZone.getLooper().get(loopNum)).setOnMute(mute);
				return;
			}});
		add(new Command(CLEAR.name, CLEAR.desc, loopProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				int idx = getLoopNum(props);
				if (idx == ALL)
					JudahZone.getLooper().forEach(loop -> loop.clear());
				else 
					JudahZone.getLooper().get(idx).clear();
			}});
			
		add(new Command(VOLUME.name, VOLUME.desc, gainProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				int volume = 0;
					if (midiData2 >= 0)
						volume = midiData2;
					else volume = Integer.parseInt("" + props.get(GAIN)); 
					
				boolean isInput = Boolean.parseBoolean(props.get(IS_INPUT).toString());
				int idx = Integer.parseInt(props.get(INDEX).toString());
				log.trace((isInput ? JudahZone.getChannels().get(idx).getName() : 
						JudahZone.getLooper().get(idx).getName()) + " gain: " + volume);
				if (isInput) 
					JudahZone.getChannels().get(idx).setVolume(volume);
				else 
					JudahZone.getLooper().get(idx).setVolume(volume);

			}});

		add(new Command(LOAD_SAMPLE.name, LOAD_SAMPLE.desc, sampleProps()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				loadSample(props);}});
		
//		add(new Command(PLUGIN.name, PLUGIN.desc, pluginTemplate()) {
//			@Override
//			public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
//				String name = props.get(NAME).toString();
//				int index = Integer.parseInt(props.get(INDEX).toString());
//				LineType type = LineType.valueOf(props.get(TYPE).toString());
//				JudahZone.getPlugins().add(new Plugin(name, index, type));
//			}});
		
		add(new AudioPlay());

	}

	public static HashMap<String, Class<?>> pluginTemplate() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(NAME, String.class);
		result.put(INDEX, Integer.class);
		result.put(TYPE, LineType.class);
		return result;
	}
	
	public static HashMap<String, Class<?>> loopProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(LOOP, Integer.class);
		commandProps.put(ACTIVE, Boolean.class);
		return commandProps;
	}
	
	public static HashMap<String, Class<?>> gainProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(IS_INPUT, Boolean.class);
		commandProps.put(INDEX, Integer.class);
		commandProps.put(GAIN, Float.class);
		return commandProps;
	}

	public static HashMap<String, Class<?>> muteProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(LOOP, Integer.class);
		commandProps.put(ACTIVE, Boolean.class);
		commandProps.put(CHANNEL, Integer.class);
		return commandProps;
	}

	public static HashMap<String, Class<?>> sampleProps() {
		HashMap<String, Class<?>> commandProps = loopProps();
		commandProps.put(LOOP, Integer.class);
		commandProps.put(SOURCE_LOOP, Integer.class);
		commandProps.put(FILE, Integer.class);
		commandProps.put(LOOP_DUPLICATE, Boolean.class);
		return commandProps;
	}

	private int getLoopNum(HashMap<String, Object> props) {
		int idx = ALL;
		try {
			idx = Integer.parseInt(props.get(LOOP).toString());
			if (idx < 0 || idx >= JudahZone.getLooper().size()) throw new Exception();
		} catch (Throwable t) { }
		return idx;
	}
	
	private void loadSample(HashMap<String, Object> props) throws JudahException {
		 
		Object file = props.get(FILE);
		Object loop = props.get(LOOP);
		if (file != null && file.toString().length() > 0) {
			try {
				JudahZone.getLooper().get(Integer.parseInt(loop.toString()))
					.setRecording(Recording.readAudio(file.toString()));
			} catch (Throwable t) {
				throw new JudahException(t);
			}
			return;
		}
		else {
			// TODO, quick implement an empty loop[1] based on size of loop[0] for now
			Object sauce = props.get(SOURCE_LOOP);
			Sample source = JudahZone.getLooper().get(0);
			if (sauce != null && StringUtils.isNumeric(sauce.toString()))
				source = JudahZone.getLooper().get(Integer.parseInt(sauce.toString()));
			Sample destination = JudahZone.getLooper().get(0);
			if (LOOP != null && StringUtils.isNumeric(loop.toString()))
				destination = JudahZone.getLooper().get(Integer.parseInt(loop.toString()));
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
//	@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {	}};
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
