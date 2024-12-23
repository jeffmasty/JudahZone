package net.judah.looper;

import java.security.InvalidParameterException;

import lombok.Getter;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;

public class LoopCmdr implements Cmdr {

	@Getter private final String[] keys;

	private final Looper looper;

	public LoopCmdr(Looper loops) {
		this.looper = loops;
		keys = new String[looper.size()];
		for (int i = 0; i < looper.size(); i++) {
			keys[i] = looper.get(i).getName();
		}
	}

	@Override
	public Loop resolve(String key) {
		for (int i = 0; i < looper.size(); i++)
			if (looper.get(i).getName().equals(key))
				return looper.get(i);
		return null;
	}

	@Override
	public void execute(Param p) {
		Loop loop = resolve(p.val);
		if (loop == null)
			return;
		switch (p.getCmd()) {
		case Delete:
			looper.clear(loop);
			break;
		case Dup:
			loop.duplicate();
			break;
		case Record:
			if (false == looper.getClock().isActive())
				break;
			loop.trigger();
			break;
		case RecEnd:
			loop.capture(false);
			break;
		case Sync:
			looper.onDeck(loop);
			break;
		default:
			throw new InvalidParameterException("" + p);
		}
	}

}
