package net.judah.midi;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.transport.udp.OSCPortOut;

import net.judah.JudahZone;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** communicates with https://github.com/widdly/midiclock over OSC */ 
public class MidiClock implements Closeable {

	public static final int port = 4040;
	private OSCPortOut out;
	// port communications
	private final ArrayList<Object> dat = new ArrayList<>();

	public MidiClock() throws Exception {
		// TODO check for/start midiclock process...
		// ~/git/midiclock/midiclock -s -t95
		out = new OSCPortOut(InetAddress.getLocalHost(), port);
		JudahZone.getServices().add(this);
		Constants.sleep(40);
	}

	public void start() {
		dat.clear();
		send("/start", dat);
	}
	
	public void stop() {
		dat.clear();
		send("/stop", dat);
	}
	
	public void cont() {
		dat.clear();
		send("/continue", dat);
	}
	
	public void writeTempo(int tempo) {
		dat.clear();
		dat.add(Integer.valueOf(tempo));
		send("/tempo", dat);
	}
	
	private void send(String address, List<Object> params) {
		try {
			if (!out.isConnected())
				out.connect();
			out.send(new OSCMessage(address, params));
		} catch (Exception e) {
			RTLogger.warn(this, "tempo " + e.getMessage());
		}
	}

	@Override
	public void close() {
		if (out != null && out.isConnected()) {
			stop();
			try {
				out.disconnect();
			} catch (IOException e) {
				RTLogger.warn(this, e);
			}
		}
		out = null;
	}
	

	
}