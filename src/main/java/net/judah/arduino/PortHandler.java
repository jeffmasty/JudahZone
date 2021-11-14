package net.judah.arduino;

import java.io.InputStream;
import java.io.OutputStream;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class PortHandler implements SerialPortEventListener {
	
 int[] packet = new int[4];
	
 private SerialPort serialPort = null;
 //input and output streams for sending and receiving data
 private InputStream input = null;
 private OutputStream output = null;
 //just a boolean flag that i use for enabling
 //and disabling buttons depending on whether the program
 //is connected to a serial port or not
 private boolean bConnected = false;
 //the timeout value for connecting with the port
 final static int TIMEOUT = 2000;
 //some ascii values for for certain things
 final static int SPACE_ASCII = 32;
 final static int DASH_ASCII = 45;
 final static int NEW_LINE_ASCII = 10;
 
 public static void main(String[] args) {
	 PortHandler o = new PortHandler();
	 
	 o.openAndSetSerialPort("/dev/ttyACM0");
	 //o.silentClose();
 }
 private void silentClose() {
 try {
     serialPort.closePort();
          System.out.println("port closed");
   } catch (SerialPortException ex) {
       ex.printStackTrace();
   }
 }
 
  public void openAndSetSerialPort(String port) {
   serialPort = new SerialPort(port);
   try {
	   
	   
	   
     System.out.println("Port opened: " + serialPort.openPort());
     System.out.println("Port params: " + serialPort.setParams(SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, 1, 0));
     serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
     Thread.sleep(1000);
     serialPort.addEventListener(this);
     
     serialPort.writeBytes("hello, world\n".getBytes());
     Thread.sleep(100);
     serialPort.writeInt(12);
     Thread.sleep(100);
     serialPort.writeString("hello2\n");
     Thread.sleep(100);
serialPort.writeBytes("hello, world3\n".getBytes());
     //     String back = null;
     while (true) {
    	 
    	 
    	 
     }
//    		 back = serialPort.readString();
//     if (back != null) System.out.println("back: " + back);
//     }
     
   } catch (SerialPortException e) {
     e.printStackTrace();
   } catch (InterruptedException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
   } finally {
	   silentClose();
   }

 }
@Override
public void serialEvent(SerialPortEvent event) {
 if(event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                String receivedData = serialPort.readString(event.getEventValue());
                System.out.println("Received response: " + receivedData);
            }
            catch (SerialPortException ex) {
                System.out.println("Error in receiving string from COM-port: " + ex);
            }
        }	
}
 
 
}
