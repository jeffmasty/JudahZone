package net.judah.clock;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.clock.JudahClock.Mode;
import net.judah.util.*;

public class ClockGui extends JPanel implements ActionListener, TimeListener {
	
	private final JudahClock clock;
	private JLabel tempoLbl;
	private Slider tempo;
    private JToggleButton start;
    private ToggleSwitch mode = new ToggleSwitch(this);
    private JLabel beatLbl = new JLabel("Beat:", JLabel.CENTER);
    private JLabel beat = new JLabel("0", JLabel.CENTER);
    private JPanel bpmPnl = new JPanel();
	private final JLabel syncLbl;
	
	private final JudahMenu popup = new JudahMenu();

	private final JComboBox<String> timeSignature = new JComboBox<>();
	
    ClockGui(JudahClock clock) {
    	this.clock = clock;
    	clock.addListener(this);
        tempo = new Slider(35, 175, e -> {
            clock.setTempo(((Slider)e.getSource()).getValue());});
        JPanel tempoPnl = new JPanel();
        Dimension spacer = new Dimension(6, 6);
        tempoPnl.setLayout(new BoxLayout(tempoPnl, BoxLayout.X_AXIS));
        tempoPnl.add(Box.createRigidArea(spacer));
        tempoPnl.add(tempo);
        tempoPnl.add(Box.createRigidArea(spacer));
        tempoPnl.add(mode);
        mode.setPreferredSize(new Dimension(50, 20));
        mode.setMaximumSize(new Dimension(50, 20));
        tempoPnl.add(Box.createRigidArea(spacer));
        

        
        TapTempo tapButton = new TapTempo("Tempo: ", msec -> {
            clock.setTempo(60000 / msec);
        });
        tempoLbl = new JLabel("" + clock.getTempo(), JLabel.CENTER);
        tempoLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                String input = Constants.inputBox("Tempo:");
                if (input == null || input.isEmpty()) return;
                try { clock.setTempo(Float.parseFloat(input));
                } catch (Throwable t) { Console.info(t.getMessage() + " " + input); }
            }});
        tempoLbl.setFont(Constants.Gui.BOLD);

        
        bpmPnl = new JPanel(new GridLayout(1, 6));
        bpmPnl.add(tapButton);
        bpmPnl.add(tempoLbl);
        bpmPnl.add(new JLabel("Sync:", JLabel.CENTER));
        syncLbl = new JLabel("" + JudahClock.getLength(), JLabel.CENTER);
        syncLbl.setFont(Constants.Gui.BOLD);
        
        bpmPnl.add(syncLbl);
        
        
        for (String s : JudahClock.getTimeSignatures())
        	timeSignature.addItem(s);
        timeSignature.setAlignmentX(0.5f);
        bpmPnl.add(new JLabel("Style:", JLabel.CENTER));
        bpmPnl.add(timeSignature);


        JButton menu = new JButton("Menu");
        menu.addActionListener(e -> {
            popup.show(menu, menu.getLocation().x, menu.getLocation().y);
        });
        start = new JToggleButton("Start");
        start.setSelected(clock.isActive());
        start.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (clock.isActive()) {
                    clock.reset();
                }
                else
                    clock.begin();
            }
        });
		JPanel startPnl = new JPanel(new GridLayout(1, 3));
        startPnl.add(menu); 
        startPnl.add(start);
        JPanel beatPnl = new JPanel();
        beatPnl.setLayout(new GridLayout(1,2));
        beatLbl.setOpaque(true);
        beat.setOpaque(true);
        beatPnl.add(beatLbl);
        beatPnl.add(beat);
        startPnl.add(beatPnl);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(startPnl);

        add(bpmPnl);
        add(tempoPnl);

        add(Box.createVerticalGlue());
        doLayout();

        update(null, null);
    }
    
	@Override
	public void actionPerformed(ActionEvent e) {
		JudahClock.setMode(mode.isActivated() ? Mode.Internal : Mode.Midi24);
	}

	@Override
	public void update(Property prop, Object value) {
		if (Property.TEMPO == prop) { 
			tempo.setValue(Math.round(clock.getTempo()));
			tempoLbl.setText("" + value);
		}
		else if (Property.TRANSPORT == prop) {
			start.setText(clock.isActive() ? "Stop" : "Start");
		}
		else if (Property.BEAT == prop) {
			beat.setText(value.toString());	
		}
		mode.setActivated(JudahClock.getMode() == Mode.Internal);
		
//		if (JudahClock.getSteps() != (Integer)steps.getSelectedItem())
//            steps.setSelectedItem(JudahClock.getSteps());
//        if (JudahClock.getSubdivision() != div.getSelectedIndex())
//        	div.setSelectedItem(JudahClock.getSubdivision());
			
// TODO
//			if (Property.BEAT == prop)
//				new Thread( () -> {
//					beatLbl.setText(value.toString());
//					beatLbl.repaint();
//				}).start();
//			else if (Property.TRANSPORT == prop) {
//				if (value == JackTransportState.JackTransportStopped && gnome != null) {
//					gnome.stop();
//				}
		}

	public void blink(boolean on) {
		beatLbl.setBackground(on ? clock.isActive() ? Pastels.GREEN : Pastels.BLUE : Pastels.EGGSHELL);
		beat.setBackground(on ? clock.isActive() ? Pastels.GREEN : Pastels.BLUE : Pastels.EGGSHELL);
	//	bpmPnl.setBackground(on ? clock.isActive() ? Pastels.GREEN : Pastels.BLUE : Pastels.EGGSHELL);
	}

	public void sync(int bars) {
		syncLbl.setText("" + bars);
	}

//    private void createSteps() {
//        steps = new JComboBox<>();
//        for (int i = 4; i < 33; i++) steps.addItem(i);
//        steps.setSelectedItem(JudahClock.getSteps());
//        steps.setFont(Constants.Gui.FONT11);
//        steps.addActionListener(e ->{
//            if (inUpdate) return;
//            clock.setSteps((int)steps.getSelectedItem());
//            BeatsView.getInstance().getGrid2().repaint();
//            });
//
//        div = new JComboBox<>();
//        for (int i = 2; i < 7; i++) div.addItem(i);
//        div.setSelectedItem(JudahClock.getSubdivision());
//        div.setFont(Constants.Gui.FONT11);
//        div.addActionListener(e -> {
//            if (inUpdate) return;
//            clock.setSubdivision((int)div.getSelectedItem());
//            BeatsView.getInstance().getGrid2().repaint();
//            });
//
//        JPanel stepsPnl = new JPanel(new GridLayout(1, 2));
//        stepsPnl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        stepsPnl.add(new JLabel("steps", JLabel.CENTER));
//        stepsPnl.add(steps);
//        JPanel divPnl = new JPanel(new GridLayout(1, 2));
//        divPnl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        divPnl.add(new JLabel("div.", JLabel.CENTER));
//        divPnl.add(div);
//        add(stepsPnl);
//        add(divPnl);
//    }

}
