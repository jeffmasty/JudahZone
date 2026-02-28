package net.judah.gui.drums;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import judahzone.data.Letter;

/** Lightweight two-step envelope display with optional cached rendering.
 *  Set attack/decay (0..1 knob fraction) and call repaint(); the image is regenerated
 *  only when parameters or size change. */
public class TwoStepGraph extends JComponent {

    private static final int MARGIN = 8;
    private static final Stroke ENV_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final float MAX_ATTACK_FRACTION = Math.min(0.25f,
            Letter.MAX_ATTACK_MS / (float) (Letter.MAX_ATTACK_MS + Letter.MAX_DECAY_MS));

    // stored knob fractions [0..1]
    private volatile float attack;
    private volatile float decay;

    // cached raster
    private BufferedImage cache;
    private volatile boolean dirty = true;

    public TwoStepGraph() {
        // Gui.resi
        setPreferredSize(new Dimension(200, 64));
        // rebuild cache on resize
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { dirty = true; }
        });
    }

    public void setAttackKnob(int knob) { // 0..100
        setAttackFraction(knob / 100f);
    }
    public void setDecayKnob(int knob) { // 0..100
        setDecayFraction(knob / 100f);
    }

    /** attack knob fraction (0..1) where envelope reaches peak; will be mapped to a small display span */
    public void setAttackFraction(float a) {
        float v = Math.max(0f, Math.min(1f, a));
        if (v == attack)
            return;
        attack = v;
        dirty();
    }

    /** decay knob fraction (0..1) relative to width where envelope reaches final level (usually 0) */
    public void setDecayFraction(float d) {
        float v = Math.max(0f, Math.min(1f, d));
        if (v == decay)
            return;
        decay = v;
        dirty();
    }

    private void dirty() {
        dirty = true;
        repaint();
    }

    public float getAttackFraction() { return attack; }
    public float getDecayFraction() { return decay; }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
            cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            dirty = true;
        }

        if (dirty) rebuildCache(cache);
        g.drawImage(cache, 0, 0, null);
    }

    // Rebuilds the cached image. Keep allocations out of inner loops.
    private void rebuildCache(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        try {
            // clear
            g.setComposite(AlphaComposite.Src);
            g.setColor(getBackground() != null ? getBackground() : Color.BLACK);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());

            // quality
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = img.getWidth(), h = img.getHeight();

            // draw subtle grid / baseline
            g.setColor(new Color(255, 255, 255, 20));
            for (int i = 0; i <= 4; i++) {
                int y = MARGIN + i * (h - 2 * MARGIN) / 4;
                g.drawLine(MARGIN, y, w - MARGIN, y);
            }

            // envelope coordinates
            int left = MARGIN;
            int right = w - MARGIN;
            int top = MARGIN;
            int bottom = h - MARGIN;
            int drawableWidth = Math.max(1, right - left);

            int minGap = Math.max(8, Math.round(drawableWidth * 0.08f));
            float gapFrac = minGap / (float) drawableWidth;

            // knob fractions (0..1)
            float knobAttack = Math.max(0f, Math.min(1f, attack));
            float knobDecay  = Math.max(0f, Math.min(1f, decay));

            // Map full knob range into the limited display span for attack so 0..100 uses full knob but only
            // occupies up to MAX_ATTACK_FRACTION of the drawable width.
            float normalizedAttack = knobAttack * MAX_ATTACK_FRACTION;
            // decay uses full drawable range
            float normalizedDecay = knobDecay;

            // Respect gap and bounds: ensure at least minGap between attack and decay,
            // and attack does not exceed drawable area minus gap.
            normalizedAttack = Math.min(normalizedAttack, 1f - gapFrac);
            normalizedDecay = Math.max(normalizedDecay, normalizedAttack + gapFrac);

            // If decay was forced past 1, clamp and pull attack back if needed.
            if (normalizedDecay > 1f) {
                normalizedDecay = 1f;
                normalizedAttack = Math.max(0f, normalizedDecay - gapFrac);
            }

            int attackX = left + Math.round(normalizedAttack * drawableWidth);
            int decayX  = left + Math.round(normalizedDecay * drawableWidth);

            int peakY = top;
            int finalY = bottom;

            Polygon poly = new Polygon();
            poly.addPoint(left, bottom);
            poly.addPoint(left, finalY);
            poly.addPoint(attackX, peakY);
            poly.addPoint(decayX, finalY);
            poly.addPoint(right, bottom);
            g.setColor(new Color(100, 180, 255, 60));
            g.fillPolygon(poly);

            g.setStroke(ENV_STROKE);
            g.setColor(new Color(200, 240, 255));
            g.drawLine(left, finalY, attackX, peakY);
            drawDecayCurve(g, attackX, peakY, decayX, finalY);

            g.setColor(new Color(220, 220, 220, 180));
            g.fillOval(attackX - 3, peakY - 3, 7, 7);
            g.fillOval(decayX - 3, finalY - 3, 7, 7);

            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            g.setColor(new Color(200, 200, 200, 200));
            g.drawString("A", Math.max(left, attackX - 10), Math.max(peakY - 4, top));
            g.drawString("D", Math.max(left, decayX - 8), Math.min(finalY + 12, h - 2));

        } finally {
            g.dispose();
            dirty = false;
        }
    }

    // draw a light exponential-ish decay between two points with a few segments
    private void drawDecayCurve(Graphics2D g, int x0, int y0, int x1, int y1) {
        int segs = Math.max(4, (x1 - x0) / 8);
        int px = x0, py = y0;
        for (int i = 1; i <= segs; i++) {
            float t = i / (float) segs;
            // exponential feel: y moves quickly then eases
            float et = (float) (1.0 - Math.pow(1.0 - t, 2.2));
            int nx = x0 + Math.round(t * (x1 - x0));
            int ny = y0 + Math.round(et * (y1 - y0));
            g.drawLine(px, py, nx, ny);
            px = nx; py = ny;
        }
    }
}
