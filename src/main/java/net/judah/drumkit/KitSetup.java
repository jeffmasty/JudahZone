package net.judah.drumkit;

import javax.sound.midi.ShortMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import judahzone.api.Midi;
import judahzone.util.Constants;
import lombok.Data;
import net.judah.JudahZone;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.seq.automation.ControlChange;
import net.judah.song.Overview;

@Data
public class KitSetup implements AtkDec {

	static final int LENGTH = DrumType.values().length;

	@JsonIgnore Gain[] gain = new Gain[LENGTH];
	int[] vol = new int[LENGTH];
	int[] pan = new int[LENGTH];
	int[] atk, dk;
	@JsonIgnore boolean cloned;

	public KitSetup() {
		for (int i = 0; i < gain.length; i++)
			gain[i] = new Gain();
		atk = new int[] {1, 1, 1, 1, 1, 1, 1, 1};
		dk = new int[] {100, 100, 100, 100, 100, 100, 100, 100};
	}

	/** JSON output only (no Gain) */
	public KitSetup(KitSetup in) {
		atk = in.atk.clone();
		dk = in.dk.clone();
		for (int i = 0; i < in.gain.length; i++) {
			vol[i] = in.gain[i].get(Gain.VOLUME);
			pan[i] = in.gain[i].get(Gain.PAN);
		}
	}

	/** from Song JSON */
	public void clone(KitSetup in) {
		atk = in.atk;
		dk = in.dk;
		for (int i = 0; i < LENGTH; i++) {
			gain[i].set(Gain.VOLUME, in.vol[i]);
			gain[i].set(Gain.PAN, in.pan[i]);
		}
		cloned = true;
		MainFrame.update(this);
	}

	public void reset() {
		for (Gain g : gain)
			g.reset();
		for (int i = 0; i < atk.length; i++) {
			atk[i] = 1;
			dk[i] = 100;
		}
		cloned = false;
		MainFrame.update(this);
	}

	public void serialize() {
		Overview overview = JudahZone.getInstance().getOverview();
		overview.getSong().setKit(new KitSetup(this));
		overview.save();
	}

	@Override public int getAtk(int typeIdx) {
		return atk[typeIdx];
	}

	@Override public int getDk(int typeIdx) {
		return dk[typeIdx];
	}

	@Override public int getPan(int typeIdx) {
		return gain[typeIdx].get(Gain.PAN);
	}

	@Override public int getVol(int typeIdx) {
		return gain[typeIdx].get(Gain.VOLUME);
	}

	@Override public void setVol(int type, int val) {
		gain[type].set(Gain.VOLUME, val);
	}

	@Override public void setPan(int type, int val) {
		gain[type].set(Gain.PAN, val);
	}

	@Override public void setAtk(int type, int val) {
		atk[type] = val;
	}

	@Override public void setDk(int type, int val) {
		dk[type] = val;
	}

	/**
	 * @param cc
	 * @return true if the message is filtered by this envelope
	 */
	public boolean cc(ShortMessage cc) {
		if (!Midi.isCC(cc))
			return false;

		int data1 = cc.getData1();
		if (data1 == ControlChange.ATTACK.data1) {
			int val = (int) (cc.getData2() * Constants.TO_100);
			for (int i = 0; i < LENGTH; i++)
				atk[i] = val;
		}
		else if (data1 == ControlChange.DECAY.data1 || data1 == ControlChange.RELEASE.data1) { // DK2
			int val = (int) (cc.getData2() * Constants.TO_100);
			for (int i = 0; i < LENGTH; i++)
				dk[i] = val;
		} else
			return false;
		MainFrame.update(this);
		return true;
	}

}
