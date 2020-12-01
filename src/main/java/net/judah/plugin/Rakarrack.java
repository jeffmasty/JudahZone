package net.judah.plugin;

import java.io.File;
import java.io.IOException;

import net.judah.JudahZone;

/** shell:  rakarrack --Load [File /home/judah/Tracks/Ramarok/JudahZone.rkr] */

public class Rakarrack /* implements Service */ {

    public static final File RAKARRACK_SETTINGS = 
    		new File(JudahZone.class.getClassLoader().getResource("JudahZone.rkr").getFile());
	
	private static final String clientName = "rakarrack:";
	public static final String IN_L= clientName + "in_1";
	public static final String IN_R = clientName + "in_2";
	public static final String OUT_L = clientName + "out_1";
	public static final String OUT_R = clientName + "out_2";

	private Process process;
	public static final String SHELL_COMMAND = "rakarrack -l ";
	private final String cmd;


	public Rakarrack() throws IOException {
		this (RAKARRACK_SETTINGS);
	}
	
	public Rakarrack(File settings) throws IOException {
		cmd = SHELL_COMMAND + settings.getAbsolutePath();
		process = Runtime.getRuntime().exec(cmd);
	}

//	@Override
//	public String getServiceName() {
//		return Rakarrack.class.getSimpleName();
//	}
//
//
//	@Override
//	public List<Command> getCommands() {
//		return Collections.emptyList(); // TODO
//	}
//
//
//	@Override
//	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
//		// TODO Auto-generated method stub
//
//	}
//
//
	/* @Override */
	public void close() {
		process.destroy();
	}


}
