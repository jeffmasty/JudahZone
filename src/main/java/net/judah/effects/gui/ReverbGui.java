package net.judah.effects.gui;

import javax.swing.JToggleButton;

import net.judah.effects.api.Reverb;
import net.judah.mixer.Channel;

public class ReverbGui extends Widget {

    private Slider revRoom, revDamp, revWet;

    public ReverbGui(Channel ch) {
        super(ch, new JToggleButton("Reverb"), e -> {
            ch.getReverb().setActive(!ch.getReverb().isActive());});
        revRoom = new Slider(l -> {channel.getReverb().setRoomSize(revRoom.getValue() / 100f);});
        revRoom.setPreferredSize(MINI); revRoom.setMaximumSize(MINI);
        revDamp = new Slider(l -> {channel.getReverb().setDamp(revDamp.getValue() / 100f);});
        revDamp.setPreferredSize(MINI); revDamp.setMaximumSize(MINI);
        revWet = new Slider(l -> {channel.getReverb().setWet(revWet.getValue() / 100f);});
        revWet.setPreferredSize(MINI); revWet.setMaximumSize(MINI);

        add(revRoom);
        add(new Label("room"));
        add(revDamp);
        add(new Label("damp"));
        add(revWet);
        add(new Label("wet"));
    }

    @Override
    void update() {
        Reverb reverb = channel.getReverb();
        activeButton.setSelected(reverb.isActive());
        revRoom.setValue(Math.round(reverb.getRoomSize() * 100f));
        revDamp.setValue(Math.round(reverb.getDamp() * 100f));
        revWet.setValue(Math.round(reverb.getWet() * 100f));
    }
}
