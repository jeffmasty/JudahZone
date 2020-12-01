package net.judah.midi;

import java.util.HashMap;

import lombok.Getter;

public class GMDrums {

	@Getter private static final HashMap<Integer, String> map = initMap(); 

	
	
	
	public static HashMap<Integer, String> initMap() {
		HashMap<Integer, String> result = new HashMap<>();
		result.put(35, "Acoustic Bass Drum");
		result.put(36, "Bass Drum 1");
		result.put(37, "Side Stick");
		result.put(38, "Acoustic Snare");
		result.put(39, "Hand Clap");
		result.put(40, "Electric Snare");
		result.put(41, "Low Floor Tom");
		result.put(42, "Closed Hi-Hat");
		result.put(43, "High Floor Tom");
		result.put(44, "Pedal Hi-Hat");
		result.put(45, "Low Tom");
		result.put(46, "Open Hi-Hat");
		result.put(47, "Low-Mid Tom");
		result.put(48, "Hi-Mid Tom");
		result.put(49, "Crash Cymbal 1");
		result.put(50, "High Tom");
		result.put(51, "Ride Cymbal 1");
		result.put(52, "Chinese Cymbal");
		result.put(53, "Ride Bell");
		result.put(54, "Tambourine");
		result.put(55, "Splash Cymbal");
		result.put(56, "Cowbell");
		result.put(57, "Crash Symbol 2");
		result.put(58, "Vibraslap");
		result.put(59, "Ride Cymbal 2");
		result.put(60, "Hi Bongo");	// middle C				
		result.put(61, "Low Bongo");
		result.put(62, "Mute Hi Conga");
		result.put(63, "Open Hi Conga");
		result.put(64, "Low Conga");
		result.put(65, "High Timbale");
		result.put(66, "Low Timbale");
		result.put(67, "High Agogo");
		result.put(68, "Low Agogo");
		result.put(69, "Cabasa");
		result.put(70, "Maracas");
		result.put(71, "Short Whistle");
		result.put(72, "Long Whistle");
		result.put(73, "Short Guiro");
		result.put(74, "Long Guiro");
		result.put(75, "Claves");
		result.put(76, "Hi Wood Block");
		result.put(77, "Low Wood Block");
		result.put(78, "Mute Cuica");
		result.put(79, "Open Cuica");
		result.put(80, "Mute Triangle");
		result.put(81, "Open Triangle");
		result.put(82, "Shaker");
		return result;
	}




	public static String format(Midi midi) {
		if (midi == null) return "null";
		if (map.containsKey(midi.getData1()))
			return map.get(midi.getData1()) + " (" + midi + ")";
		return midi.toString();
	}
	
	
}

