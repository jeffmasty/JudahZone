package net.judah.mixer;

import javax.sound.midi.ShortMessage;

import judahzone.fx.Filter;
import net.judah.channel.Channel;
import net.judah.controllers.KorgPads;
import net.judah.gui.MainFrame;

public class DJFilter {

	private static final int HZ = Filter.Settings.Hz.ordinal();
	private final int LEFT = KorgPads.LEFT;
	private final int RIGHT = KorgPads.RIGHT;

	private final Channel ch;
	private final Filter loCut;
	private final Filter hiCut;

	public DJFilter(Channel channel, Filter lowPass, Filter hiPass) {
		ch = channel;
		loCut = hiPass;
		hiCut = lowPass;
	}

	public void joystick(ShortMessage m) {
		// convert to 100, run the 100(LFO) version
		int data2 = m.getData2();

		if (data2 > LEFT) {
			if (data2 < RIGHT)
				joystick(50);
			else
				data2 = data2 - RIGHT + 50;
		}
		joystick(data2);
	}

	public void joystick(int x) {

		if (x > 48 && x < 52) {
			loCut.set(HZ, 0);
			hiCut.set(HZ, 100);
			if (ch.isActive(loCut))
				ch.setActive(loCut, false);
			else
				MainFrame.updateFx(ch, loCut);
			if (ch.isActive(hiCut))
				ch.setActive(hiCut, false);
			else
				MainFrame.updateFx(ch, hiCut);
			return;
		}
		else if (x < 50) {
			// as x approaches 0 (from 50) hiCut goes to 0 (from 100) // x50=100, x25=50, x0=0
			hiCut.set(HZ, 2 * x);
			if (ch.isActive(loCut))
				ch.setActive(loCut, false);
			else
				MainFrame.updateFx(ch, hiCut);
			if (!ch.isActive(hiCut))
				ch.setActive(hiCut, true);
		}
		else {
			// as x approaches 100 (from 50) lowCut goes to 100 (from 0) // x50=0,  x75=50 x100=100
			loCut.set(HZ, (x - 50) * 2);
			if (!ch.isActive(loCut))
				ch.setActive(loCut, true);
			else MainFrame.updateFx(ch, loCut);

			if (ch.isActive(hiCut))
				ch.setActive(hiCut, false);

		}
	}

}
