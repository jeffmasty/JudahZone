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

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Console;
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

	@Getter @Setter protected String port;

	
	
	public Midi(byte[] bytes) {
		super(bytes);
	}

	public Midi(byte[] bytes, String sourcePort) {
	    super(bytes);
	    this.port = sourcePort;
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
	    this.port = port;
	}


    @Override
	public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getCommand()).append("/");
        b.append(getChannel()).append("/");
        b.append(getData1()).append("/");
        b.append(getData2());
        if (port != null) b.append("(").append(port).append(")");
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

	public static HashMap<String, Class<?>> midiTemplate() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(PARAM_COMMAND, Integer.class);
		result.put(PARAM_CHANNEL, Integer.class);
		result.put(PARAM_DATA1, Integer.class);
		result.put(PARAM_DATA2, Integer.class);
		return result;
	}

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
        String source = null;
        if (src.length == 2)
            source = src[1].substring(0, src[1].length() - 1); // remove last parentheses

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
            if (source != null)
                result.setPort(source);
        } catch (Throwable t) {
            Console.warn(t);
        }
        return result;
	}

    public static Midi copy(Midi copy) {
        if (copy == null) return null;
        byte[] clone = new byte[copy.getMessage().length];
        System.arraycopy(copy.getMessage(), 0, clone, 0, clone.length);
        Midi result = new Midi(clone);
        result.setPort(copy.getPort());
        return result;
    }

    public static class Serialization extends StdSerializer<Midi> {
        public Serialization() {this(null);}
        public Serialization(Class<Midi> vc) {super(vc);}
        @Override
        public void serialize(Midi value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
//            gen.writeStartObject();
//            gen.writeStringField("midi", value.toString());
//            gen.writeEndObject();
        }
    }

    public static class Deserialization extends StdDeserializer<Midi> {
        public Deserialization() {this(null);}
        public Deserialization(Class<?> vc) {super(vc);}
        /**@return null on parsing errors */
        @Override public Midi deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            // various contexts where we will be parsing Midi messages:
            return Midi.deserialize(node.asText());
//            JsonNode n = node.get("midi");
//            if (n == null)
//                n = node.get("fromMidi");
//            if (n == null)
//                n = node.get("toMidi");
//            if (n == null) return null;
//            String raw = n.asText();
//            return Midi.deserialize(raw);
        }
    }

    public int octave() {
		return getData1() / 12 - 2;
	}
	public Key key() {
		return Key.values()[getData1() % 12];
	}

    
}
