package net.judah.mixer;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.clock.JudahClock;
import net.judah.looper.Loop;
import net.judah.midi.JudahMidi;
import net.judah.util.Pastels;
import net.judah.util.RainbowFader;

/**Mixer view
 * <pre>
  each Channel has:
		Menu
		Volume Fader 
		LEDs  {fx status}
		Channel Icon </pre>
 * @author judah */
public class ChannelFader extends JPanel implements Pastels {

	// status Mute, mute record, playing, stopped, recording, latched, default    purple blue green orange red
	// input mute mute record  defalut
	// output  stopped  mute playing  latched  recording
	
	@Getter private final Channel channel;
	private final MixWidget icon;
	@Getter protected final Menu menu;
	@Getter private RainbowFader volume;
	private final LEDs indicators;
	
	public ChannelFader(Channel channel) {
		this.channel = channel;
		setBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		icon = new Thumbnail(channel);
		indicators = new LEDs(channel);

		
		volume = new RainbowFader(vol -> {
			channel.getGain().setVol(volume.getValue());
			MainFrame.update(channel);
		});
		JPanel volPnl = new JPanel();
		volPnl.add(volume);
		//add(volume);

		menu = (channel instanceof Loop) ?
			new SyncWidget((Loop)channel) : new Menu(channel);

		if (channel instanceof LineIn) {
			add(icon);
			add(indicators);
			add(volPnl);
			add(menu);
		} else {
			add(menu);
			add(indicators);
			add(volPnl);
			add(icon);
		}
		
	}

	public void update() {
		indicators.sync();		
		Color bg = BLUE;
		
		if (channel instanceof Loop) {
			Loop s = (Loop) channel;
			if (channel instanceof Loop && (((Loop)channel).isRecording() == AudioMode.RUNNING)) 
				bg = RED;
			else if (s.isOnMute())
				bg = PURPLE;
			else if (s.hasRecording() && s.isPlaying() == AudioMode.STOPPED)
				bg = Color.DARK_GRAY;
			else if (s.isPlaying() == AudioMode.RUNNING)
				bg = GREEN;
			else if (s.isSync()) 
				bg = ORANGE;
			else {
				Loop a = JudahZone.getLooper().getLoopA();
				if (a == channel && JudahClock.isLoopSync())
					bg = ORANGE;
				else if (a != channel && s.getPrimary() != null) {
					bg = PINK;
				}
			}
		}
		else { // line in/master track // TODO upper and lower indicator color
			if (channel.isOnMute()) 
				bg = Color.BLACK;
			else if (channel instanceof LineIn && ((LineIn)channel).isMuteRecord()) {
				bg = PURPLE;
				if (channel.getSync() != null && JudahMidi.getInstance().getSync().contains(channel.getSync()))
					bg = YELLOW;
			}
			else if (channel.getSync() != null && JudahMidi.getInstance().getSync().contains(channel.getSync()))
				bg = PINK;
			else 
				bg = GREEN;
		}
		
		if (false == getBackground().equals(bg)) {
			setBackground(bg);
		}

		updateVolume();
		if (menu instanceof SyncWidget)
			((SyncWidget)menu).update();
		
	}
	
	public void updateVolume() {
		if (channel.getVolume() != volume.getValue()) {
			volume.setValue(channel.getVolume());
			volume.repaint();
		}
		
	}
	
}
