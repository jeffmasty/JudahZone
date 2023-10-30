package net.judah.gui.fx;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import lombok.Getter;
import net.judah.fx.Chorus;
import net.judah.fx.Delay;
import net.judah.fx.LFO;
import net.judah.fx.TimeEffect;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.mixer.Channel;

public class FxPanel extends JPanel {

    @Getter private final MultiSelect selected = new MultiSelect();
    private JPanel placeholder = new JPanel(new GridLayout(1, 1, 0, 0));
    
    public FxPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        placeholder.setBorder(new LineBorder(Pastels.MY_GRAY, 2));
        add(placeholder);
        doLayout();
    }

    public void addFocus(Channel ch) {
    	if (selected.contains(ch))
    		return;
    	selected.add(ch);
    	getChannel().getGui().getTitle().name(selected); 
    }
    
    public void setFocus(Channel ch) {
    	if (getChannel() == ch && selected.size() == 1) {
    		return;
    	}
    	selected.clear();
    	selected.add(ch);
    	ch.getGui().getTitle().name(selected);
        placeholder.removeAll();
        placeholder.add(ch.getGui());
        ch.getGui().update();
        validate();
    }
    
    public Channel getChannel() {
    	if (selected.isEmpty()) 
    		return null;
        return selected.get(0);
    }
    
    public void timeFx(int subdiv, Class<? extends TimeEffect> type) {
    	if (type == Delay.class) 
    		selected.forEach(ch->ch.getDelay().sync());
    	else if (type == LFO.class) 
    		selected.forEach(ch->ch.getLfo().sync());
    	else if (type == Chorus.class)
    		selected.forEach(ch->ch.getChorus().sync());
    	MainFrame.update(getChannel());
    }




}
