package net.judah.mixer;

import static net.judah.mixer.MixerCommands.Labels.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.judah.jack.RecordAudio;
import net.judah.looper.Sample;
import net.judah.settings.Command;
import net.judah.settings.DynamicCommand;
import net.judah.util.JudahException;

@Log4j
public class MixerCommands extends ArrayList<Command> {
	
	@RequiredArgsConstructor
	static enum Labels {
		TOGGLE_RECORD("Record start/stop"),
		TOGGLE_PLAY("Sample start/stop"),
		UNDO("undo recording"), 
		REDO("redo recording"), 
		CLEAR("clear looper"), 
		CHANNEL("record channel");
		@Getter private final String label;
	}

	
	protected final Mixer mixer;
	
	public static final String GAIN_PROP = "Gain";
	public static final String INDEX = "Index";
	public static final String IS_INPUT = "isInput";
	public static final String CHANNEL_PROP = "Channel";
	public static final String PLUGIN_PROP = "Plugin Name";
	public static final String GAIN_COMMAND = "Mixer Gain";
	public static final String PLUGIN_COMMAND = "Load Plugin";
	public static final String LOOP_PARAM = "Loop";
	public static final String CHANNEL_PARAM = "Channel";
	
	protected final Command gainCommand;
	protected final DynamicCommand playCmd;
	protected final DynamicCommand recordCmd;
	
	protected final Command clearCommand;
	protected final Command muteCommand;

	//	final Command pluginCommand;
	//	final Command undoCommand;
	//	final Command redoCommand;
	//	private float masterGain = 1f;  
	
	MixerCommands(Mixer mixer) {
		this.mixer = mixer;

		recordCmd = new DynamicCommand(TOGGLE_RECORD.getLabel(), mixer, loopProps(),
				"Activate/deactivate recording on the provided looper number.") {
					@Override public void processMidi(int data2, HashMap<String, Object> props) {
						props.put(ACTIVE_PARAM, data2 > 0); }};
		playCmd = new DynamicCommand(TOGGLE_PLAY.getLabel(), mixer, loopProps(), 
				"Activate/deactivate playing a recorded loop with the provided Sample number") {
					@Override public void processMidi(int data2, HashMap<String, Object> props) {
						props.put(ACTIVE_PARAM, data2 > 0); }};
			
		muteCommand = new Command(CHANNEL.getLabel(), mixer, channelProps(),
				"Mute/unmute the recording of a given looper channel");
		clearCommand = new Command(CLEAR.getLabel(), mixer, loopProps(),
				"Reset the given looper");
		
		add(recordCmd);
		add(playCmd);
		add(muteCommand);
		add(clearCommand);
		
		gainCommand = new DynamicCommand(GAIN_COMMAND, mixer, gainProps(), "Adjust loop or input gain between 0 and 1") {
			@Override public void processMidi(int data2, HashMap<String, Object> props) {
						props.put(GAIN_PROP, ((data2 - 1))* 0.01f); }};
		add(gainCommand);
		
//  	TODO		
//		props.put(CHANNEL_PROP, Integer.class); // -1 = main
//		props.put(PLUGIN_PROP, String.class); // plugin name;
//		pluginCommand = new Command(PLUGIN_COMMAND, this, "Load the given plugin on the given channel");
//		commands.add(pluginCommand);
// 		new Command("Pan", this, "Adjust L/R pan, pan parameter between -1 and 1"); 
//		commandProps.put("Active", Boolean.class);
//		cmdToggleMode = new Command("Toggle Mode", this, commandProps, "Play either loop 1 or 2, not at the same time. (verse/chorus)");
//		cmdReset = new Command("Reset", this, "Clear all recordings");
//		cmdUndo = new Command("Undo", this, "Undo the last record or overdub");
	}

	private HashMap<String, Class<?>> loopProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(LOOP_PARAM, Integer.class);
		commandProps.put(Command.ACTIVE_PARAM, Boolean.class);
		return commandProps;
	}
	
	private HashMap<String, Class<?>> gainProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(IS_INPUT, Boolean.class);
		commandProps.put(INDEX, Integer.class);
		commandProps.put(GAIN_PROP, Float.class);
		return commandProps;
	}

	private HashMap<String, Class<?>> channelProps() {
		HashMap<String, Class<?>> commandProps = loopProps();
		commandProps.put(CHANNEL_PARAM, Integer.class);
		return commandProps;
	}

	public void execute(Command cmd, HashMap<String, Object> props) throws JudahException {
		
		if (cmd.getName().equals(GAIN_COMMAND)) {
			gainCommand(cmd, props);
			return;
		} 
		
		List<Sample> loops = mixer.getSamples();
		boolean all = false;
		int idx = -1;
		try {
			idx = Integer.parseInt(props.get(LOOP_PARAM).toString());
			if (idx < 0 || idx >= loops.size()) throw new Exception();
		} catch (Throwable t) {
			all = true;
		}
		if (cmd.equals(clearCommand)) {
			if (all) 
				for (Sample loop : loops)
					loop.clear();
			else 
				loops.get(idx).clear();
			return;
		} 
		
		boolean active = Boolean.parseBoolean(props.get(Command.ACTIVE_PARAM).toString());
		if (cmd.equals(recordCmd)) {
			Sample s = loops.get(idx);
			if (false == s instanceof RecordAudio) 
				throw new JudahException("Sample " + idx + " (" + s.getName() + ") does not record audio.");
			((RecordAudio)s).record(active);
			
		} else if (cmd.equals(playCmd)) {
			if (all)
				for (Sample loop : loops) 
					loop.play(active);
			else 
				loops.get(idx).play(active);
		// TODO Mute
		//	else if (cmd.equals(muteCommand)) {
		//		int ch = Integer.parseInt(props.get(CHANNEL_PARAM).toString());
		//		if (all) 
		//			for (Sample loop : loops)
		//				loop.channel(ch, active);
		//		else 
		//			loops.get(idx).channel(ch, active);
		//	else if (cmd.equals(cmdUndo)) { }
		//	else if (cmd.equals(cmdToggleMode)) { }

		} else throw new JudahException("Unknown command: " + cmd);
	}

	private void gainCommand(Command cmd, HashMap<String, Object> props) throws JudahException {
		if (!props.containsKey(GAIN_PROP)) throw new JudahException("No Volume. " + cmd + " " );
		float gain = (Float)props.get(GAIN_PROP);
		boolean isInput = Boolean.parseBoolean(props.get(IS_INPUT).toString());
		int idx = Integer.parseInt(props.get(INDEX).toString());
		log.debug((isInput ? mixer.getChannels().get(idx).getName() : mixer.getSamples().get(idx).getName()) 
				+ " gain: " + gain);
		if (isInput) 
			mixer.getChannels().get(idx).setGain(gain);
		else
			mixer.getSamples().get(idx).setGain(gain);
		mixer.getGui().update();
	}


}
