package net.judah.settings;

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
		GAIN		("mixer:volume", 	"Adjust loop or input gain between 0 and 1"),
		CHANNEL		("mixer:mute", 		"Mute/unmute the recording of a given looper channel"),
		AUDIOPLAY	("audio:play", 		"play an audio sample")
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
		UNIT	("seq:unit", "Change the unit used to calculate the sequencer."),
		SEQ     ("seq:load", "install an 8-step midi sequence"),
		ACTIVE  ("seq:activate", "activate/deactivate a sequence by name"),
		VOLUME  ("seq:volume", "adjust volume of a sequence by name"),
		SETUP	("seq:setup", "Set sequencer settings"),
		RELOAD	("song:reload", 	"clear loops, refresh sequencer")
		
		
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
		ROUTECHANNEL("midi:routeChannel", "take all commands on a given channel and route them to another"),
		MIDINOTE("midi:note", "send a midi note"),
		OCTAVER("midi:octaver", "translate midi notes"),
		MIDIPLAY("midi:play", "play a midi file"),
		;
		public final String name;
		public final String desc;
	}
	
}
 // MixerLbls TOGGLE_LOOP("Loop:ToggleRecord", "Switch Recording between first 2 loops."),