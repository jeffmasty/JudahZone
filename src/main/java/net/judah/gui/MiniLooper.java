package net.judah.gui;

import static net.judah.api.Notification.Property.TEMPO;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.widgets.LoopWidget;
import net.judah.gui.widgets.Slider;
import net.judah.gui.widgets.StartBtn;
import net.judah.gui.widgets.TapTempo;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

public class MiniLooper extends JPanel implements TimeListener {
	
	private final JudahClock clock;
	private final StartBtn start;

	private final Looper looper;
	private final LoopWidget loopWidget;
    private final JButton record;
   	private final JLabel tempoLbl = new JLabel("?", JLabel.CENTER);
	private final TapTempo tapButton = new TapTempo("Tempo", msec -> {
            JudahZone.getClock().writeTempo(Math.round(60000 / msec));});	
	private Loop recording;
	private final Slider tempoKnob;
	private final ChangeListener tempoEar = new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
        	int tempo = tempoKnob.getValue();
        	if (clock.getTempo() < tempo - 1 || clock.getTempo() > tempo + 1)
        		clock.writeTempo(tempo);
			}
		};
	
	public MiniLooper(Looper loops, JudahClock time) {
		this.clock = time;
		this.looper = loops;
		clock.addListener(this);
		loopWidget = new LoopWidget(looper);
		tempoLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                String input = Gui.inputBox("Tempo:");
                if (input == null || input.isEmpty()) return;
                try { 
                	clock.writeTempo((int)Float.parseFloat(input));
                } catch (Throwable t) { 
                	RTLogger.log(this, t.getMessage() + " -> " + input); 
                }
            }});
        tempoLbl.setFont(Gui.BOLD);
        tempoKnob = new Slider(55, 155, null);
        
        JButton delete = new JButton("Del");
        delete.addActionListener(e->looper.reset());
        record = new JButton("Record " + clock.getLength());
        record.addActionListener(e->record());

        Gui.resize(tempoKnob, Size.MEDIUM_COMBO);
        setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        start = new StartBtn(clock);
  
        add(Gui.wrap(start, record, delete));
        add(loopWidget);
        add(Gui.wrap(tapButton, tempoLbl, tempoKnob));
        tempoKnob.addChangeListener(tempoEar);
        
	}

	public void record() {
		if (recording == null) {
			if (clock.isActive()) {
				recording = looper.getLoopA();
				recording.trigger();
			} else {
				recording = looper.getLoopC();
				recording.record(true);
			}
		}
		else {
			recording.record(false);
			recording = null;
		}
		update();
	}
	
	public void update() {
		record.setBackground(recording == null ? null : recording.isRecording() == AudioMode.ARMED ? Pastels.PINK : 
			recording.isRecording() == AudioMode.RUNNING ? Pastels.RED : null);
		record.setText("Record " + clock.getLength());	// sync.setSelectedItem(clock.getLength());
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == TEMPO) {
			tempoLbl.setText("" + value.toString().substring(0, 3));
			tempoKnob.removeChangeListener(tempoEar);
			tempoKnob.setValue(Math.round((float)value));
			tempoKnob.addChangeListener(tempoEar);
			return;
		}

		record.setBackground(recording == null ? null : recording.isRecording() == AudioMode.ARMED ? Pastels.PINK : 
			recording.isRecording() == AudioMode.RUNNING ? Pastels.RED : null);
	}
	
}
