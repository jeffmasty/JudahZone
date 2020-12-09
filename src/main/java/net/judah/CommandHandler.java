package net.judah;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.api.Service;
import net.judah.midi.MidiListener;
import net.judah.midi.MidiListener.PassThrough;
import net.judah.sequencer.Sequencer;
import net.judah.song.Link;
import net.judah.util.JudahException;
import net.judah.util.Links;

@RequiredArgsConstructor @Log4j 
public class CommandHandler {
	
	private final ArrayList<Command> available = new ArrayList<>();
	private final Links links = new Links();
	private final Sequencer sequencer;
	
	@Setter private MidiListener midiListener;


	/** call after all services have been initialized */
	public void initializeCommands() {
		for (Service s : sequencer.getServices()) 
			available.addAll(s.getCommands());

		for (Service s : JudahZone.getServices()) 
			available.addAll(s.getCommands());
		
		//log.debug("currently handling " + available.size() + " available different commands");
		//for (Command c : available) log.debug("    " + c);
	}

	/** @return true if consumed */
	public boolean midiProcessed(Midi midiMsg) throws JudahException {
		if (midiListener != null) {
			new Thread() {
			@Override public void run() {midiListener.feed(midiMsg);};
			}.start();
			
			MidiListener.PassThrough mode = midiListener.getPassThroughMode();
			if (PassThrough.NONE == mode)
				return true;
			if (PassThrough.NOTES == mode)
				return !Midi.isNote(midiMsg);
		}
		
		Command cmd;
		HashMap<String, Object> p;
		for (Link mapping : links) {
			cmd = mapping.getCmd();
			if (cmd == null)
				throw new JudahException("Command not found for mapping. " + mapping);

			if (midiMsg.getCommand() == Midi.CONTROL_CHANGE 
					&& (byte)midiMsg.getData1() == mapping.getMidi()[1]) {
				p = new HashMap<String, Object>();
				p.putAll(mapping.getProps());
				fire(cmd, p, midiMsg.getData2());
				return true;
			}
			
			else if (Arrays.equals(mapping.getMidi(), midiMsg.getMessage())) { // Prog Change
				fire(cmd, mapping.getProps(), midiMsg.getData2());
				return true;
			}
		}
		return false;
	}
	
	public void fire(final Command cmd, HashMap<String, Object> props, int midiData2) {
		new Thread() {
			@Override public void run() {
				try {
					cmd.setSeq(sequencer);
					cmd.execute(props, midiData2);
				} catch (Exception e) { 
					log.error(e.getMessage() + " for " + cmd + " with " + Command.toString(props), e); 
					}
			}}.start();
	}

	public Command[] getAvailableCommands() {
		return available.toArray(new Command[available.size()]);
	}

	public Command find(String name) {
		for (Command c : available)
			if (c.getName().equals(name))
				return c;
		return null;
	}
	
//	public Command find(String service, String command) {
//		for (Command c : available) 
//			try {
//				assert c != null;
//				assert c.getName() != null;
//				if (c.getService().getServiceName().equals(service) && c.getName().equals(command))
//					return c;
//			} catch (Throwable t) {
//				log.error(c + " " + c.getService() + " " + t.getMessage(), t);
//			}
//		log.warn("could not find " + service + " - " + command + " in " + Arrays.toString(available.toArray()));
//		return null;
//	}
	
	public void clearMappings() {
		links.clear();
	}
	
	public void addMappings(LinkedHashSet<Link> linkedHashSet) {
		links.addAll(linkedHashSet);
		log.debug("added " + linkedHashSet.size() + " mappings, total: " + links.size());
	}

}

//for (Mapping mapping : mappings) log.warn(mapping);
//Mapping Metronome.tick for 176.101.127/0  Properties: none
//Mapping Metronome.tock for 176.101.0/0  Properties: none
//Mapping Metronome.Metronome settings for 176.14.0/0 dynamic  Properties: bpm:todo
//Mapping Metronome.Metronome settings for 176.15.0/0 dynamic  Properties: volume:todo
//Mapping Mixer.record loop for 176.97.127/0  Properties: Active:true Loop:1
//Mapping Mixer.record loop for 176.97.0/0  Properties: Active:false Loop:1
//Mapping Mixer.record loop for 176.96.127/0  Properties: Active:true Loop:0
//Mapping Mixer.record loop for 176.96.0/0  Properties: Active:false Loop:0
//Mapping Mixer.play loop for 176.100.127/0  Properties: Active:true Loop:1
//Mapping Mixer.play loop for 176.100.0/0  Properties: Active:false Loop:1
//Mapping Mixer.play loop for 176.99.127/0  Properties: Active:true Loop:0
//Mapping Mixer.play loop for 176.99.0/0  Properties: Active:false Loop:0
//Mapping Mixer.clear looper for 176.98.127/0  Properties: none

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
