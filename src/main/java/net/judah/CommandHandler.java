package net.judah;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.midi.Midi;
import net.judah.midi.MidiListener;
import net.judah.mixer.MixerCommands;
import net.judah.settings.Command;
import net.judah.settings.Service;
import net.judah.song.Link;
import net.judah.util.JudahException;
import net.judah.util.Links;

@Log4j
public class CommandHandler {
	
	@Getter private static CommandHandler instance;

	private final ArrayList<Command> available = new ArrayList<>();
	// private final List<Mapping> mappings;
	private final Links links = new Links();
	
	@Setter private MidiListener midiListener;

//	public CommandHandler(List<Mapping> mappings) {
//		instance = this;
//		this.mappings = mappings;
//		if (mappings != null)
//		log.debug("currently handling " + mappings.size() + " command mappings. ");
//		for (Mapping mapping : mappings) {
//			log.debug("    " + mapping);
//		}
//	}
	public CommandHandler() {
		instance = this;
	}

	/** call after all services have been initialized */
	void initializeCommands() {

		available.clear();
		for (Service s : JudahZone.getServices()) {
			available.addAll(s.getCommands());
		}
		log.debug("currently handling " + available.size() + " available different commands");
		log.debug("known commands:");
		for (Command c : available) {
			log.debug("    " + c);
		}
//		if (mappings.isEmpty())
//			mappings.addAll(Settings.dummyMappings(available));
		
//for (Mapping mapping : mappings) log.warn(mapping);
//	Mapping Metronome.tick for 176.101.127/0  Properties: none
//	Mapping Metronome.tock for 176.101.0/0  Properties: none
//	Mapping Metronome.Metronome settings for 176.14.0/0 dynamic  Properties: bpm:todo
//	Mapping Metronome.Metronome settings for 176.15.0/0 dynamic  Properties: volume:todo
//	Mapping Mixer.record loop for 176.97.127/0  Properties: Active:true Loop:1
//	Mapping Mixer.record loop for 176.97.0/0  Properties: Active:false Loop:1
//	Mapping Mixer.record loop for 176.96.127/0  Properties: Active:true Loop:0
//	Mapping Mixer.record loop for 176.96.0/0  Properties: Active:false Loop:0
//	Mapping Mixer.play loop for 176.100.127/0  Properties: Active:true Loop:1
//	Mapping Mixer.play loop for 176.100.0/0  Properties: Active:false Loop:1
//	Mapping Mixer.play loop for 176.99.127/0  Properties: Active:true Loop:0
//	Mapping Mixer.play loop for 176.99.0/0  Properties: Active:false Loop:0
//	Mapping Mixer.clear looper for 176.98.127/0  Properties: none
		// verifyMappings(); // TODO
	}

