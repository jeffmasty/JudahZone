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
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
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
		add(icon);
		indicators = new LEDs(channel);
		add(indicators);
		
		volume = new RainbowFader(vol -> {
			channel.getGain().setVol(volume.getValue());
			MainFrame.update(channel);
		});
		JPanel temp = new JPanel();
		temp.add(volume);
		add(temp);
		//add(volume);

		menu = (channel instanceof Recorder) ?
			new SyncWidget((Recorder)channel) : new Menu(channel);
		add(menu);

		
	}

	public void update() {
		indicators.sync();		
		Color bg = BLUE;
		
		if (channel instanceof Sample) {
			Sample s = (Sample) channel;
			if (channel instanceof Recorder && (((Recorder)channel).isRecording() == AudioMode.RUNNING)) 
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
				Recorder a = JudahZone.getLooper().getLoopA();
				if (a == channel && JudahClock.isLoopSync())
					bg = ORANGE;
				else if (a != channel && ((Recorder)s).getPrimary() != null) {
					bg = PINK;
				}
			}
		}
		else { // line in/master track
			if (channel.isOnMute()) 
				bg = PURPLE;
			else if (channel instanceof LineIn && ((LineIn)channel).isMuteRecord())
				bg = ORANGE;
			else if (channel == JudahZone.getChannels().getCrave() && JudahClock.getInstance().isSyncCrave())
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
