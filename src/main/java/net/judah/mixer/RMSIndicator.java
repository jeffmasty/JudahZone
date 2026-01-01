package net.judah.mixer;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import judahzone.gui.Gui;
import judahzone.gui.Updateable;
import judahzone.util.Rainbow;
import net.judah.channel.Channel;
import net.judah.channel.Mains;
import net.judah.gui.Size;
import net.judah.looper.Loop;

/** listen to channel's audio, paint widget height/color based on RMS of the audio frame */
public class RMSIndicator extends JPanel implements Updateable {

    // Boost RMS into color/pixels range, todo see AudioTools.linearToDecibal
    private static final float FACTOR = 23;
    private static final float MAINS = 5f;
    private static final float LOOP = 14;

    private static final int WIDTH = 12;
    private static final int HALF = WIDTH / 2;
    private static final int HEIGHT = Size.FADER_SIZE.height;

    private final Channel ch;
    private float left, right; // computed gain

    public RMSIndicator(Channel ch) {
        this.ch = ch;
        Gui.resize(this, new Dimension(WIDTH, HEIGHT));
        setBorder(Gui.SUBTLE);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int h = (int)(HEIGHT * left);
        g.setColor(Rainbow.get(left));
        g.fillRect(1, HEIGHT - h, HALF, h);
        h = (int)(HEIGHT * right);
        g.setColor(Rainbow.get(right));
        g.fillRect(HALF, HEIGHT - h, WIDTH - 1, h);
    }

    @Override
    public void update() {
        float factor = ch instanceof Loop ? LOOP : ch instanceof Mains ? MAINS : FACTOR;
        left = ch.getLastRmsLeft() * factor;
        right = ch.getLastRmsRight() * factor;
        repaint();
    }

}