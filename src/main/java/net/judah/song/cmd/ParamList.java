package net.judah.song.cmd;

import java.util.ArrayList;

import lombok.NoArgsConstructor;
import net.judah.midi.JudahMidi;

@NoArgsConstructor
public class ParamList extends ArrayList<Param> {

	public ParamList(ParamList params) {
		super(params);
	}

	/**@return number of steps if Cmds contain a countdown */
	public int getBeats() {
		for (Param p : this) {
			if (p.cmd == Cmd.Bars)
				try {
					return Integer.parseInt(p.val) * JudahMidi.getClock().getMeasure();
				} catch (NumberFormatException e) {/* nada */}
			if (p.cmd == Cmd.Beats)
				try {
					return Integer.parseInt(p.val);
				} catch (NumberFormatException e) {/* nada */}
		}
	return 0;
	}

}
