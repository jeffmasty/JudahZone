package net.judah.mixer;

import java.util.ArrayList;
import java.util.Arrays;

import lombok.Getter;

@Getter 
public class Instruments extends ArrayList<Instrument> {
	
	public static Channel byName(String name, ArrayList<Channel> channels) {
		for (Channel c : channels)
			if (c.getName().equals(name))
				return c;
		throw new NullPointerException(name + " " + Arrays.toString(channels.toArray()));
	}
	
	public Instrument byName(String name) {
		for (Instrument c : this)
			if (c.getName().equals(name))
				return c;
		return null;
	}

    public ArrayList<Instrument> getInstruments() {
    	ArrayList<Instrument> result = new ArrayList<>();
    	for (LineIn ch : this)
    		if (ch instanceof Instrument)
    			result.add((Instrument)ch);
    	return result;
    }

//    public void setVolume(int vol, JackPort midiOut) {
//        JudahMidi midi = JudahZone.getMidi();
//        if (midiOut == midi.getFluidOut())
//            getFluid().getGain().setVol(vol);
//    }
//    public int getVolume(JackPort midiOut) {
//        JudahMidi midi = JudahZone.getMidi();
//        if (midiOut == midi.getFluidOut())
//            return getFluid().getVolume();
//        return 0;
//    }

    @Override
    public String toString() {
        String debug = "channels: ";
        for (Channel ch : this)
            debug += ch.getName() + " ";
        return debug;
    }
    
}
// public static final String CIRCUIT = "TRAX";
// public static final String UNO = "UNO";
// private LineIn circuit, uno, aux1;
// fluid.setIcon(Icons.load("Synth.png"));
// calf.setIcon(Icons.load("Drums.png"));
// uno = new LineIn(UNO, 
//		new String[] {"system:capture_7", "system:capture_8"},
//		new String[] {"UnoL", "UnoR"});
// circuit = new LineIn("TRAX", 
//		new String[] {"system:capture_5", "system:capture_6"},
//		new String[] {"CircuitL", "CircuitR"});
