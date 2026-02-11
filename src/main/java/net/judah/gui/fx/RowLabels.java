package net.judah.gui.fx;

import java.security.InvalidParameterException;

import judahzone.data.Pair;
import net.judah.channel.Channel;

public class RowLabels extends Row {
	public static final int KNOBS = 4;

	public RowLabels(Channel ch, Pair... source) {
		super(ch);
		if (source.length != KNOBS)
			throw new InvalidParameterException("source knobs " + source.length);
		for (Pair item : source) {
			add(new FxTrigger(item, ch));
		}
	}

}
