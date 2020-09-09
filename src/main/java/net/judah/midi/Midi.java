package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.extern.log4j.Log4j;



/** serialization isn't working, save as byte[] */
@Log4j
public class Midi extends ShortMessage {

	public Midi(byte[] bytes) {
		super(bytes);
	}

	/** @see javax.sound.midi.ShortMessage#setMessage(int, int, int, int) */
	public Midi(int command, int channel, int data1, int data2) throws InvalidMidiDataException {
		super(command, channel, data1, data2);
	}

	public Midi(int command, int channel, int data1) throws InvalidMidiDataException {
		super(command, channel, data1, 0);
	}

	public Midi(int command, int channel) throws InvalidMidiDataException {
		super(command, channel, 0, 0);
	}


	@Override
	public String toString() {
		
        StringBuffer sb = new StringBuffer();
        if (data == null || data.length == 0) return "";
        sb.append( (data[0] - getChannel()) & 0xFF );
        
        for (int j = 1; j < getLength(); j++) {
            sb.append(".").append(data[j] & 0xFF);
        }
        return sb.append("/").append(getChannel()).toString();
	}

	@Override
	public int hashCode() {
		int result = 0;
		for (byte b: getMessage()) {
			result += b;
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if ( obj instanceof Midi == false) {
//			System.err.println("not Midi " + obj + ":" + obj.getClass().getName());
			return false;
		}
		Midi other = (Midi) obj;

//		System.out.println("Other command: " + other.getCommand() +
//				" data1: " + other.getData1() + " data2: " + other.getData2());

		boolean result = this.getChannel() == other.getChannel() &&
				this.getCommand() == other.getCommand() &&
				this.getData1() == other.getData1() &&
				this.getData2() == other.getData2();

//		System.out.println("start equals? " + obj + " vs. " + this + " = " + result);
		return result;
	}

	/** equal, ignoring data2 */
	public boolean matches(ShortMessage midi) {
		return getChannel() == midi.getChannel() && getCommand() == midi.getCommand() && getData1() == midi.getData1();
	}

	public void setVelocity(int velocity) {
		try {
			setMessage(getCommand(), getChannel(), getData1(), velocity);
		} catch (InvalidMidiDataException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public static boolean isCC(ShortMessage msg) {
		 return msg.getStatus() - msg.getChannel() == ShortMessage.CONTROL_CHANGE;
	}
	
	public static boolean isNote(ShortMessage msg) {
		int stat = msg.getStatus() - msg.getChannel();
		return stat == Midi.NOTE_OFF || stat == NOTE_ON; 
	}
	
}

/* sys message
11111010	none	 	Start (song)
11111011	none	 	Stop
 */

//	public static void main(String[] args) {
//		/*
//		Pedal Event: 176 . 101 . 127 (0)
//		Pedal Event: 176 . 101 . 0 (0)
//		Pedal Event: 176 . 100 . 127 (0)
//		Pedal Event: 176 . 100 . 0 (0)
//		Pedal Event: 176 . 99 . 127 (0)
//		Pedal Event: 176 . 99 . 0 (0)
//		Pedal Event: 176 . 98 . 127 (0)
//		Pedal Event: 176 . 98 . 0 (0)
//		Pedal Event: 176 . 97 . 127 (0)
//		Pedal Event: 176 . 97 . 0 (0)
//		Pedal Event: 176 . 96 . 127 (0)
//		Pedal Event: 176 . 96 . 0 (0) */
//try {
//		Midi m;
//		m = new Midi(176, 0, 101, 127);
//		System.out.println("176, 101, 127 ???????     " + m + " vs. " + m.getCommand() + "+" + m.getData1() + "+" + m.getData2());
//		System.out.println("176, 0, 96, 0 ???????     " + new Midi(176, 0, 96, 0));
//} catch (InvalidMidiDataException e) {
//	e.printStackTrace();
//}
//	}
//}
