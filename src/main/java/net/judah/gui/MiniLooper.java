package net.judah.gui;

import static net.judah.api.Notification.Property.TEMPO;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.widgets.Slider;
import net.judah.gui.widgets.TapTempo;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;

public class MiniLooper extends JPanel implements TimeListener {
	private static final Dimension TEMPO_SLIDER = new Dimension(75, Size.STD_HEIGHT + 4);
	private static final Dimension LOOP_SLIDER = new Dimension(TEMPO_SLIDER.width * 2, TEMPO_SLIDER.height);

	private final JudahClock clock;
   	private final JLabel tempoLbl = new JLabel("####", JLabel.CENTER);
	private final Slider tempoKnob = new Slider(55, 155, null);
	private final ChangeListener tempoEar = new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
        	int tempo = tempoKnob.getValue();
        	if (clock.getTempo() < tempo - 1 || clock.getTempo() > tempo + 1)
        		clock.setTempo(tempo);
			}
		};

	public MiniLooper(Looper loops, JudahClock time) {
		this.clock = time;
		clock.addListener(this);
		tempoLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
            	clock.inputTempo();
            }});
		tempoLbl.setFont(Gui.BOLD);

        setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        setLayout(new GridLayout(2, 1));
        Gui.resize(loops.getLoopWidget().getSlider(), TEMPO_SLIDER);
        add(Gui.resize(loops.getLoopWidget(), LOOP_SLIDER));
        JPanel btm = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
    	TapTempo tapButton = new TapTempo("Tempo", msec -> {
            clock.setTempo(Math.round(60000 / msec));});
        btm.add(tapButton);
        btm.add(tempoLbl);
        btm.add(Gui.resize(tempoKnob, TEMPO_SLIDER));
        add(btm);
        tempoKnob.addChangeListener(tempoEar);

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
	}


}
