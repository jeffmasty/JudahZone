package net.judah.mixer;

import static net.judah.looper.LoopInterface.CMD.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.extern.log4j.Log4j;
import net.judah.jack.RecordAudio;
import net.judah.looper.Sample;
import net.judah.settings.Command;
import net.judah.settings.DynamicCommand;
import net.judah.util.JudahException;

@Log4j
public class MixerCommands extends ArrayList<Command> {
	
	protected final Mixer mixer;
	
	public static final String GAIN_PROP = "Gain";
	public static final String INDEX = "Index";
	public static final String IS_INPUT = "isInput";
	public static final String CHANNEL_PROP = "Channel";
	public static final String PLUGIN_PROP = "Plugin Name";
	public static final String GAIN_COMMAND = "Mixer Gain";
	public static final String PLUGIN_COMMAND = "Load Plugin";
	public static final String ACTIVE_PARAM = "Active";
	public static final String LOOP_PARAM = "Loop";
	public static final String CHANNEL_PARAM = "Channel";
	
	protected final Command gainCommand;
	protected final Command playCommand;
	protected final Command recordCommand;
	protected final Command clearCommand;
	protected final Command muteCommand;

	//	final Command pluginCommand;
	//	final Command undoCommand;
	//	final Command redoCommand;
	//	private float masterGain = 1f;  
	
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
		commandProps.put(ACTIVE_PARAM, Boolean.class);
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
		
		boolean active = Boolean.parseBoolean(props.get(ACTIVE_PARAM).toString());
		if (cmd.equals(recordCommand)) {
			Sample s = loops.get(idx);
			if (false == s instanceof RecordAudio) 
				throw new JudahException("Sample " + idx + " (" + s.getName() + ") does not record audio.");
			((RecordAudio)s).record(active);
			
		} else if (cmd.equals(playCommand)) {
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
		log.debug("gain: " + gain + (isInput ? mixer.getChannels().get(idx).getName() : mixer.getSamples().get(idx).getName()));
		if (isInput) 
			mixer.getChannels().get(idx).setGain(gain);
		else
			// TODO
			mixer.getSamples().get(idx).setGain(gain);
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
