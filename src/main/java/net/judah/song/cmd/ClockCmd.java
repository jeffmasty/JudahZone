package net.judah.song.cmd;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

public class ClockCmd {

	static JudahClock clock = JudahZone.getClock();
	@Getter static Start start = new Start();
	@Getter static Tempo tempo = new Tempo();
	@Getter static Length length = new Length();
	@Getter static Swing swing = new Swing();

	static class Start extends BooleanProvider {
		@Override public void execute(Param p) {
			if (p.cmd == Cmd.Start) {
				if (resolve(p.val))
					clock.begin();
				else
					clock.end();
			}
		}
	}

	static class Tempo extends IntProvider {
		Tempo() {
			super(40, 200, 2);
		}
		@Override public void execute(Param p) {
			try {
				clock.setTempo(Integer.parseInt(p.val));
			} catch (NumberFormatException e) {RTLogger.warn(this, "tempo: " + p.val);}
		}
	}

	static class Length extends IntProvider {
		public Length() {
			super (1, 64, 1);
		}
		@Override public void execute(Param p) {
			clock.setLength(Integer.parseInt(p.val));
		}
	}

	static class Swing extends IntProvider {
		public Swing() {
			super(-50, 50, 5);
		}
		@Override
		public void execute(Param p) {
			try {
				clock.setSwing(.01f * Integer.parseInt(p.val));
			} catch (NumberFormatException e) { RTLogger.warn(this, e); }
		}

	}

}
