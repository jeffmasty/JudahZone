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
import net.judah.api.TimeListener;
import net.judah.clock.JudahClock.Mode;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahMenu;
import net.judah.util.Slider;
import net.judah.util.TapTempo;
import net.judah.util.ToggleSwitch;

public class ClockGui extends JPanel implements ActionListener, TimeListener {
	
	// latch
	// stomp
	// song mode 
	// drum song
	
	private final JudahClock clock;
	private JLabel tempoLbl;
	private Slider tempo;
    private JToggleButton start;
    private ToggleSwitch mode = new ToggleSwitch(this);
    
//    File test = new File("metronome/44_Minor_4-4_i_-III_iv_V.mid");
//    MidiPlayer playa;
//    JackReceiver jackMidi;
//    private JButton fileBtn = new JButton(test.getName());
    private JLabel beatLbl = new JLabel("0");
	
	final Dimension combo = new Dimension(MainFrame.WIDTH_CLOCK / 2, 19);
		final JudahMenu popup = new JudahMenu();
		
        ClockGui(JudahClock clock) {
        	this.clock = clock;
        	clock.addListener(this);
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

            JPanel bpmPnl = new JPanel(new GridLayout(1, 4));
            bpmPnl.add(tapButton);
            bpmPnl.add(tempoLbl);
            bpmPnl.add(new JLabel("Beat"));
            bpmPnl.add(beatLbl);

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

            add(bpmPnl);
            add(tempoPnl);

//            JPanel testPnl = new JPanel(new GridLayout(0, 3, 0, 2));
//            testPnl.add(beatLbl);
//            testPnl.add(new JLabel("[crave]"));
//            testPnl.add(fileBtn);
//            add(testPnl);
//            fileBtn.addActionListener(e -> {gnome.stop();});

            add(Box.createVerticalGlue());
            doLayout();

            update(null, null);
        }
        
//        void update() { 
//        }

		@Override
		public void actionPerformed(ActionEvent e) {
			JudahClock.setMode(mode.isActivated() ? Mode.Internal : Mode.Midi24);
		}

		@Override
		public void update(Property prop, Object value) {
			if (Property.TEMPO == prop) { 
				tempo.setValue(Math.round(clock.getTempo()));
				tempoLbl.setText("" + clock.getTempo());
			}
			else if (Property.TRANSPORT == prop) {
				start.setText(clock.isActive() ? "Stop" : "Start");
			}
			if (mode.isActivated() != (JudahClock.getMode() == Mode.Internal))
				mode.setActivated(JudahClock.getMode() == Mode.Internal);
			beatLbl.setText("" + JudahClock.getBeat());
			repaint();

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
//				else if (value == JackTransportState.JackTransportStarting) {
//					
//					
//					if (playa != null) playa.stop();
//					try {
//						if (gnome != null)
//							gnome.stop();
//							
//						gnome = new MidiGnome(clock, JudahMidi.getInstance());
//						gnome.setFile(test);
//						gnome.start();
//						
////						if (jackMidi == null)
////							jackMidi = new JackReceiver(JudahMidi.getInstance().getCraveOut());
////						playa = new MidiPlayer(test, 0, jackMidi);
////						playa.setTempo(clock.getTempo());
////						playa.start();
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//				update();
//			}
		}
    }
