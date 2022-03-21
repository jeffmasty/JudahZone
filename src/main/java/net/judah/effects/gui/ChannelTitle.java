package net.judah.effects.gui;

import java.awt.Color;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.util.Pastels;

public class ChannelTitle extends JPanel {

	private final Mute mute;
	
	private final PresetCheckBox presetActive = new PresetCheckBox();
	
	private final Channel channel;
	private final JLabel name;
	
	public ChannelTitle(Channel channel) {
		this.channel = channel;
		name = new JLabel(channel.getName(), JLabel.CENTER);
		if (channel.getIcon() != null)
			add(new JLabel(channel.getIcon(), JLabel.CENTER));
		add(name);
		
		add(new JLabel("  fx;"));
		add(presetActive);
		add(new JLabel("  mute:"));
		mute = new Mute();
		add(mute); 
		
	}

	private class PresetCheckBox extends JCheckBox {
        PresetCheckBox() {
            addItemListener(e -> {
                if (isSelected()) {
                    setBackground(Color.GREEN);
                    setOpaque(true);
                }
                else {
                    setOpaque(false);
                }
                if (channel.getPreset() == null) return;
                channel.getPreset().applyPreset(channel, isSelected());
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
	}

	
}
