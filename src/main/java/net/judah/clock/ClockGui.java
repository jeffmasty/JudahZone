package net.judah.clock;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.MainFrame;
import net.judah.clock.JudahClock.Mode;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahMenu;
import net.judah.util.Slider;
import net.judah.util.TapTempo;
import net.judah.util.ToggleSwitch;

public class ClockGui extends JPanel implements ActionListener {
	
	// latch
	// stomp
	// song mode 
	// drum song
	
	private final JudahClock clock;
	private JLabel tempoLbl;
	private Slider tempo;
    private JToggleButton start;
    private ToggleSwitch mode = new ToggleSwitch(this);
	
	final Dimension combo = new Dimension(MainFrame.WIDTH_CLOCK / 2, 19);
		final JudahMenu popup = new JudahMenu();
		
        ClockGui(JudahClock clock) {
        	this.clock = clock;
            tempo = new Slider(35, 175, e -> {
                clock.setTempo(((Slider)e.getSource()).getValue());});
            JPanel tempoPnl = new JPanel();
            tempoPnl.setLayout(new BoxLayout(tempoPnl, BoxLayout.X_AXIS));
            
            tempoPnl.add(Box.createRigidArea(new Dimension(6, 6)));
            tempoPnl.add(tempo);
            tempoPnl.add(Box.createRigidArea(new Dimension(6, 6)));

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

            JPanel bpmPnl = new JPanel(new GridLayout(1, 2));
            bpmPnl.add(tapButton);
            bpmPnl.add(tempoLbl);

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
            JPanel startPnl = new JPanel(new GridLayout(1, 2));
            startPnl.add(menu); startPnl.add(start);

            // setLayout(new GridLayout(5, 1, 3, 0));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(startPnl);

            add(mode);

//            JPanel logPnl = new JPanel(new GridLayout(0, 3, 0, 2));
//            JToggleButton log = new JToggleButton("log");
//            log.setFont(Constants.Gui.FONT9);
//            JToggleButton seq = new JToggleButton("seq");
//            seq.setFont(Constants.Gui.FONT9);
//            JToggleButton fx = new JToggleButton("fx");
//            fx.setFont(Constants.Gui.FONT9);
//            logPnl.add(seq); //seq.setVisible(false); // TODO
//            logPnl.add(fx);  // fx.setVisible(false); // not implemented yet
//            logPnl.add(log); // log.setVisible(false);
//            add(logPnl);
            
            add(bpmPnl);
            add(tempoPnl);
            add(Box.createVerticalGlue());
            doLayout();
            update();
        }
        
        void update() { 
        	new Thread() { @Override public void run() {
        			tempo.setValue(Math.round(clock.getTempo()));
        			tempoLbl.setText("" + clock.getTempo());
        			start.setText(clock.isActive() ? "Stop" : "Start");
        			mode.setActivated(JudahClock.getMode() == Mode.Internal);
        			mode.invalidate();
        			repaint();
            	}}.start();
        	}

		@Override
		public void actionPerformed(ActionEvent e) {
			JudahClock.setMode(mode.isActivated() ? Mode.Internal : Mode.Midi24);
		}
    }
