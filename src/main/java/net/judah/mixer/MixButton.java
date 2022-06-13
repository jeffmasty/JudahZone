package net.judah.mixer;

import javax.swing.JLabel;

import lombok.Getter;
import lombok.ToString;
import net.judah.api.AudioMode;
import net.judah.looper.Loop;
import net.judah.sequencer.Sequencer;
import net.judah.util.Icons;

@ToString
public class MixButton extends JLabel {
	
	@Getter private final String name;
	private final Icons.Pair icon;
	private final Channel channel;
	
	public MixButton(Icons.Pair icon, Channel target) {
		this.name = icon.getName(); 
		this.icon = icon;
		this.channel = target;
		update();
	}

	public void update() {
		if (icon == Icons.MUTE) 
			setSelected(!channel.isOnMute());
		else if (icon == Icons.MUTE_RECORD)
			setSelected( (! ((LineIn)channel).isMuteRecord()));
		else if (icon == Icons.MICROPHONE && channel instanceof Loop) {
			AudioMode mode = ((Loop)channel).isRecording();
			if (AudioMode.STOPPED == mode || AudioMode.NEW == mode || AudioMode.ARMED == mode || AudioMode.STOPPING == mode)
				setSelected(false);
			else setSelected(true);
		}
		else if (icon == Icons.PLAY) {
			if (channel instanceof MasterTrack) {
				Sequencer seq = Sequencer.getCurrent();
				if (seq == null) {
					setSelected(false);
					return;
				}
				setSelected(seq.isRunning());
			}
			else if (channel instanceof Loop) {
				AudioMode mode = ((Loop)channel).isPlaying();
				if (AudioMode.STOPPED == mode || AudioMode.NEW == mode || AudioMode.ARMED == mode || AudioMode.STOPPING == mode)
					setSelected(false);
				else if (AudioMode.STARTING == mode || AudioMode.RUNNING == mode)
					setSelected(true);
			}
		}
	}
	
	public void setSelected(boolean active) {
		setIcon(active ? icon.getActive() : icon.getInactive());
	}

	public boolean isSelected() {
		return getIcon() == icon.getActive();
	}
	
}
