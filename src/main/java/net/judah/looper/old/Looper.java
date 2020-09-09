package net.judah.looper.old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.jack.ClientConfig;
import net.judah.looper.old.LoopSettings.Kickoff;
import net.judah.looper.old.LoopSettings.PostRecordAction;
import net.judah.metronome.Quantization;
import net.judah.settings.Command;
import net.judah.settings.Service;
import net.judah.util.JudahException;
import net.judah.util.Tab;

@Log4j
// TODO features: toggle play loop 1 or 2 (verse vs. chorus)
public class Looper implements Service {

	public static final String SERVICE_NAME = "Looper";
	static final LoopSettings defaultMasterSettings = new LoopSettings(
			"Master", MasterLoop.class, 0, 0, 0, new Quantization(), false,
			Kickoff.PressRecord, PostRecordAction.DUB_AND_PLAY, true, false, true);
	static final LoopSettings defaultSlaveSettings = new LoopSettings(
			"Slave", SlaveLoop.class, 0, 0, 0, null, false,
			Kickoff.PressRecord, PostRecordAction.NOTHING, false, false, true);

	final LooperUI UI;
	/** Looper unique name. */
	@Getter private final String name;
	@Getter private LoopCommand status = LoopCommand.STOP;
	Command cmdPlay, cmdRecord, cmdStop, cmdReset, cmdUndo, cmdToggleMode;
	private final List<Command> commands = new ArrayList<>();
	private HashMap<String, Class<?>> commandProps;
	@Getter private ArrayList<GLoop> loops = new ArrayList<>();

    public Looper(String label, List<LoopSettings> initialSetup) throws JackException {

    	this.name = label;

    	initCommands();

		for (LoopSettings settings: initialSetup) {
			Class<?> clazz = settings.getClass();
			if (MasterLoop.class.equals(clazz))
				loops.add(new MasterLoop(this, new ClientConfig("Loop Master"), settings));
			else if (SlaveLoop.class.equals(clazz))
				loops.add(new SlaveLoop(this, new ClientConfig("loop Slave"), settings));
		}

		if (loops.isEmpty()) { // bootstrap 2? loops
			loops.add(new MasterLoop(this, new ClientConfig("Loop Master"), defaultMasterSettings));
			loops.add(new SlaveLoop(this, new ClientConfig("Loop Slave"), defaultSlaveSettings));
		}
		log.warn("Loop size(2): " + loops.size());
    	UI = new LooperUI(label, this);

	}

    public GLoop getMasterLoop() {
    	for (GLoop loop : loops) {
    		if (loop.isMaster())
    			return loop;
    	}
    	return null;
    }

	@Override
	public List<Command> getCommands() {
		return commands;
	}

	@Override
	public void execute(Command cmd, Properties props) throws Exception {
		log.debug("looper execute " + cmd + " " + Command.toString(props));
		HashMap<String, Class<?>> types = cmd.getProps();
		for (String key : types.keySet()) {
			if (!props.containsKey(key))
				throw new JudahException("for property: " + key + " (" + types.get(key) + ") " + Arrays.toString(props.keySet().toArray()));
		}
		if (cmd.equals(cmdRecord)) {
			int idx = Integer.parseInt(props.get("Loop").toString());
			if (idx < loops.size() && idx >= 0)
				loops.get(idx).setRecord(Boolean.parseBoolean(props.get("Active").toString()));
		}
		else if (cmd.equals(cmdPlay)) {
			int idx = Integer.parseInt(props.get("Loop").toString());
			if (idx < loops.size() && idx >= 0)
				loops.get(idx).setPlay(Boolean.parseBoolean(props.get("Active").toString()));
		}
		else if (cmd.equals(cmdReset)) {
			for (GLoop loop : loops)
				loop.clear();
		}
		else if (cmd.equals(cmdUndo)) {
			// TODO
		}

		else if (cmd.equals(cmdToggleMode)) {
			// TODO
		}
	}


	private HashMap<String, Class<?>> channelProps() {
		if (commandProps == null) {
			commandProps = new HashMap<>();
			commandProps.put("Loop", Integer.class);
			commandProps.put("Active", Boolean.class);
		}
		return commandProps;
	}

    private void initCommands() {
    	// RECORD("record"), PLAY("play"), STOP("stop"), DUB("dub"), PAUSE("pause") /*TODO*/, CONTINUE("continue") /*TODO*/;
    	cmdPlay = new Command(LoopCommand.PLAY.txt, this, channelProps(),
    			"Play a recorded loop on the provided loop number");
    	cmdRecord = new Command(LoopCommand.RECORD.txt, this, channelProps(),
    			"Record on the provided loop number.");
    	cmdStop = new Command(LoopCommand.STOP.txt, this, channelProps(),
    			"Stop playing a loop or stop recording on the given loop number.");

    	HashMap<String, Class<?>> commandProps = new HashMap<>();
		commandProps.put("Active", Boolean.class);
    	cmdToggleMode = new Command("Toggle Mode", this, commandProps, "Play either loop 1 or 2, not at the same time. (verse/chorus)");

    	cmdReset = new Command("Reset", this, "Clear all recordings");
    	cmdUndo = new Command("Undo", this, "Undo the last record or overdub");
    	commands.add(cmdPlay);
    	commands.add(cmdRecord);
    	commands.add(cmdStop);
    	commands.add(cmdReset);
    	commands.add(cmdUndo);
    	commands.add(cmdToggleMode);
	}

    @Override
    public void close() {
    	for (GLoop loop : loops)
    		loop.close();
    }

	@Override public Tab getGui() {
		return UI; }

	@Override public String getServiceName() {
		return SERVICE_NAME; }
}



