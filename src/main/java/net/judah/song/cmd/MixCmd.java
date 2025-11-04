package net.judah.song.cmd;

import java.security.InvalidParameterException;
import java.util.List;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Fader;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.seq.Trax;

public class MixCmd implements Cmdr {

	private static final DJJefe mixer = JudahZone.getMixer();
	private static final List<Channel> channels = mixer.getAll();
	private static final Looper looper = JudahZone.getLooper();
	@Getter private static final Line line = new Line();
	@Getter private static final MixCmd instance = new MixCmd();

	@Getter private final String[] keys = new String[channels.size()]; //mixer.getChannels()

	MixCmd() {
		//mixer.getChannels()
	    for (int i = 0; i < keys.length; i++)
	    	keys[i] = channels.get(i).getName();
	}

	@Override
	public Channel resolve(String key) {
		for (Channel ch : channels)
			if (ch.getName().equals(key))
				return ch;
		for (Trax legacy : Trax.values())
			if (legacy.getName().equals(key))
				return resolve(legacy.toString());
		return null;
	}

	@Override
	public void execute(Param p) {
		Channel ch = resolve(p.val);
		if (ch == null)
			return;
		switch (p.cmd) {
			case FadeOut:
				Fader.execute(new Fader(ch, Fader.DEFAULT_FADE, ch.getVolume(), 0));
				break;
			case FadeIn:
				Fader.execute(new Fader(ch, Fader.DEFAULT_FADE, 0, 51));
				break;
			case FX:
				ch.toggleFx();
				break;
			case Mute:
				ch.setOnMute(true);
				break;
			case Unmute:
				ch.setOnMute(false);
				break;
			case Latch:
				looper.syncFx(ch);
				break;

			case OffTape:
				if (ch instanceof LineIn line)
					line.setMuteRecord(true);
				break;
			case OnTape:
				if (ch instanceof LineIn line)
					line.setMuteRecord(false);
				break;
			case SoloCh:
				if (ch instanceof LineIn line)
					looper.getSoloTrack().setSoloTrack(line);
				break;

			default: throw new InvalidParameterException("" + p);
		}
	}


	static class Line implements Cmdr {

		final Zone instruments = JudahZone.getInstruments();
		@Getter private String[] keys = new String[instruments.size()];

		Line() {
			for (int i = 0; i < keys.length; i++)
				keys[i] = instruments.get(i).getName();
		}

		@Override public void execute(Param p) {
			Channel ch = resolve(p.val);
			if (ch == null)
				return;
			switch (p.cmd) {
				case OffTape:
					if (ch instanceof LineIn line)
						line.setMuteRecord(true);
					break;
				case OnTape:
					if (ch instanceof LineIn line)
						line.setMuteRecord(false);
					break;
				case SoloCh:
					if (ch instanceof LineIn line)
						looper.getSoloTrack().setSoloTrack(line);
					break;

				default: throw new InvalidParameterException("" + p);
				}
			}

			@Override public Channel resolve(String key) {
				Channel ch = instruments.byName(key);
				if (ch != null)
					return ch;
				for (Trax legacy : Trax.values())
					if (legacy.getName().equals(key))
						return resolve(legacy.toString());
				return null;
			}
		}

}
