package net.judah.midi;

import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

/** serialization isn't working, save as byte[] */
public class Midi extends ShortMessage {

	public static final String PARAM_COMMAND = "command";
	public static final String PARAM_CHANNEL = "channel";
	public static final String PARAM_DATA1 = "data1";
	public static final String PARAM_DATA2 = "data2";
	
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
		
        StringBuffer sb = new StringBuffer("" + getCommand());
        if (getLength() > 1)
        	sb.append(".").append(getData1());
        if (getLength() > 2)
        	sb.append(".").append(getData2());
        return sb.append("/").append(getChannel()).toString();
//        if (data == null || data.length == 0) return "";
//        sb.append( (data[0] - getChannel()) & 0xFF );
//        for (int j = 1; j < getLength(); j++) {
//            sb.append(".").append(data[j] & 0xFF);
//        }
//        return sb.append("/").append(getChannel()).toString();
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
			return false;
		}
		Midi other = (Midi) obj;


		boolean result = this.getChannel() == other.getChannel() &&
				this.getCommand() == other.getCommand() &&
				this.getData1() == other.getData1() &&
				this.getData2() == other.getData2();

		return result;
	}

	/** equal, ignoring data2 */
	public boolean matches(ShortMessage midi) {
		return getChannel() == midi.getChannel() && getCommand() == midi.getCommand() && getData1() == midi.getData1();
	}

	public void setVelocity(int velocity) throws InvalidMidiDataException {
		setMessage(getCommand(), getChannel(), getData1(), velocity);
	}
	
	public static boolean isCC(ShortMessage msg) {
		 return msg.getStatus() - msg.getChannel() == ShortMessage.CONTROL_CHANGE;
	}
	
	public static boolean isProgChange(ShortMessage msg) {
		return msg.getStatus() - msg.getChannel() == ShortMessage.PROGRAM_CHANGE;
	}
	
	public static boolean isNote(ShortMessage msg) {
		int stat = msg.getStatus() - msg.getChannel();
		return stat == Midi.NOTE_OFF || stat == NOTE_ON; 
	}

	public static HashMap<String, Class<?>> midiTemplate() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(PARAM_COMMAND, Integer.class);
		result.put(PARAM_CHANNEL, Integer.class);
		result.put(PARAM_DATA1, Integer.class);
		result.put(PARAM_DATA2, Integer.class);
		return result;
	}

	public static ShortMessage fromProps(HashMap<String, Object> props) throws InvalidMidiDataException {
		try {
			return new Midi(
				Integer.parseInt("" + props.get(PARAM_COMMAND)),
				Integer.parseInt("" + props.get(PARAM_CHANNEL)),
			    Integer.parseInt("" + props.get(PARAM_DATA1)),
			    Integer.parseInt("" + props.get(PARAM_DATA2)));
		} catch (Throwable t) {
			if (t instanceof InvalidMidiDataException) throw t;
			throw new InvalidMidiDataException(t.getMessage());
		}
	}

	
}

/* sys message
11111010	none	 	Start (song)
11111011	none	 	Stop
 */
