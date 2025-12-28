package net.judah.song.cmd;

import java.security.InvalidParameterException;
import java.util.List;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.mixer.Channel;
import net.judah.mixer.Fader;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.seq.Trax;

public class MixCmd implements Cmdr {

	@Getter private static final Line line = new Line();
	@Getter private static final MixCmd instance = new MixCmd();

	@Getter private final String[] keys;

	private final List<Channel> channels;
	/*
	private static final DJJefe mixer = JudahZone.getMixer();
	private static final List<Channel> channels = mixer.getAll();
	private static final Looper looper = JudahZone.getLooper();
	new String[channels.size()]; //mixer.getChannels()
	 */

	MixCmd() {
		channels = JudahZone.getInstance().getMixer().getAll();
		keys = new String[channels.size()];
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
				JudahZone.getInstance().getLooper().syncFx(ch);
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
					JudahZone.getInstance().getLooper().getSoloTrack().setSoloTrack(line);
				break;

			default: throw new InvalidParameterException("" + p);
		}
	}


	static class Line implements Cmdr {

		final Zone instruments = JudahZone.getInstance().getInstruments();
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
						JudahZone.getInstance().getLooper().getSoloTrack().setSoloTrack(line);
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
