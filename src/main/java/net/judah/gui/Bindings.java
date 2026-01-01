package net.judah.gui;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import judahzone.api.Effect;
import judahzone.gui.Pastels;
import net.judah.channel.Channel;
import net.judah.channel.Mains;
import net.judah.fx.Chorus;
import net.judah.fx.Compressor;
import net.judah.fx.Convolution;
import net.judah.fx.Delay;
import net.judah.fx.EQ;
import net.judah.fx.Filter;
import net.judah.fx.Gain;
import net.judah.fx.MonoFilter;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;
import net.judah.midi.JudahMidi;
import net.judah.midi.LFO;
import net.judah.mixer.DJJefe;
import net.judah.seq.Seq;

// TODO extra looper commands?
// ctrl-enter = next song?
public class Bindings {
	final InputMap inputs;
	final ActionMap actions;
	final Mains mains;


	public static class Act extends AbstractAction {
		final Runnable doIt;
		public Act(Runnable r) {
			doIt = r;
			putValue(NAME, toString());
		}
		@Override public void actionPerformed(ActionEvent e) {
			doIt.run();
		}
	}

	public Bindings(JFrame view, Seq seq, DJJefe mixer) {
		JComponent jc = (JComponent)view.getContentPane();
		inputs = jc.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		actions = jc.getActionMap();
		mains = mixer.getMains();

		ArrayList<Channel> channels = mixer.getChannels();
		int limit = channels.size() < 9 ? channels.size() : 9;

		for (int i = 0; i < limit; i++) {
			final Channel focus = channels.get(i);
			bind(getKeyStroke("F" + (i+1)), new Act(()->MainFrame.setFocus(focus)));
		}
		bind(getKeyStroke("F10"), new Act(()->MainFrame.setFocus(mixer.getMains())));

//		TrackList<DrumTrack> drums = seq.getDrumTracks();
//		bind(getKeyStroke(VK_1, ALT_DOWN_MASK), new Act(()->TabZone.edit(drums.get(0))));
//		bind(getKeyStroke(VK_2, ALT_DOWN_MASK), new Act(()->TabZone.edit(drums.get(1))));
//		bind(getKeyStroke(VK_3, ALT_DOWN_MASK), new Act(()->TabZone.edit(drums.get(2))));
//		bind(getKeyStroke(VK_4, ALT_DOWN_MASK), new Act(()->TabZone.edit(drums.get(3))));

//		TrackList<PianoTrack> synths = seq.getSynthTracks();
//		bind(getKeyStroke(VK_5, ALT_DOWN_MASK), new Act(()->TabZone.edit(synths.get(0))));
//		bind(getKeyStroke(VK_6, ALT_DOWN_MASK), new Act(()->TabZone.edit(synths.get(1))));
//		bind(getKeyStroke(VK_7, ALT_DOWN_MASK), new Act(()->TabZone.edit(synths.get(2))));
//		bind(getKeyStroke(VK_8, ALT_DOWN_MASK), new Act(()->TabZone.edit(synths.get(3))));
//		bind(getKeyStroke(VK_9, ALT_DOWN_MASK), new Act(()->TabZone.edit(synths.get(4))));
//		bind(getKeyStroke(VK_0, ALT_DOWN_MASK), new Act(()->TabZone.edit(synths.get(5))));
		bind(getKeyStroke(VK_BACK_SPACE, 0), new Act(()->mains.toggleMute()));
		bind(getKeyStroke(VK_UP, CTRL_DOWN_MASK), new Act(()->volume(true)));
		bind(getKeyStroke(VK_DOWN, CTRL_DOWN_MASK), new Act(()->volume(false)));
		bind(getKeyStroke(VK_ENTER, 0), new Act(()->JudahMidi.getClock().toggle()));
	}

	private void bind(KeyStroke k, Action a) {
		inputs.put(k, k.toString());
		actions.put(k.toString(), a);
	}

	private void volume(boolean up) {
		  int vol = mains.getVolume();
		  vol += up? 5 : -5;
		  if (vol > 100) vol = 100;
		  if (vol < 0) vol = 0;
		  mains.getGain().set(Gain.VOLUME, vol);
		  MainFrame.update(mains);
	}

	public static Color getFx(Class<? extends Effect> class1) {
		if (Reverb.class.isAssignableFrom(class1))
			return Pastels.RED;
		if (Overdrive.class.equals(class1))
			return Pastels.YELLOW;
		if (Chorus.class.equals(class1))
			return Pastels.GREEN;
		if (MonoFilter.class.equals(class1))
			return Pastels.PINK;
		if (Filter.class.equals(class1))
			return Pastels.PINK;
		if (EQ.class.equals(class1))
			return Pastels.MY_GRAY;
		if (Delay.class.equals(class1))
			return Pastels.ORANGE;
		if (Compressor.class.equals(class1))
			return Pastels.PURPLE;
		if (LFO.class.equals(class1))
			return Pastels.BLUE;
		if (Convolution.class.equals(class1))
			return Color.BLACK;
		return Pastels.EGGSHELL;
	}
}
/*           case VK_R:
            	if (focus instanceof Loop) {
            		Loop loop = (Loop)focus;
            		if (loop == looper.getLoopA())
            			getLooper().getLoopA().trigger();
            		else
            			loop.capture(!loop.isRecording());
            	} else if (focus instanceof Instrument) {
            		Instrument line = (Instrument)focus;
            		line.setMuteRecord(!line.isMuteRecord());
            	}
            	return true;
            case VK_D:
            	if (focus instanceof Loop)
            		looper.clear((Loop)focus);
            	return true;
            case VK_X:
            	if (focus instanceof Loop)
            		((Loop)focus).clear();
            	return true;
 */
