package net.judah.song;

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
				return p.val;
		return 0;
	}
	
}
