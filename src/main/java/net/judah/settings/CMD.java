package net.judah.settings;

import lombok.RequiredArgsConstructor;

public class CMD {

	@RequiredArgsConstructor
	public static enum MetronomeLbls {
		TICKTOCK("metro:start", "start/stop the metronome"),
		CLICKTRACK("metro:setup", "Click Track settings"),
		TEMPO("metro:tempo", "Set the metronome's tempo"),
		VOLUME("metro:volume", "Set Gain for the volume")
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
		CHANNEL		("mixer:mute", 		"Mute/unmute the recording of a given looper channel")
		//UNDO("undo recording", ), // REDO("redo recording", ), 
		;
		public final String name;
		public final String desc;
	}
	
	@RequiredArgsConstructor
	public static enum SequencerLbls {
		TRIGGER ("seq:trigger", 	"Move Sequencer to the next song section"),
		END		("seq:end", 		"Stop Transport."),
		EXTERNAL("seq:extCtrl", 	"Move time clock from midi sequencer to a looper (TimeProvider)"),
		INTERNAL("seq:internal", 	"run an internal setup"),
		DROPBEAT("seq:dropDaBeat", 	"Mute loops until next pulse"),
		QPLAY	("seq:queuePlay", 	"play/stop on next pulse."),
		QREC	("seq:queueRecord", "record/stop on next pulse."),
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
		DRUMBANK("synth:drumSet", "Change drum bank up (true/false) or preset #")
		;
		public final String name;
		public final String desc;
	}
	
	@RequiredArgsConstructor
	public static enum OtherLbls {
		ROUTECHANNEL("midi:routeChannel", "take all commands on a given channel and route them to another"),
		MIDINOTE("midi:note", "send a midi note")
		;
		public final String name;
		public final String desc;
	}
	
}
 // MixerLbls TOGGLE_LOOP("Loop:ToggleRecord", "Switch Recording between first 2 loops."),