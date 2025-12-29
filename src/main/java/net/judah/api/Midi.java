package net.judah.api;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

/** Utilities added to javax ShortMessage*/
public class Midi extends ShortMessage {

	public static final String PARAM_COMMAND = "command";
	public static final String PARAM_CHANNEL = "channel";
	public static final String PARAM_DATA1 = "data1";
	public static final String PARAM_DATA2 = "data2";

	/** @return null on internally handled exception */
	public static Midi create(int command, int channel, int data1, int data2) {
		try { return new Midi(command, channel, data1, data2);
		} catch(Throwable t) {
			System.err.printf("Midi.create(%d, %d, %d, %d) + %s%n",
	                  command, channel, data1, data2, t.getMessage());
			System.err.println("Midi.create(" + command + ", " + channel + ", " + data1 + ", " + data2 + ") + " + t.getMessage()); }
		return null;
	}

	public Midi(byte[] bytes) {
		super(bytes);
	}

	/**Create a midi message for Channel 0
	 * @return null on internally handled exception */
    public static Midi create(int command, int data1, int data2) {
        return create(command, 0, data1, data2);
    }

    public static MidiEvent createEvent(long tick, int cmd, int ch, int data1, int data2) {
    	Midi midi = create(cmd, ch, data1, data2);
    	return new MidiEvent(midi, tick);
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

	public Midi(int command, int channel, int data1, String port) throws InvalidMidiDataException {
	    this(command, channel, data1);
	}

    @Override
	public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getCommand()).append("/");
        b.append(getChannel()).append("/");
        if (getChannel() == 9) {
        	if (GMDrum.lookup(getData1()) != null)
        		b.append(GMDrum.lookup(getData1()).name());
        	else b.append(getData1()).append("?");
        }
        else if (isNote())
        	b.append(Key.key(getData1())).append(getData1() / 12);
        else
        	b.append(getData1());
        b.append("/").append(getData2());
        return b.toString();
	}

	public static String toString(MidiMessage m) {
		if (m instanceof Midi) return ((Midi)m).toString();
		return new Midi(m.getMessage()).toString();
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
		if (false == obj instanceof Midi) return false;
		Midi other = (Midi) obj;
		return this.getChannel() == other.getChannel() &&
				this.getCommand() == other.getCommand() &&
				this.getData1() == other.getData1() &&
				this.getData2() == other.getData2();
	}

	/** equal, ignoring data2 */
	public boolean matches(ShortMessage midi) {
		return getChannel() == midi.getChannel() && getCommand() == midi.getCommand() && getData1() == midi.getData1();
	}

	public void setVelocity(int velocity) throws InvalidMidiDataException {
		setMessage(getCommand(), getChannel(), getData1(), velocity);
	}


    public static Midi copy(ShortMessage copy) {
        if (copy == null) return null;
        byte[] clone = new byte[copy.getMessage().length];
        System.arraycopy(copy.getMessage(), 0, clone, 0, clone.length);
        Midi result = new Midi(clone);
        return result;
    }

	public static ShortMessage format(ShortMessage midi, int ch, float gain) {
		if (midi.getChannel() == ch && gain == 1)
			return copy(midi);
		return create(midi.getCommand(), ch, midi.getData1(), (int) (gain * midi.getData2()));
	}

	public static boolean isCC(MidiMessage msg) 		{ return is(CONTROL_CHANGE, msg); }
	public static boolean isPitchBend(MidiMessage msg) 	{ return is(PITCH_BEND, msg); }
	public static boolean isProgChange(MidiMessage msg) { return is(PROGRAM_CHANGE, msg); }
	public static boolean isNoteOn(MidiMessage msg) 	{ return is (NOTE_ON, msg); }
	public static boolean isNoteOff(MidiMessage msg) 	{ return is(NOTE_OFF, msg); }
	public boolean isNote() { return isNote(this); }
	public static boolean isNote(MidiMessage msg) {
		int stat = msg instanceof ShortMessage m ? msg.getStatus() - m.getChannel() : msg.getStatus();
		return stat == Midi.NOTE_OFF || stat == NOTE_ON;
	}
	private static boolean is(int type, MidiMessage msg) {
		return msg instanceof ShortMessage m && m.getStatus() - m.getChannel() == type;
	}

}


//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;
//import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
//import com.fasterxml.jackson.databind.ser.std.StdSerializer;
//@JsonDeserialize(using = Midi.Deserialization.class)
//@JsonSerialize(using = Midi.Serialization.class) // class Midi {

//public static class Serialization extends StdSerializer<Midi> {
//public Serialization() {this(null);}
//public Serialization(Class<Midi> vc) {super(vc);}
//@Override
//public void serialize(Midi value, JsonGenerator gen, SerializerProvider provider) throws IOException {
//  gen.writeString(value.toString());
//}}

//public static class Deserialization extends StdDeserializer<Midi> {
//public Deserialization() {this(null);}
//public Deserialization(Class<?> vc) {super(vc);}
///**@return null on parsing errors */
//@Override public Midi deserialize(JsonParser p, DeserializationContext ctxt)
//      throws IOException, JsonProcessingException {
//  JsonNode node = p.getCodec().readTree(p);
//  return Midi.deserialize(node.asText());
//}}

