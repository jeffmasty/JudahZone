package net.judah.settings;

import static net.judah.util.Constants.Param.*;

import java.util.HashMap;

import lombok.RequiredArgsConstructor;

public class Commands {

	@RequiredArgsConstructor
	public static enum MetronomeLbls {
		TICKTOCK("metro:start", "start/stop the metronome"),
		TEMPO("metro:tempo", "Set the metronome's tempo"),
		VOLUME("metro:volume", "Set Gain for the volume"),
		HIHATS("metro:hihats", "play some hi hats"),
		CLICKTRACK("metro:clicktrack", "Click Track settings"),
		;
		public final String name;
		public final String desc;
	}
	
	@RequiredArgsConstructor
	public static enum MixerLbls {
		TOGGLE_RECORD("loop:record", 	"Activate/deactivate recording on the provided looper number."),
		TOGGLE_PLAY	("loop:play", 		"Activate/deactivate playing a recorded loop with the Sample number"),
		CLEAR		("loop:clear", 		"Reset the given looper"), 
		LOAD_SAMPLE	("loop:loadSample", "load looper or sample, can be empty"),
		VOLUME		("mixer:volume", 	"Adjust loop or input gain between 0 and 1"),
		MUTE		("mixer:mute", 		"Mute/unmute the recording of a given looper channel"),
		AUDIOPLAY	("audio:play", 		"play an audio sample"),
		//UNDO("undo recording", ), // REDO("redo recording", ), 
		;
		public final String name;
		public final String desc;
	}
	
	@RequiredArgsConstructor
	public static enum SequencerLbls {
		CLICKTRACK("seq:clicktrack", "Click Track settings"),
		TRIGGER ("seq:trigger", 	"Move Sequencer to the next song section"),
		TRANSPORT("seq:transport", 	"Start/Stop Transport"),
		EXTERNAL("seq:extCtrl", 	"Move time clock from midi sequencer to a looper (TimeProvider)"),
		INTERNAL("seq:internal", 	"run an internal setup"),
		DROPBEAT("seq:dropDaBeat", 	"Mute loops until next pulse"),
		QPLAY	("seq:queuePlay", 	"play/stop on next pulse."),
		QREC	("seq:queueRecord", "record/stop on next pulse."),
		QUEUE   ("seq:queue", 		"queue a command for the next pulse"),
		UNIT	("seq:unit", 		"Change the unit used to calculate the sequencer."),
		SEQ     ("seq:load", 		"install an 8-step midi sequence"),
		ACTIVATE  ("seq:activate", 	"activate/deactivate a sequence by name"),
		VOLUME  ("seq:volume", 		"adjust volume of a sequence by name"),
		SETUP	("seq:setup", 		"Set sequencer settings"),
		RELOAD	("song:reload", 	"clear loops, refresh sequencer"),
		NEXT	("song:next",		"load next song")
		;
		public final String name;
		public final String desc;
	}

	@RequiredArgsConstructor
	public static enum FluidLbls {
		PROGCHANGE("synth:progChange", "Change Fluidsynth instrument"),
		INSTUP("synth:instUp", "Fluid instrument up, channel 0"),
		INSTDOWN("synth:instDown", "Fluid instrument down, channel 0"),
		DRUMBANK("synth:drumSet", "Change drum bank up (true/false) or preset #"),
		DIRECT("synth:direct", "Send semicolan terminated strings to fluid")
		;
		public final String name;
		public final String desc;
	}
	
	@RequiredArgsConstructor
	public static enum OtherLbls {
		RECORD  	("midi:record", 		"record a midi sequence"),
		PLAY		("midi:play", 		"play a midi sequence"),
		MIDINOTE	("midi:note", 		"send a midi note"),
		OCTAVER		("midi:octaver", 	"translate incoming midi notes"),
		TRANSPOSE   ("midi:transpose",  "translate a midi track"),
		MIDIGAIN	("midi:volume", 	"adjust a midi track volume"),
		MIDIFILE	("midi:file", 		"play a midi file"),
		ROUTECHANNEL("midi:routeChannel", "take all commands on a given channel and route them to another"),
		PLUGIN		("carla:load",     	"define a plugin"),
		DRYWET  	("carla:drywet",	"set dry/wet of a plugin"),
		PARAMETER	("carla:param", 	"set the parameter value of a plugin")
		;
		public final String name;
		public final String desc;
		
		public static HashMap<String, Class<?>> transposeTemplate() {
			HashMap<String, Class<?>> result = new HashMap<String, Class<?>>();
			result.put(ACTIVE, Integer.class);
			result.put(STEPS, Integer.class);
			result.put(CHANNEL, Boolean.class);
			return result;
		}

	}
	
}
 // MixerLbls TOGGLE_LOOP("Loop:ToggleRecord", "Switch Recording between first 2 loops."),