	Midi midiMsg;
	/** @return true if consumed */
	public boolean midiProcessed(byte[] midi) throws JudahException {
		midiMsg = new Midi(midi);
		
		if (midiListener != null) {
			new Thread() {
			@Override public void run() {midiListener.feed(midiMsg);};
			}.start();
			return !Midi.isNote(midiMsg);
			
		}
		
		for (Link mapping : links) {
//		for (Mapping mapping : mappings) {

			if ( Arrays.equals(midiMsg.getMessage(), mapping.getMidi()) && midiMsg.getCommand() == Midi.CONTROL_CHANGE) {
			// if (mapping.isDynamic() && midiMsg.matches(mapping.getMidi()) && midiMsg.getCommand() == Midi.CONTROL_CHANGE) {
				final Properties p = new Properties();
				if (mapping.getCommand().equals("Metronome settings")) {
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
				else if (mapping.getCommand().equals(MixerCommands.GAIN_COMMAND)) {
					p.put(MixerCommands.GAIN_PROP, midiMsg.getData2() / 100f);
					assert mapping.getProps().get(MixerCommands.CHANNEL_PROP) != null : Arrays.toString(mapping.getProps().keySet().toArray());
					p.put(MixerCommands.CHANNEL_PROP, mapping.getProps().get(MixerCommands.CHANNEL_PROP));
					fire(mapping, p);
				}
				else { // if (mapping.getCommand().equals(MixerCommands.PLUGIN_COMMAND)) {
					p.putAll(mapping.getProps());
					fire(mapping, p);
				}
				// else log.warn(mapping.getCommand() + " midi: " + mapping.getMidi());
			}

			else if (Arrays.equals(mapping.getMidi(), midiMsg.getMessage())) {
				Command c = findCommand(mapping);
					if (c == null)
						throw new JudahException("Command not found for mapping. " + mapping);
					new Thread() {
						@Override public void run() {
							try {
								Properties p = new Properties();
								p.putAll(mapping.getProps());
								fire(mapping, p);
								// c.getService().execute(c, mapping.getProps());
							} catch (Exception e) { log.error(e.getMessage(), e); }
						}}.start();
				return true;
			}
		}
		return false;
	}

	
	
	private void fire(Link m, Properties p) throws JudahException {
		final Command c = findCommand(m);
		if (c == null)
			throw new JudahException("Command not found for mapping. " + m);
		new Thread() {
			@Override public void run() {
				try {
					log.info(c + " " + prettyPrint(p));
					c.getService().execute(c, p);
				} catch (Exception e) { log.error(e.getMessage(), e); }
			}}.start();
	}

	private Command findCommand(Link m) {
		for (Command c : available)
			if (c.getName().equals(m.getCommand()) && c.getService().getServiceName().equals(m.getService()))
				return c;
		return null;
	}

	public static Command[] getAvailableCommands() {
		return instance.available.toArray(new Command[instance.available.size()]);
	}

	@SuppressWarnings("rawtypes")
	private String prettyPrint(Properties p) {
		if (p == null) return " null properties";
		StringBuffer b = new StringBuffer();
		for (Entry entry:  p.entrySet())
			b.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
		return b.toString();
	}

	public static Command find(String service, String command) {
		for (Command c : instance.available) 
			if (c.getService().getServiceName().equals(service) && c.getName().equals(command))
				return c;
		log.warn("could not find " + service + " - " + command + " in " + Arrays.toString(instance.available.toArray()));
		return null;
	}
	
	public static void clearMappings() {
		instance.links.clear();
	}
	
	public static void addMappings(ArrayList<Link> links) {
		instance.links.addAll(links);
		log.warn("added " + links.size() + " mappings, total: " + instance.links.size());
	}
	
}



//else if (mapping.getMidi().matches(midiMsg)) {
//final Properties p = new Properties();
//if (midiMsg.getCommand() == Midi.CONTROL_CHANGE) {
//	if (mapping.getCommandName().equals("Metronome settings")) {
//		if (mapping.getProps().containsKey("volume")) {
//			assert midiMsg.getData1() == 15 : midiMsg.getData1();
//			p.put("volume", ((midiMsg.getData2() - 1))* 0.01f);
//		} else if (mapping.getProps().containsKey("bpm")) {
//			assert midiMsg.getData1() == 14 : midiMsg.getData1();
//			p.put("bpm", (midiMsg.getData2() + 50) * 1.2f);
//		}
//		fire(mapping, p);
//		return true;
//	}
//	else if (mapping.getCommandName().equals(Mixer.GAIN_COMMAND)) {
//		p.put(Mixer.GAIN_PROP, midiMsg.getData2() / 50f );
//		assert mapping.getProps().get(Mixer.CHANNEL_PROP) != null : Arrays.toString(mapping.getProps().keySet().toArray());
//		p.put(Mixer.CHANNEL_PROP, mapping.getProps().get(Mixer.CHANNEL_PROP));
//		fire(mapping, p);
//	}
//	else log.warn(mapping.getCommandName() + " midi: " + mapping.getMidi());
//} else {
//	RTLogger.log(this, "interesting midi: " + midi);
//}
//}
// else { RTLogger.warn(this, "No match " + midiMsg); }

