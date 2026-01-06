package net.judah.gui.fx;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import judahzone.api.TimeFX;
import judahzone.fx.Chorus;
import judahzone.fx.Delay;
import judahzone.gui.Pastels;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.channel.Mains;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.midi.JudahClock;
import net.judah.midi.LFO;

public class FxPanel extends JPanel {

    @Getter private final MultiSelect selected;
    private final JudahClock clock;
    private final Mains mains;
    private JPanel placeholder = new JPanel(new GridLayout(1, 1, 0, 0));

    public FxPanel(MultiSelect multi, JudahClock clock, Mains mains) {
    	selected = multi;
    	this.mains = mains;
    	this.clock = clock;
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

    	checkMainsRMS();
    }

    public void setFocus(Channel ch) {
    	selected.clear();
    	selected.add(ch);
    	ch.getGui().getTitle().name(selected);
        placeholder.removeAll();
        placeholder.add(ch.getGui());
        ch.getGui().update();
        validate();

        checkMainsRMS();
    }

    // Get Mains up on RMS meters if needed.
    private void checkMainsRMS() {
    	if (MainFrame.getKnobMode() == KnobMode.Tuner)
    		mains.setCopy(selected.contains(mains) && selected.size() == 1);
    	else
			mains.setCopy(false);
	}

    public Channel getChannel() {
    	if (selected.isEmpty())
    		return null;
        return selected.get(0);
    }

    public void timeFx(int subdiv, Class<? extends TimeFX> type) {
    	float syncUnit = clock.syncUnit();
    	if (type == Delay.class)
    		selected.forEach(ch->ch.getDelay().sync(syncUnit));
    	else if (type == Chorus.class)
    		selected.forEach(ch->ch.getChorus().sync(syncUnit));
    	else if (type == LFO.class)
    		selected.forEach(ch->ch.getLfo().sync(syncUnit));
    	MainFrame.update(getChannel());
    }


}
