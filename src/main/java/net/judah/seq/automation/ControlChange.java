package net.judah.seq.automation;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public
enum ControlChange {
	// BANK(0),
	MODWHEEL(1, "Open the filter"),
	// BREATH(2),
	ROOM(3, "Reverb depth"), // undefined
	// WAHPEDAL(4),
	GLIDE(5, "Portamento"), // Portamento time // TODO
	// data entry
	VOLUME(7, "Gain"),
	BALANCE(8, "Pan"), // stereo-patch
	// undefined
	PAN(10, "Balance"), // mono-patch
	// EXPRESSION(11),
	// CTRL1(12),
	// CTRL2(13),
	// 14 - 31 undefined, general purpose // THRESH(14), CGAIN(15), RATIO(16), // Compressor
	// 32 -63 LSB for above
	PEDAL(64, "Drone/Hold", true),
	// P_ON(65), // Portamento Switch
	// SUSTAIN(66), // Sustenuto, Sample and Hold RAND
	// SOFTPED(67), // Soft Pedal, lower volume
	// LEGATO(68), // Legato
	// R_HOLD(69), // Hold2, w/ Release
	LFO(70, "LFO cycle rate"), // Sound Controller 1
	BRIGHT(71, "Resonance"),
	RELEASE(72, "Envelope release"),  // Envelope Release
	ATTACK(73, "Envelope attack"),  // Envelope Attack
	HZ(74, "Filter cutoff"), // Filter Cutoff frequency
	DECAY(75, "Envelope decay"),
	RATE(76, "Chorus speed"), // vibrato
	DEPTH(77, "Chorus expression"), // vibrato
	ECHO(78, "Delay Time"), // non-std delayFx time
	ECHO_FB(79, "Delay Amount"), // non-std   'metronome rate'
	SUSTAIN(80, "Envelope sustain"), // Envelop Decay
	LOCUT(81, "Hi-Pass filter on/off", true),
	// 82, 83 generic toggle switch
	ZIP(82, "Turn on Compressor", true), // non-std toggle Compression
	// PORTAMENTO(84),
	// 85 - 90 undefined
	DRIVE(90, "Overdrive distortion"), // non-std
	REVERB(91, "Reverb amount"),
	TREMELO(92, "Tremelo variation"),
	CHORUS(93, "Chorus amount"), // Feedback?
	DETUNE(94, "Amount off 64"),
	PHASER(95, "Chorus Phase wobble"),

	// CHANNEL MODE:
	PANIC(120, "Send Panic", true), // cut release/reverb "allSoundOff"
	RESET(121, "Reset Fx", true), // fx
	// LOCAL(122), // enable keys
	STOP(123, "Stop track", true), // release ok "allNotesOff"
	// OFF(124), // play specific channels
	// ON(125), // play all incoming channels
	// MONO(126), // TODO
	// POLY(127);
	;
	public final int data1;
	public final String description;
	public final boolean toggle;


	ControlChange(int val, String d) {
		this(val, d, false);
	}

	public static ControlChange find(MidiMessage m) {
		if (Midi.isCC(m))
			return find(((ShortMessage)m).getData1());
		return null;
	}

	/** Handled by this system or null */
	public static ControlChange find(int data1) {
		for (ControlChange cc : values())
			if (cc.data1 == data1)
				return cc;
		return null;
	}

	public boolean matches(ShortMessage m) {
		return Midi.isCC(m) && m.getData1() == data1;
	}

	/** ordered by function/popularity */
	public static final ControlChange[] ORDERED = {VOLUME, PAN,
			REVERB, ROOM, ECHO, ECHO_FB, CHORUS, RATE, DEPTH, PHASER, TREMELO, DRIVE, ZIP,
			LFO, GLIDE, PEDAL, MODWHEEL, BRIGHT, DETUNE, LOCUT, HZ, ATTACK, DECAY, SUSTAIN, RELEASE,
			RESET, STOP, PANIC};

}