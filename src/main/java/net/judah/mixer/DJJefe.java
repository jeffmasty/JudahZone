package net.judah.mixer;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
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
public class DJJefe extends JPanel implements Pastels {

	// status Mute, mute record, playing, stopped, recording, latched, default    purple blue green orange red
	// input mute mute record  defalut
	// output  stopped  mute playing  latched  recording
	
	@Getter private final Channel channel;
	private final MixWidget icon;
	private final MixWidget menu;
	private RainbowFader volume;
	private final LEDs indicators;
	
	public DJJefe(Channel channel) {
		this.channel = channel;
		channel.setFader(this);
		setBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		menu = new Menu(channel);
		add(menu);
		
		volume = new RainbowFader(vol -> {
			channel.getGain().setVol(volume.getValue());
			MainFrame.update(channel);
		});
		volume.setValue(channel.getGain().getVol());
		JPanel temp = new JPanel();
		temp.add(volume);
		add(temp);
		//add(volume);

		indicators = new LEDs(channel);
		add(indicators);

		icon = new Thumbnail(channel);
		add(icon);
		
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
				bg = PURPLE;
			else if (((Recorder)channel).isSync())
				bg = ORANGE;
			else if (s.isPlaying() == AudioMode.RUNNING)
				bg = GREEN;
		}
		else { // line in/master track
			if (channel.isOnMute()) 
				bg = PURPLE;
			else if (channel instanceof LineIn && ((LineIn)channel).isMuteRecord())
				bg = BLUE;
			else if (false == getBackground().equals(GREEN))
				bg = GREEN;
		}
		
		if (false == getBackground().equals(bg)) {
			setBackground(bg);
		}
		
		if (channel.getVolume() != volume.getValue())
			volume.setValue(channel.getVolume());
	}
	
}
