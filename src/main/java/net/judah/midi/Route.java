package net.judah.midi;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.Getter;
import net.judah.api.Midi;


@Data 
public class Route {

	private final Integer fromChannel;
	private final Integer toChannel;
	private final Integer fromCommand;
	private final Integer toCommand;
	private final Integer fromData1;
	private final Integer toData1;
	
	@JsonIgnore
	@Getter private transient Octaver octaver;

	public Route(Octaver octaver) {
		this(octaver.getChannel(), octaver.getChannel());
		this.octaver = octaver;
	}
	
	public Route(int fromChannel, int toChannel) {
		this.fromChannel = fromChannel;
		this.toChannel = toChannel;
		fromCommand = toCommand = fromData1 = toData1 = null;
	}

	public Route(Midi from, Midi to) {
		fromChannel = from.getChannel();
		fromCommand = from.getCommand();
		fromData1 = from.getData1();
		toChannel = to.getChannel();
		toCommand = to.getCommand();
		toData1 = to.getData1();
	}
	

	public boolean isChannelRoute() {
		return fromCommand == null;
	}

}
