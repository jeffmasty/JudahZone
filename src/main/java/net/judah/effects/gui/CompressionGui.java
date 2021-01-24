package net.judah.effects.gui;

import javax.swing.JToggleButton;

import net.judah.effects.Compression;
import net.judah.mixer.Channel;

public class CompressionGui extends Widget {

    private Slider compAtt, compRel, compThresh;

    public CompressionGui(Channel ch) {
        super(ch, new JToggleButton("Comp."), e -> {
            ch.getCompression().setActive(!ch.getCompression().isActive());});
        compThresh = new Slider(l -> {
                channel.getCompression().setThreshold((compThresh.getValue() - 100) / 2.5f);});
        compThresh.setPreferredSize(MINI); compThresh.setMaximumSize(MINI);
        compThresh.setToolTipText("-40 to 0");
        compAtt = new Slider(l -> {
                channel.getCompression().setAttack(Math.round(compAtt.getValue() * 1.5f));});
        compAtt.setPreferredSize(MINI); compAtt.setMaximumSize(MINI);
        compAtt.setToolTipText("0 to 150 milliseconds");
        compRel = new Slider(l -> {channel.getCompression().setRelease(Math.round(compRel.getValue() * 3));});
        compRel.setPreferredSize(MINI); compRel.setMaximumSize(MINI);
        compRel.setToolTipText("0 to 300 milliseconds");

        add(compThresh);
        add(new Label("t/hold"));
        add(compAtt);
        add(new Label("attk."));
        add(compRel);
        add(new Label("rel."));

    }

    @Override
    void update() {
        Compression compression = channel.getCompression();
        activeButton.setSelected(compression.isActive());
        compThresh.setValue((int) ((compression.getThreshold() + 40) * 2.5));
        int attack = Math.round(compression.getAttack() * 0.75f);
        if (attack > 100) attack = 100;
        compAtt.setValue(attack);
        int release = Math.round(compression.getRelease() * 0.333f);
        if (release > 100) release = 100;
        compRel.setValue(release);
    }

}
