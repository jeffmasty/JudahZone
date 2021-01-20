package net.judah.effects.gui;

import javax.swing.JToggleButton;

import net.judah.effects.Chorus;
import net.judah.mixer.Channel;

public class EffectsButton extends JToggleButton {
    private Class<?> effect;
    public EffectsButton(String lbl, Class<?> effectClazz) {
        super(lbl);
        this.effect = effectClazz;
        addActionListener(listener -> {
            Boolean result = null;
            Channel ch = EffectsRack.getInstance().getFocus();
            if (effect.equals(Chorus.class)) {
                ch.getChorus().setActive(!ch.getChorus().isActive());
                result = ch.getChorus().isActive();
            }
            update();
            // Console.info(effect.getSimpleName() + (result ? " On : " : " Off : ") + ch.getName());
        });

    }

    public void update() {
        if (effect.equals(Chorus.class))
            setSelected(EffectsRack.getInstance().getFocus().getChorus().isActive());
    }
}

