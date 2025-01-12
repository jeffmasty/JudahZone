package net.judah.scope;

/*
*
*  TarsosDSP is developed by Joren Six at
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
*  http://tarsos.0110.be/tag/TarsosDSP
*
*/


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.sound.sampled.Mixer;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import net.judah.omni.AudioTools;

public class InputPanel extends JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	Mixer mixer = null;

	public InputPanel(){
		super(new BorderLayout());
		this.setBorder(new TitledBorder("1. Choose a microphone input"));
		JPanel buttonPanel = new JPanel(new GridLayout(0,1));
		ButtonGroup group = new ButtonGroup();
		for(Mixer.Info info : AudioTools.getMixerInfo(false, true)){

			JRadioButton button = new JRadioButton();
			button.setText(info.toString());
			buttonPanel.add(button);
			group.add(button);
			button.setActionCommand(info.toString());
//			button.addActionListener(setInput);
		}
		this.add(new JScrollPane(buttonPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),BorderLayout.CENTER);
		this.setMaximumSize(new Dimension(300,150));
		this.setPreferredSize(new Dimension(300,150));
	}

//	private ActionListener setInput = arg0 -> {
//		for(Mixer.Info info : Shared.getMixerInfo(false, true)){
//			if(arg0.getActionCommand().equals(info.toString())){
//				Mixer newValue = AudioSystem.getMixer(info);
//				this.firePropertyChange("mixer", mixer, newValue);
//				this.mixer = newValue;
//			}
//		}
//	};

}
