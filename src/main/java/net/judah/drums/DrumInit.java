package net.judah.drums;

import static judahzone.api.MidiConstants.DRUM_CH;
import static net.judah.mixer.Channels.RegisteredDrums.OldSkool;
import static net.judah.mixer.Channels.RegisteredDrums.Synth;

import lombok.RequiredArgsConstructor;
import net.judah.mixer.Channels.RegisteredDrums;

@RequiredArgsConstructor // hardcoded
public enum DrumInit {

		D1("Drum1", DRUM_CH, "808", "Rock1", OldSkool),
		D2("Drum2", DRUM_CH + 1, "Pearl", "Bossa1", Synth),
		H1("Hats", DRUM_CH + 2, "Hats", "Hats1", OldSkool),
		H2("Fills", DRUM_CH + 3, "VCO", "Fills1", OldSkool)
		;

		public final String name;
		public final int ch;
		public  final String program;
		public final String file;
		public final RegisteredDrums engine;

}
