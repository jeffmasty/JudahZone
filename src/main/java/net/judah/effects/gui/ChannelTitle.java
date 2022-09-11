package net.judah.effects.gui;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.util.Constants;
import net.judah.util.GuitarTuner;
import net.judah.util.Pastels;

public class ChannelTitle extends JPanel {

	private final Mute mute;
	private final PresetCheckBox presetActive = new PresetCheckBox();
	private final JToggleButton tunerBtn;
	
	private final Channel channel;
	private final JLabel name;
	
	public ChannelTitle(Channel channel) {
		JPanel main = new JPanel();
		
		this.channel = channel;
		name = new JLabel(channel.getName(), JLabel.CENTER);
		name.setFont(Constants.Gui.BOLD13);
		if (channel.getIcon() != null)
			main.add(new JLabel(channel.getIcon(), JLabel.CENTER));
		main.add(name);
		
		main.add(new JLabel(" fx:"));
		main.add(presetActive);
		main.add(new JLabel(" mute:"));
		mute = new Mute();
		main.add(mute); 
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(main);
		
		tunerBtn = (channel instanceof LineIn) ?
			 new JToggleButton("tuner") : null;
		if (tunerBtn != null) {
			tunerBtn.setSelected(false);
			tunerBtn.addChangeListener(e -> {
				ControlPanel.getInstance().getTuner()
						.setChannel(tunerBtn.isSelected() ? channel : null);
			});
			main.add(tunerBtn);
		}
	}

	private class PresetCheckBox extends JCheckBox {
        PresetCheckBox() {
            addItemListener(e -> {
            	setBackground(isSelected() ? Color.GREEN : null);
                channel.setPresetActive(isSelected());
            });
        }
    }

	
	
	private class Mute extends JCheckBox {
		boolean inUpdate = false;
		Mute() {
            addItemListener(e -> {
            	if (inUpdate) return;
                if (channel instanceof LineIn)
                	((LineIn)channel).setMuteRecord(isSelected());
                else
                	channel.setOnMute(isSelected());
                update();
            });
            
		}

		void update() {
			
			boolean muted = channel instanceof LineIn ? 
					((LineIn)channel).isMuteRecord() :
						channel.isOnMute();
			
			if (muted) {
                ChannelTitle.this.setBackground(Color.ORANGE);
                setOpaque(true);
            }
            else {
				ChannelTitle.this.setBackground(Pastels.EGGSHELL);
				setOpaque(false);
            }
			setSelected(muted);
		}
		
		void ignore() {
			inUpdate = true;
		}
		void listen() {
			inUpdate = false;
		}
	}

	public void update() {
		presetActive.setSelected(channel.isPresetActive());
		mute.ignore();
		mute.update();
		mute.listen();
		if (tunerBtn != null)
			tunerBtn.setSelected(GuitarTuner.getChannel() == channel);
	}

	
}
