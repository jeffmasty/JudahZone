package net.judah.mixer;

import static net.judah.looper.LoopInterface.CMD.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import lombok.extern.log4j.Log4j;
import net.judah.JudahException;
import net.judah.looper.Loop;
import net.judah.settings.Command;

@SuppressWarnings("serial") @Log4j
public class MixerCommands extends ArrayList<Command> {
	
	protected final Mixer mixer;
	
	public static final String GAIN_PROP = "Gain";
	public static final String CHANNEL_PROP = "Channel";
	public static final String PLUGIN_PROP = "Plugin Name";
	public static final String GAIN_COMMAND = "Channel Gain";
	public static final String PLUGIN_COMMAND = "Load Plugin";

	static final String ACTIVE_PARAM = "Active";
	static final String LOOP_PARAM = "Loop";
	static final String CHANNEL_PARAM = "Channel";
	
	// mixer interface
	final Command gainCommand;
//	final Command pluginCommand;
	
	// loop interface
	
	protected final Command playCommand;
	protected final Command recordCommand;
//	final Command undoCommand;
//	final Command redoCommand;
	protected final Command clearCommand;
	protected final Command muteCommand;

	@SuppressWarnings("unused")
	private float masterGain = 1f;  
	
	MixerCommands(Mixer mixer) {
		this.mixer = mixer;

		recordCommand = new Command(RECORD.getLabel(), mixer, loopProps(),
				"Activate/deactivate recording on the provided looper number.");
		playCommand = new Command(PLAY.getLabel(), mixer, loopProps(), 
				"Activate/deactivate playing a recorded loop on the provided loop number");
		muteCommand = new Command(CHANNEL.getLabel(), mixer, channelProps(),
				"Mute/unmute the recording of a given looper channel");
		clearCommand = new Command(CLEAR.getLabel(), mixer, loopProps(),
				"Reset the given looper");
		
		add(recordCommand);
		add(playCommand);
		add(muteCommand);
		add(clearCommand);
		
		gainCommand = new Command(GAIN_COMMAND, mixer, gainProps(), "Adjust gain, gain parameter between 0 and 1");
		add(gainCommand);
		
//		props = new HashMap<>();
//		props.put(CHANNEL_PROP, Integer.class); // -1 = main
//		props.put(PLUGIN_PROP, String.class); // plugin name;
//		pluginCommand = new Command(PLUGIN_COMMAND, this, "Load the given plugin on the given channel");
//		commands.add(pluginCommand);
		// new Command("Pan", this, "Adjust L/R pan, pan parameter between -1 and 1"); // TODO

//		// RECORD("record"), PLAY("play"), STOP("stop"), DUB("dub"), PAUSE("pause") /*TODO*/, CONTINUE("continue") /*TODO*/;
//		cmdPlay = new Command("Play", this, channelProps(),
//				"Play a recorded loop on the provided loop number");
//		cmdRecord = new Command("Record", this, channelProps(),
//				"Record on the provided loop number.");
//		cmdStop = new Command("Stop", this, channelProps(),
//				"Stop playing a loop or stop recording on the given loop number.");
	//
//		HashMap<String, Class<?>> commandProps = new HashMap<>();
//		commandProps.put("Active", Boolean.class);
//		cmdToggleMode = new Command("Toggle Mode", this, commandProps, "Play either loop 1 or 2, not at the same time. (verse/chorus)");
	//
//		cmdReset = new Command("Reset", this, "Clear all recordings");
//		cmdUndo = new Command("Undo", this, "Undo the last record or overdub");
//		commands.add(cmdPlay);
//		commands.add(cmdRecord);
//		commands.add(cmdStop);
//		commands.add(cmdReset);
//		commands.add(cmdUndo);
//		commands.add(cmdToggleMode);

	}

	private HashMap<String, Class<?>> loopProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(LOOP_PARAM, Integer.class);
		commandProps.put(ACTIVE_PARAM, Boolean.class);
		return commandProps;
	}
	
	private HashMap<String, Class<?>> gainProps() {
		HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put(LOOP_PARAM, Integer.class);
		commandProps.put(ACTIVE_PARAM, Boolean.class);
		return commandProps;
	}

	private HashMap<String, Class<?>> channelProps() {
		HashMap<String, Class<?>> commandProps = loopProps();
		commandProps.put(CHANNEL_PARAM, Integer.class);
		return commandProps;
	}

	public void execute(Command cmd, Properties props) throws JudahException {
		
				if (cmd.getName().equals(GAIN_COMMAND)) {
					gainCommand(cmd, props);
					return;
				} 
		// 		if (cmd.getName().equals(PLUGIN_COMMAND)) {
		//			pluginCommand(cmd, props);
		//		} else throw new JudahException("Unknown Command: " + cmd);

		
		List<Loop> loops = mixer.getLoops();
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
				for (Loop loop : loops)
					loop.clear();
			else 
				loops.get(idx).clear();
			return;
		} 
		
		boolean active = Boolean.parseBoolean(props.get(ACTIVE_PARAM).toString());
		if (cmd.equals(recordCommand)) {
			loops.get(idx).record(active);
		} else if (cmd.equals(playCommand)) {
			loops.get(idx).play(active);
		} else if (cmd.equals(muteCommand)) {
			int ch = Integer.parseInt(props.get(CHANNEL_PARAM).toString());
			if (all) 
				for (Loop loop : loops)
					loop.channel(ch, active);
			else 
				loops.get(idx).channel(ch, active);
		} else throw new JudahException("Unknown command: " + cmd);
	}
//	else if (cmd.equals(cmdUndo)) {
//		// TODO
//	}
//
//	else if (cmd.equals(cmdToggleMode)) {
//		// TODO
//	}

	@SuppressWarnings("unused")
	private void gainCommand(Command cmd, Properties props) throws JudahException {
		if (!props.containsKey(GAIN_PROP)) throw new JudahException("No Volume. " + cmd + " " );
		float gain = (Float)props.get(GAIN_PROP);
		Object o = props.get(CHANNEL_PROP);
		if (o == null) {
			masterGain = gain;
			return;
		}

		log.debug("channel " + o + " gain: " + gain);
		int idx = (Integer)o;
		
		mixer.getChannels().get(idx).setGain(gain);
		mixer.getGui().update();
		
//		if (idx >= 0 && idx < inputPorts.size()) {
//			MixerPort p = inputPorts.get(idx);
//			p.setGain(gain);
//			if (p.isStereo()) {
//				if (p.getType() == Type.LEFT) 
//					inputPorts.get(idx + 1).setGain(gain);
//				else 
//					inputPorts.get(idx -1).setGain(gain);
//			}
//		}
//		else masterGain = gain;
		
	}


}
