package net.judah.tracker;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import net.judah.drumz.DrumType;
import net.judah.midi.JudahClock;
import net.judah.midi.MidiPort;
import net.judah.util.Constants;

public class DrumTrack extends Track {

	@Getter private final int[] gm = new int[DrumType.values().length];
	@Setter @Getter private String beatKit;

	
	public static final String HEADER = "Contract";
	public static final float DEF_VOL = 0.9f;

	@Getter private final ArrayList<GMDrum> kit = new ArrayList<>();
	@Getter private final ArrayList<Float> volume = new ArrayList<>();
	
	public DrumTrack(JudahClock clock, String name, MidiPort port, JudahBeatz t) {
		super(clock, name, 9, port, t);
		for (GMDrum d : GMDrum.Standard) {
			kit.add(d);
			volume.add(DEF_VOL);
		}
		edit = new DrumEdit(this);

	}

	public float velocity(int data1) {
		for (int i = 0; i < kit.size(); i++)
			if (kit.get(i).getData1() == data1)
				return volume.get(i);
		return 0f;
	}
//TODO mutes
	public String kitToFile() {
		StringBuffer sb = new StringBuffer(HEADER).append(Constants.NL);
		for (int i = 0; i < kit.size(); i++)
			sb.append(kit.get(i).getData1()).append(",").append(volume.get(i)).append(Constants.NL);
		return sb.append(HEADER).append(Constants.NL).toString();
	}
	
	public void kitFromFile(ArrayList<String> s) {
		kit.clear();
		volume.clear();
		if (s.size() < 2)
			throw new InvalidParameterException(s.toString());
		for (int i = 0; i < s.size(); i++) {
			String[] line = s.get(i).split(",");
			GMDrum d = GMDrum.lookup(Integer.parseInt(line[0]));
			float vol = DEF_VOL;
			if (line.length > 1)
				vol = Float.parseFloat(line[1]);
			kit.add(d);
			volume.add(vol);
		}
		((DrumEdit)edit).fillKit();
	}

}
