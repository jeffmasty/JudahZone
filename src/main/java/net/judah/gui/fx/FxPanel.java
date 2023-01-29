package net.judah.gui.fx;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Pastels;
import net.judah.mixer.Channel;

public class FxPanel extends JPanel {

    @Getter private final MultiSelect selected = new MultiSelect();
    private JPanel placeholder = new JPanel(new GridLayout(1, 1, 0, 0));
    
    public FxPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        placeholder.setBorder(new LineBorder(Pastels.MY_GRAY, 3));
        add(placeholder);
        doLayout();
        Channel main = JudahZone.getMains();
        main.getGui();
        JudahZone.getGuitar().getGui();
        setFocus(main);
    }

    public void addFocus(Channel ch) {
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


}
