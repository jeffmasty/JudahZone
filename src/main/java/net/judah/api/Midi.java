package net.judah.api;

import java.io.IOException;
import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import net.judah.tracker.GMDrum;
import net.judah.util.RTLogger;

/** Utilities added to javax ShortMessage, with an optional source Midi port */
@JsonDeserialize(using = Midi.Deserialization.class)
@JsonSerialize(using = Midi.Serialization.class) 
public class Midi extends ShortMessage {

	public static int MIDDLE_C = 60;

	public static final String PARAM_COMMAND = "command";
	public static final String PARAM_CHANNEL = "channel";
	public static final String PARAM_DATA1 = "data1";
	public static final String PARAM_DATA2 = "data2";

	public Midi(byte[] bytes) {
		super(bytes);
	}

	/** @return null on internally handled exception */
	public static Midi create(int command, int channel, int data1, int data2) {
		try { return new Midi(command, channel, data1, data2);
		} catch(Throwable t) { 
			RTLogger.warn("Midi.create()", t); }
		return null;
	}

	/**Create a midi message for Channel 0
	 * @return null on internally handled exception */
    public static Midi create(int command, int data1, int data2) {
        return create(command, 0, data1, data2);
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
        if (getChannel() == 9)
        	b.append(GMDrum.lookup(getData1()).name());
        else 
        	b.append(getData1());
        b.append("/").append(getData2());
        return b.toString();
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

	public static boolean isPitchBend(ShortMessage msg) {
		return msg.getStatus() - msg.getChannel() == ShortMessage.PITCH_BEND;
	}
	
	public static boolean isCC(ShortMessage msg) {
		 return msg.getStatus() - msg.getChannel() == ShortMessage.CONTROL_CHANGE;
	}
	public boolean isCC() { return isCC(this); }

	public static boolean isProgChange(ShortMessage msg) {
		return msg.getStatus() - msg.getChannel() == ShortMessage.PROGRAM_CHANGE;
	}
	public boolean isProgChange() { return isProgChange(this); }

	public static boolean isNoteOn(ShortMessage msg) {
		return msg != null && msg.getStatus() - msg.getChannel() == NOTE_ON;
	}
	
	public static boolean isNote(ShortMessage msg) {
		int stat = msg.getStatus() - msg.getChannel();
		return stat == Midi.NOTE_OFF || stat == NOTE_ON;
	}
	public boolean isNote() { return isNote(this); }

	public static Midi fromProps(HashMap<String, Object> props) throws InvalidMidiDataException {
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

	public static Midi deserialize(String raw) {
        String[] src = raw.split("[(]");
        String[] values = src[0].split("/");

        int command = Integer.parseInt(values[0]);
        int channel = Integer.parseInt(values[1]);
        int data1 = -1;
        if (values.length >= 3 && values[2].length() > 0)
            data1 = Integer.parseInt(values[2]);
        int data2 = -1;
        if (values.length == 4 && values[3].length() > 0)
            data2 = Integer.parseInt(values[3]);
        Midi result = null;
        try {
            if (data1 >= 0) {
                if (data2 >= 0)
                    result = new Midi(command, channel, data1, data2);
                else
                    result = new Midi(command, channel, data1);
            }
            else
                result = new Midi(command, channel);
        } catch (Throwable e) {
        	RTLogger.warn("Midi.deserialize", e);
        }
        return result;
	}

    public static Midi copy(ShortMessage copy) {
        if (copy == null) return null;
        byte[] clone = new byte[copy.getMessage().length];
        System.arraycopy(copy.getMessage(), 0, clone, 0, clone.length);
        Midi result = new Midi(clone);
        return result;
    }

    public static class Serialization extends StdSerializer<Midi> {
        public Serialization() {this(null);}
        public Serialization(Class<Midi> vc) {super(vc);}
        @Override
        public void serialize(Midi value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    public static class Deserialization extends StdDeserializer<Midi> {
        public Deserialization() {this(null);}
        public Deserialization(Class<?> vc) {super(vc);}
        /**@return null on parsing errors */
        @Override public Midi deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            return Midi.deserialize(node.asText());
        }
    }

    public int octave() {
		return getData1() / 12 - 2;
	}
	public Key key() {
		return Key.values()[getData1() % 12];
	}
    
}
