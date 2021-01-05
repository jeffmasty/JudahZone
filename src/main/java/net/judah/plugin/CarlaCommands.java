package net.judah.plugin;

import static net.judah.settings.Commands.OtherLbls.*;
import static net.judah.util.Constants.Param.*;

import java.util.ArrayList;
import java.util.HashMap;

import net.judah.JudahZone;
import net.judah.api.Command;

public class CarlaCommands extends ArrayList<Command> {

	CarlaCommands(final Carla carla) {
		add(new Command(DRYWET.name, DRYWET.desc, template()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				int idx = Integer.parseInt(props.get(INDEX).toString());
				float val = (midiData2 >= 0) ? 
					midiData2 / 127f 
					: Float.parseFloat(props.get(VALUE).toString());
				carla.setDryWet(idx, val);
			}});
		
		add(new Command(PARAMETER.name, PARAMETER.desc, paramTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				int paramIdx = Integer.parseInt(props.get("paramIdx").toString());
				int idx = Integer.parseInt(props.get(INDEX).toString());
				float val = midiData2 >= 0 ? 
						midiData2 / 100f
						: Float.parseFloat(props.get(VALUE).toString());
				carla.setParameterValue(idx, paramIdx, val);
			}
		});
		
		
		add(new Command("carla:flanger", "turn on/off flanger", channelTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = (midiData2 >= 0) 
						? midiData2 > 0 : parseActive(props);
				carla.flanger(active, JudahZone.getChannels().byName(parseString(CHANNEL, props)));
			}			
		});
	}

	public static HashMap<String, Class<?>> template() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(INDEX, Integer.class);
		result.put(VALUE, Float.class);
		return result;
	}
	public static HashMap<String, Class<?>> paramTemplate() {
		HashMap<String, Class<?>> result = template();
		result.put("paramIdx", Integer.class);
		return result;
	}
	public static HashMap<String, Class<?>> channelTemplate() {
		HashMap<String, Class<?>> result = activeTemplate();
		result.put(CHANNEL, String.class);
		return result;
	}
	
}
