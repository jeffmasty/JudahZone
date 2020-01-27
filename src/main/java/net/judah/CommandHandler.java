package net.judah;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import lombok.extern.log4j.Log4j;
import net.judah.midi.Midi;
import net.judah.mixer.Mixer;
import net.judah.settings.Command;
import net.judah.settings.Mapping;
import net.judah.settings.Service;
import net.judah.settings.Settings;

@Log4j
public class CommandHandler implements Service {
	
	private static CommandHandler instance;

	private final ArrayList<Command> available = new ArrayList<>();
	private final List<Mapping> mappings;


	private final ArrayList<Command> commands = new ArrayList<>();
	private final Command save;
	private final Command load;

	public CommandHandler(List<Mapping> mappings) {
		instance = this;
		this.mappings = mappings;
		if (mappings != null)
		log.debug("currently handling " + mappings.size() + " command mappings. ");
		for (Mapping mapping : mappings) {
			log.debug("    " + mapping);
		}
		commands.add(new Command("EffectOn", this, "tUrN oN tHe EfFeCt"));
		commands.add(new Command("EffectOff", this, "tUrN oFf tHe EfFeCt"));

		HashMap<String, Class<?>> params = new HashMap<String, Class<?>>();
		params.put("file", File.class);
		save = new Command("Save Settings", this, params, "Save settings, filename optional");
		commands.add(save);
		load = new Command("Load Settings", this, params, "Load settings, filename optional");
		commands.add(load);

	}

	/** call after all services have been initialized */
	void initializeCommands() {

		available.clear();
		for (Service s : JudahZone.getServices()) {
			available.addAll(s.getCommands());
		}
		log.debug("currently handling " + available.size() + " available different commands");
		for (Command c : available) {
			log.debug("    " + c);
		}
		if (mappings.isEmpty())
			mappings.addAll(Settings.dummyMappings(available));
		// verifyMappings(); // TODO
	}

	Midi midiMsg;
	public boolean midiProcessed(byte[] midi) throws JudahException {
		midiMsg = new Midi(midi);
		for (Mapping mapping : instance.mappings) {

			if (mapping.isDynamic() && midiMsg.matches(mapping.getMidi()) && midiMsg.getCommand() == Midi.CONTROL_CHANGE) {
				final Properties p = new Properties();
				if (mapping.getCommandName().equals("Metronome settings")) {
					if (mapping.getProps().containsKey("volume")) {
						assert midiMsg.getData1() == 15 : midiMsg.getData1();
						p.put("volume", ((midiMsg.getData2() - 1))* 0.01f);
					} else if (mapping.getProps().containsKey("bpm")) {
						assert midiMsg.getData1() == 14 : midiMsg.getData1();
						p.put("bpm", (midiMsg.getData2() + 50) * 1.2f);
					}
					fire(mapping, p);
					return true;
				}
				else if (mapping.getCommandName().equals(Mixer.GAIN_COMMAND)) {
					p.put(Mixer.GAIN_PROP, midiMsg.getData2() / 50f );
					assert mapping.getProps().get(Mixer.CHANNEL_PROP) != null : Arrays.toString(mapping.getProps().keySet().toArray());
					p.put(Mixer.CHANNEL_PROP, mapping.getProps().get(Mixer.CHANNEL_PROP));
					fire(mapping, p);
				}
				else if (mapping.getCommandName().equals(Mixer.PLUGIN_COMMAND)) {
					fire(mapping, mapping.getProps());
				}
				else log.warn(mapping.getCommandName() + " midi: " + mapping.getMidi());
			}

			else if (mapping.getMidi().equals(midiMsg)) {
				Command c = instance.findCommand(mapping);
					if (c == null)
						throw new JudahException("Command not found for mapping. " + mapping);
					new Thread() {
						@Override public void run() {
							try {
								fire(mapping, mapping.getProps());
								// c.getService().execute(c, mapping.getProps());
							} catch (Exception e) { log.error(e.getMessage(), e); }
						}}.start();
				return true;
			}


//			else if (mapping.getMidi().matches(midiMsg)) {
//				final Properties p = new Properties();
//				if (midiMsg.getCommand() == Midi.CONTROL_CHANGE) {
//					if (mapping.getCommandName().equals("Metronome settings")) {
//						if (mapping.getProps().containsKey("volume")) {
//							assert midiMsg.getData1() == 15 : midiMsg.getData1();
//							p.put("volume", ((midiMsg.getData2() - 1))* 0.01f);
//						} else if (mapping.getProps().containsKey("bpm")) {
//							assert midiMsg.getData1() == 14 : midiMsg.getData1();
//							p.put("bpm", (midiMsg.getData2() + 50) * 1.2f);
//						}
//						fire(mapping, p);
//						return true;
//					}
//					else if (mapping.getCommandName().equals(Mixer.GAIN_COMMAND)) {
//						p.put(Mixer.GAIN_PROP, midiMsg.getData2() / 50f );
//						assert mapping.getProps().get(Mixer.CHANNEL_PROP) != null : Arrays.toString(mapping.getProps().keySet().toArray());
//						p.put(Mixer.CHANNEL_PROP, mapping.getProps().get(Mixer.CHANNEL_PROP));
//						fire(mapping, p);
//					}
//					else log.warn(mapping.getCommandName() + " midi: " + mapping.getMidi());
//				} else {
//					RTLogger.log(this, "interesting midi: " + midi);
//				}
//			}
		// else { RTLogger.warn(this, "No match " + midiMsg); }
		}
		return false;
	}

	private void fire(Mapping m, Properties p) throws JudahException {
		final Command c = instance.findCommand(m);
		if (c == null)
			throw new JudahException("Command not found for mapping. " + m);
		new Thread() {
			@Override public void run() {
				try {
					log.info("Running command: " + c + " " + prettyPrint(p));
					c.getService().execute(c, p);
				} catch (Exception e) { log.error(e.getMessage(), e); }
			}}.start();
	}

	private Command findCommand(Mapping m) {
		for (Command c : available)
			if (c.getName().equals(m.getCommandName()) && c.getService().getServiceName().equals(m.getServiceName()))
				return c;
		return null;
	}

	public static List<Command> getAvailableCommands() {
		ArrayList<Command> result = new ArrayList<>(instance.available.size());
		Collections.copy(result, instance.available);
		assert result.size() > 0;
		return result;
	}

	@Override
	public String getServiceName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Tab getGui() {
		return null; // TODO
	}

	/** This class's personal Commands, as per Service interface */
	@Override
	public List<Command> getCommands() {
		return commands;
	}

	@Override
	public void execute(Command cmd, Properties props) throws Exception {
//		if (cmd.getName().equals("EffectOn")) {
//			judah.offer(new Midi(ShortMessage.NOTE_ON, 9, 33, 90));
//			judah.offer(new Midi(ShortMessage.NOTE_OFF, 9, 33));
//		}
//
//	    if (cmd.getName().equals("EffectOff")) {
//	    	judah.offer(new Midi(ShortMessage.NOTE_ON, 9, 34, 100));
//	    	judah.offer(new Midi(ShortMessage.NOTE_OFF, 9, 34));
//	    }
	}

	@Override
	public void close() {
	}

	private String prettyPrint(Properties p) {
		if (p == null) return " null properties";
		StringBuffer b = new StringBuffer();
		for (Entry<Object, Object> entry:  p.entrySet())
			b.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
		return b.toString();
	}
	
}
