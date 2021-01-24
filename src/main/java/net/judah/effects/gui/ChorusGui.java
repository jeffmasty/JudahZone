package net.judah.effects.gui;

import javax.swing.JToggleButton;

import net.judah.effects.Chorus;
import net.judah.mixer.Channel;

public class ChorusGui extends Widget {

    private Slider choDepth, choRate, choFeedback;

    public ChorusGui(Channel ch) {

        super(ch, new JToggleButton("Chorus"), e -> {
            ch.getChorus().setActive(!ch.getChorus().isActive());});

        choRate = new Slider(e -> {channel.getChorus().setRate(choRate.getValue()/10f);});
        choRate.setPreferredSize(MINI); choRate.setMaximumSize(MINI);
        choFeedback = new Slider(e -> {channel.getChorus().setFeedback(choFeedback.getValue()/100f);});
        choFeedback.setPreferredSize(MINI); choFeedback.setMaximumSize(MINI);
        choDepth = new Slider(e -> {channel.getChorus().setDepth(choDepth.getValue()/100f);});
        choDepth.setPreferredSize(MINI); choDepth.setMaximumSize(MINI);

        add(choRate);
        add(new Label("rate"));
        add(choDepth);
        add(new Label("depth"));
        add(choFeedback);
        add(new Label("f/b"));
    }

    @Override
    void update() {
        Chorus chorus = channel.getChorus();
        activeButton.setSelected(chorus.isActive());
        choDepth.setValue(Math.round(chorus.getDepth() * 100));
        choRate.setValue(Math.round(chorus.getRate() * 10));
        choFeedback.setValue(Math.round(chorus.getFeedback() * 100));
    }

}

