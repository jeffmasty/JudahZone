package net.judah.song.cmd;

import java.security.InvalidParameterException;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.looper.Loop;
import net.judah.looper.LoopType;
import net.judah.looper.Looper;
import net.judah.midi.JudahMidi;

public class LoopCmd implements Cmdr {

	@Getter private static LoopCmd instance;

	private final Looper looper = JudahZone.getInstance().getLooper();
	@Getter private static final Solo solo = new Solo();
	@Getter private static final Sync sync = new Sync();
	@Getter private final String[] keys;

	public LoopCmd() {
		keys = new String[looper.size()];
		for (int i = 0; i < looper.size(); i++) {
			keys[i] = looper.get(i).getName();
		}
		instance = this;
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
		if (loop == null) {
			if (p.getCmd() == Cmd.Sync)
				JudahMidi.getClock().syncToLoop(looper.getPrimary());
			return;
		}
		switch (p.getCmd()) {
		case Delete:
			looper.delete(loop);
			break;
		case Dup:
			loop.duplicate();
			break;
		case Record:
			if (looper.getPrimary() != null && looper.getType() == LoopType.FREE)
				looper.trigger(loop);
			else if (JudahMidi.getClock().isActive()) { // clock not active = non-performance developing songs
				looper.trigger(loop);
			}
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

	static class Sync extends LoopCmd {
    	private String[] keys;
		@Override public String[] getKeys() {
			if (keys == null) {
				keys = new String[super.getKeys().length + 1];
				keys[keys.length - 1] = "Tempo";
				for (int i = 0; i < super.getKeys().length; i++)
					keys[i] = super.getKeys()[i];
			}
			return keys;
		}
	}

	static class Solo extends BooleanProvider {
		@Override public void execute(Param p) {
			JudahZone.getInstance().getLooper().getSoloTrack().solo(resolve(p.val));
		}
	}

}
