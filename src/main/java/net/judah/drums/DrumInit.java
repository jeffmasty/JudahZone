package net.judah.drums;

import static judahzone.api.MidiConstants.DRUM_CH;
import static net.judah.mixer.Channels.RegisteredDrums.*;

import lombok.RequiredArgsConstructor;
import net.judah.mixer.Channels.RegisteredDrums;

@RequiredArgsConstructor // hardcoded
public enum DrumInit {

		D1("Drum1", DRUM_CH, "BBuddy", "Rock1", Wav),
		D2("Drum2", DRUM_CH + 1, "[Default]", "Bossa1", Noise),
		H1("Hats", DRUM_CH + 2, "[Default]", "Hats1", FM),
		H2("Fills", DRUM_CH + 3, "[Default]", "Fills1", Pluck)
		;

		public final String name;
		public final int ch;
		public  final String program;
		public final String file;
		public final RegisteredDrums engine;

}
