package net.judah.gui;

import static net.judah.api.Notification.Property.*;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.util.Constants;
import net.judah.util.RTLogger;
import net.judah.widgets.LengthWidget;
import net.judah.widgets.LoopWidget;
import net.judah.widgets.Slider;
import net.judah.widgets.TapTempo;

public class MiniLooper extends JPanel implements TimeListener {
	public static final Dimension SLIDER = new Dimension(85, Size.STD_HEIGHT);

	private final JudahClock clock;
	private final Looper looper;
	private final LoopWidget loopWidget;
    private final JButton record = new JButton("Rec");
	private final LengthWidget sync;
    private final JLabel loopLbl = new JLabel("0.0s");
   	private final JLabel tempoLbl = new JLabel("?", JLabel.CENTER);
	private final TapTempo tapButton = new TapTempo("Tempo", msec -> {
            JudahZone.getClock().setTempo(60000 / msec);});	
	private Loop recording;
	Slider tempoKnob = new Slider(60, 160, null);
	
	public MiniLooper(Looper loops, JudahClock clock) {
		this.clock = clock;
		this.looper = loops;
		looper.getLoopA().addListener(this);
		clock.addListener(this);
		loopWidget = new LoopWidget(looper.getLoopA());
		tempoLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                String input = Constants.inputBox("Tempo:");
                if (input == null || input.isEmpty()) return;
                try { 
                	clock.writeTempo((int)Float.parseFloat(input));
                } catch (Throwable t) { 
                	RTLogger.log(this, t.getMessage() + " -> " + input); 
                }
            }});
        tempoLbl.setFont(Constants.Gui.BOLD);
        Constants.resize(tempoKnob, SLIDER);
        Constants.resize(loopWidget, SLIDER);
        tempoKnob.setValue((int)clock.getTempo());

        tempoKnob.addChangeListener(value -> {
        	int tempo = tempoKnob.getValue();
        	if (clock.getTempo() < tempo - 0.5f || clock.getTempo() > tempo + 0.5f)
        		clock.writeTempo(tempo);});
        JButton delete = new JButton("Del");
        delete.addActionListener(e->looper.reset());
        record.addActionListener(e->record());
        sync = new LengthWidget(clock);
        setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(Constants.wrap(record, sync, new JLabel("Bars"), delete));
        add(Constants.wrap(tapButton, tempoLbl, tempoKnob));
        add(Constants.wrap(new JLabel("Loop"), loopLbl, loopWidget));

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
			recording.addListener(this);
		}
		else {
			recording.record(false);
			recording = null;
		}
		update();
	}
	
	public void update() {
		sync.setSelectedItem(clock.getLength());
		record.setBackground(recording == null ? null : recording.isRecording() == AudioMode.ARMED ? Pastels.PINK : 
			recording.isRecording() == AudioMode.RUNNING ? Pastels.RED : null);
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == TEMPO) {
			tempoLbl.setText("" + value.toString().substring(0, 3));
			tempoKnob.setValue(Math.round((float)value));
		}
		else if (prop == LOOP || (prop == STATUS && value == Status.TERMINATED)) {
			String time = Float.toString(JudahZone.getLooper().getRecordedLength() / 1000f);
			if (time.length() > 2)
				time.substring(0, 2);
			loopLbl.setText(time + "s");
			record.setBackground(recording == null ? null : recording.isRecording() == AudioMode.ARMED ? Pastels.PINK : 
				recording.isRecording() == AudioMode.RUNNING ? Pastels.RED : null);
		}
	}
	
}
