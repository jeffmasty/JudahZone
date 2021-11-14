package net.judah.arduino;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.sound.midi.ShortMessage;

import jssc.SerialPort;
import jssc.SerialPortException;
import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Service;

@Log4j
public class ArduinoMidi implements Service {

	private final SerialPort port;			
	byte[] dat = new byte[4];
	
	public ArduinoMidi(String portName) { //"/dev/ttyACM0"
		   port = new SerialPort(portName);
		   try {
		     log.info("Arduino opened: " + port.openPort());
		     System.out.println("Serial params: " + port.setParams(
		    		 SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, 1, 0));
		     port.purgePort(SerialPort.PURGE_RXCLEAR);
		     Thread.sleep(200);
   } catch (SerialPortException e) {
     e.printStackTrace();
   } catch (InterruptedException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
   } 	
}

	public synchronized void send(ShortMessage midi) throws IOException {
		if (port == null || !port.isOpened())
			throw new IOException("no connection");
		try {
			
			dat[0] = (byte)midi.getCommand();
			dat[1] = (byte)midi.getData1();
			dat[2] = (byte)midi.getData2();
					
			port.writeByte((byte)midi.getCommand());
			port.writeByte((byte)midi.getData1());
			port.writeByte((byte)midi.getData2());
			//port.writeBytes(dat);
			
		} catch (SerialPortException e) { throw new IOException(e); }
	}
	
	@Override
	public List<Command> getCommands() {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	@Override
	public void close() {
		try {
			if (port != null) port.closePort();
			log.info("Arduino closed");
		} catch (SerialPortException ex) {
			log.error(ex);
		}
	}

	@Override
	public void properties(HashMap<String, Object> props) {
		// TODO Auto-generated method stub
		
	}
}