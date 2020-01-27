package net.judah.instruments;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import net.judah.Tab;
import net.judah.settings.Command;
import net.judah.settings.Service;

/** shell:  rakarrack --Load [File /home/judah/Tracks/Ramarok/JudahZone.rkr] */

public class Rakarrack implements Service {

	private static final String clientName = "rakarrack:";
	public static final String IN_L= clientName + "in_1";
	public static final String IN_R = clientName + "in_2";
	public static final String OUT_L = clientName + "out_1";
	public static final String OUT_R = clientName + "out_2";

	private Process process;
	public static final String SHELL_COMMAND = "rakarrack -l ";
	private final String cmd;


	public Rakarrack(String file) throws IOException {
		cmd = SHELL_COMMAND + file;
		process = Runtime.getRuntime().exec(cmd);
	}

	@Override
	public String getServiceName() {
		return Rakarrack.class.getSimpleName();
	}


	@Override
	public List<Command> getCommands() {
		return Collections.emptyList(); // TODO
	}


	@Override
	public void execute(Command cmd, Properties props) throws Exception {
		// TODO Auto-generated method stub

	}


	@Override
	public void close() {
		process.destroy();
	}

	@Override
	public Tab getGui() {
		// TODO Auto-generated method stub
		return null;
	}


}
