package net.judah.song.cmd;

import java.util.ArrayList;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ParamList extends ArrayList<Param> {

	public ParamList(ParamList params) {
		super(params);
	}

	public long getTimeCode() {
		for (Param p : this)
			if (p.cmd == Cmd.TimeCode)
				return Long.parseLong(p.val);
		return 0;
	}
	
}
