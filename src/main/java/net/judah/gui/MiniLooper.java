package net.judah.gui;

import static net.judah.api.Notification.Property.TEMPO;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.widgets.LoopWidget;
import net.judah.gui.widgets.Slider;
import net.judah.gui.widgets.TapTempo;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

public class MiniLooper extends JPanel implements TimeListener {
	private static final int WIDTH = 82;
	
	private final JudahClock clock;
	@Getter private final LoopWidget loopWidget;
   	private final JLabel tempoLbl = new JLabel("?", JLabel.CENTER);
	private final TapTempo tapButton = new TapTempo("Tempo", msec -> {
            JudahZone.getClock().writeTempo(Math.round(60000 / msec));});	
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
		clock.addListener(this);
		Dimension size = new Dimension(WIDTH, Size.STD_HEIGHT + 6);
		loopWidget = new LoopWidget(loops, size);
		tempoKnob = new Slider(55, 155, null);

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
		tempoLbl.setText("####");
		tempoLbl.setFont(Gui.BOLD);
        Gui.resize(tempoKnob, size);
        setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  
        add(loopWidget);
        JPanel btm = new JPanel();
        btm.add(tapButton);
        btm.add(tempoLbl);
        btm.add(tempoKnob);
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
