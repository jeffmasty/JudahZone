package net.judah.clock;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.beatbox.BeatBox;
import net.judah.effects.gui.Slider;
import net.judah.looper.Recorder;
import net.judah.sequencer.Sequencer;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahMenu;
import net.judah.util.TapTempo;

public class JudahClock implements TimeProvider, TimeListener {

    @Getter private static JudahClock instance = new JudahClock();

    private final ArrayList<TimeListener> listeners = new ArrayList<>();
    TimeListener listener;

    @Getter private final ArrayList<BeatBox> sequencers = new ArrayList<>();

	@Setter @Getter private int steps = 16;
	@Setter @Getter private int subdivision = 4;

    @Getter @Setter private boolean active;
    @Getter private float tempo = 80f;
    @Setter @Getter private int measure = 4;
	@Getter private int count = -1;

	private Gui gui;
    private JLabel tempoLbl;
    private JToggleButton start;

	/** current step */
	@Setter @Getter private int step = 0;
	/** jack frames since transport start or frames between pulses */
	@Getter private long lastPulse;
	private long nextPulse;

	private JudahClock() {
	    instance = this;
	    for (int i = 0; i < 16; i++)
	        sequencers.add(new BeatBox(i));
	}

	public void process() {
	    if (!active) return;
	    if (System.currentTimeMillis() < nextPulse) return;
	    lastPulse = nextPulse;
	    nextPulse = computeNextPulse();
	    step();
	}

	public long computeNextPulse() {
	    return lastPulse + Constants.millisPerBeat(tempo * subdivision);
	}

	private void step() {
        // run the current beat
        if (step % subdivision == 0) {
            count++;
            // Console.info("step: " + step + " beat: " + step/2f + " count: " + count);
            listeners.forEach(listener -> {listener.update(Property.BEAT, count);});
        }

        new Thread( () -> {
            listeners.forEach(listener -> {listener.update(Property.STEP, step);});
            for (BeatBox beatbox : sequencers)
                beatbox.process(step);
            step++;
            if (step == steps)
                step = 0;

        }).start();
	}

	@Override
    public void begin() {
	    lastPulse = System.currentTimeMillis();
	    if (Sequencer.getCurrent() != null)
	        Sequencer.getCurrent().setClock(this);
	    step();
	    nextPulse = computeNextPulse();
        start.setText("Stop");
	    active = true;
	}

	@Override
    public void end() {
	    active = false;
        start.setText("Start");
	    Console.info(JudahClock.class.getSimpleName() + " end");
	}

	@Override
	public boolean setTempo(float tempo2) {
		if (tempo2 < tempo || tempo2 > tempo) {
			tempo = tempo2;
			tempoLbl.setText("" + tempo);
			listeners.forEach(l -> {l.update(Property.TEMPO, tempo);});
		}
		return true;
	}

	@Override
	public void addListener(TimeListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}

	@Override
    public void update(Property prop, Object value) {
	    if (Property.TEMPO == prop) {
	        setTempo((float)value);
	    }
	}

    public JPanel getGui() {
        if (gui == null) gui = new Gui();
        return gui;
    }

    public BeatBox getSequencer(int channel) {
        return sequencers.get(channel);
    }

    class Gui extends JPanel {
        final Dimension combo = new Dimension(MainFrame.WIDTH_CLOCK / 2, 19);
        final JudahMenu popup = new JudahMenu();
        Gui() {
            Slider tempo = new Slider(35, 175, e -> {
                setTempo(((Slider)e.getSource()).getValue());});
            tempo.setValue(Math.round(getTempo()));
            JPanel tempoPnl = new JPanel();
            tempoPnl.setLayout(new BoxLayout(tempoPnl, BoxLayout.X_AXIS));
            tempoPnl.add(Box.createRigidArea(new Dimension(6, 6)));
            tempoPnl.add(tempo);
            tempoPnl.add(Box.createRigidArea(new Dimension(6, 6)));


            TapTempo tapButton = new TapTempo("Tempo: ", msec -> {
                setTempo(60000 / msec);
            });
            tempoLbl = new JLabel("" + getTempo(), JLabel.CENTER);
            tempoLbl.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    String input = Constants.inputBox("Tempo:");
                    if (input == null || input.isEmpty()) return;
                    try { setTempo(Float.parseFloat(input));
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
            start.setSelected(isActive());
            start.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isActive()) {
                        end();
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            step = 0;
                            count = -1;
                        }
                    }
                    else
                        begin();
                }
            });
            JPanel startPnl = new JPanel(new GridLayout(1, 2));
            startPnl.add(menu); startPnl.add(start);

            // setLayout(new GridLayout(5, 1, 3, 0));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(startPnl);

            JPanel logPnl = new JPanel(new GridLayout(0, 3, 0, 2));
            JToggleButton log = new JToggleButton("log");
            log.setFont(Constants.Gui.FONT9);
            JToggleButton seq = new JToggleButton("seq");
            seq.setFont(Constants.Gui.FONT9);
            JToggleButton fx = new JToggleButton("fx");
            fx.setFont(Constants.Gui.FONT9);

            logPnl.add(seq); seq.setVisible(false); // TODO
            logPnl.add(fx);   fx.setVisible(false); // not implemented yet
            logPnl.add(log); log.setVisible(false);

            add(logPnl);

            add(bpmPnl);
            add(tempoPnl);
            add(Box.createVerticalGlue());
        }
    }

	public void latch(Recorder loopA, int beats) {
		if (loopA.hasRecording() && loopA.getRecordedLength() > 0) {
			float milliPerBeat = loopA.getRecordedLength() / (float)beats;  
			float tempo = Constants.bpmPerBeat(milliPerBeat);
			setTempo(tempo); 
			listen(loopA);
			Console.info("JudahClock armed at " + tempo + " bpm.");
		}
	}

	void listen(Recorder target) {
		if (listener != null) return;
		listener = new TimeListener() {
			@Override public void update(Property prop, Object value) {
				if (prop.equals(TimeListener.Property.LOOP)) {
					begin();
					target.removeListener(this);
					listener = null;
				}
			}
		};
		target.addListener(listener);
	}
	
}


