package net.judah.effects.gui;

import javax.swing.JToggleButton;

import net.judah.effects.EQ.EqBand;
import net.judah.effects.EQ.EqParam;
import net.judah.mixer.Channel;

public class EQGui extends Widget {

    private Slider eqBass, eqMid, eqTreble;

    public EQGui(Channel ch) {
        super(ch, new JToggleButton("   EQ   "), e -> {
            ch.getEq().setActive(!ch.getEq().isActive());});

        eqBass = new Slider(e -> {eqGain(EqBand.BASS, eqBass.getValue());});
        eqBass.setPreferredSize(MINI);
        eqBass.setMaximumSize(MINI);
        eqBass.setMinimumSize(MINI);
        eqMid = new Slider(e -> {eqGain(EqBand.MID, eqMid.getValue());});
        eqMid.setPreferredSize(MINI);
        eqMid.setMinimumSize(MINI);
        eqMid.setMaximumSize(MINI);
        eqTreble = new Slider(e -> {eqGain(EqBand.TREBLE, eqTreble.getValue());});
        eqTreble.setPreferredSize(MINI);
        eqTreble.setMaximumSize(MINI);
        eqTreble.setMinimumSize(MINI);

        add(eqBass);
        add(new Label("bass"));
        add(eqMid);
        add(new Label("mid"));
        add(eqTreble);
        add(new Label("high"));

    }

    private void eqGain(EqBand eqBand, int val) {
        boolean negative = val < 50;
        float result = Math.abs(50 - val) / 2;
        if (negative) result *= -1;
        channel.getEq().update(eqBand, EqParam.GAIN, result);
    }
    @Override
    void update() {
        activeButton.setSelected(channel.getEq().isActive());
        eqBass.setValue(Math.round( (channel.getEq().getGain(EqBand.BASS) * 2 + 50)));
        eqMid.setValue(Math.round( (channel.getEq().getGain(EqBand.MID) * 2 + 50)));
        eqTreble.setValue(Math.round( (channel.getEq().getGain(EqBand.TREBLE) * 2 + 50)));
    }

}
