package net.judah.song;

import static net.judah.JudahZone.getClock;

import lombok.Getter;
import net.judah.midi.Signature;

public class SigProvider implements Cmdr {

	public static final SigProvider instance = new SigProvider();
	
	@Getter private final String[] keys = new String[Signature.values().length];

	private SigProvider() {
	}
	
//	@Override
//	public int value(String key) {
//		for (int i = 0; i < keys.length; i++)
//			if (keys[i].equals(key))
//				return i;
//		return 0;
//	}

	@Override
	public Signature resolve(String key) {
		for (Signature s : Signature.values())
			if (s.name.equals(key))
				return s;
		return null;
	}

	@Override
	public String lookup(int value) {
		if (value >= 0 && value < Signature.values().length)
			return Signature.values()[value].name;
		return Signature.FOURFOUR.name;
	}

	@Override
	public void execute(Param p) {
		if (p.cmd == Cmd.TimeSig) {
			getClock().setTimeSig(resolve(p.val));
		}
	}


}
