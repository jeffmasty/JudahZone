package net.judah.gui.knobs.synth;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

/** Four-step envelope display with optional cached rendering.
 *  Set attack/decay/release/sustain (0..1 knob fraction) and call repaint();
 *  the image is regenerated only when parameters or size change. */
public class FourStepGraph extends JComponent {

	private static final float atkFactor = 0.2f;
	private static final float dkFactor = 0.25f;
	private static final float susFactor = 0.2f;
	private static final float rlsFactor = 0.35f;

/*
We are upgrading a 2-step to a 4-step.  the 3rd step is different, it is height of the stage between decay and release,
Since sus doesn't affect width, the width will always be fixed at 20% of the available width for all segments.

Here is what we are doing now: We are modifying the current behavior.

Desired:
if attack is zero, everything shifts left. as attack grows larger. the attack segment upward to the right, pushing the other segments right.
until attack reaches maximum (100%) which will be 20% of total width.

A max decay would take up to 25% of the width.

but a decay of 50% will shift sus and release segments to the left by 12.5% of decay's max width. empty space to the right of release.

empty space to the right of release will be combined less-than-max of atk, dk and rls.

sus always 20%.  it will look weird when sus goes to zero or 100% but that's just how it is.

release can take up to 35% of the width, but it is shifted left by less-than-max atk and decay.

*/

    private static final int MARGIN = 8;
    private static final Stroke ENV_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    // stored knob fractions [0..1]
    private volatile float attack;
    private volatile float decay;
    private volatile float sustain; // vertical level fraction (0..1) where 1==peak
    private volatile float release; // end-of-release position fraction across width (0..1)

    // cached raster
    private BufferedImage cache;
    private volatile boolean dirty = true;

    public FourStepGraph() {
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
    public void setSustainKnob(int knob) { // 0..100
        setSustainFraction(knob / 100f);
    }
    public void setReleaseKnob(int knob) { // 0..100
        setReleaseFraction(knob / 100f);
    }

    /** attack knob fraction (0..1) where envelope reaches peak; will be mapped to a small display span */
    public void setAttackFraction(float a) {
        float v = Math.max(0f, Math.min(1f, a));
        if (v == attack)
            return;
        attack = v;
        dirty();
    }

    /** decay knob fraction (0..1) relative to width where envelope reaches sustain level */
    public void setDecayFraction(float d) {
        float v = Math.max(0f, Math.min(1f, d));
        if (v == decay)
            return;
        decay = v;
        dirty();
    }

    /** sustain level fraction (0..1) where 1==peak, 0==final level */
    public void setSustainFraction(float s) {
        float v = Math.max(0f, Math.min(1f, s));
        if (v == sustain)
            return;
        sustain = v;
        dirty();
    }

    /** release knob fraction (0..1) relative to width where the release stage ends (right side is 1) */
    public void setReleaseFraction(float r) {
        float v = Math.max(0f, Math.min(1f, r));
        if (v == release)
            return;
        release = v;
        dirty();
    }

    private void dirty() {
        dirty = true;
        repaint();
    }

    public float getAttackFraction() { return attack; }
    public float getDecayFraction() { return decay; }
    public float getSustainFraction() { return sustain; }
    public float getReleaseFraction() { return release; }

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

            // knob fractions (0..1), clamped to valid range
            float knobAttack = Math.max(0f, Math.min(1f, attack));
            float knobDecay  = Math.max(0f, Math.min(1f, decay));
            float knobSustain = Math.max(0f, Math.min(1f, sustain));
            float knobRelease = Math.max(0f, Math.min(1f, release));

            // Width allocation per spec:
            //   attack: 0..20% of drawable width (0 to atkFactor)
            //   decay:  0..25% of drawable width (0 to dkFactor)
            //   sustain: fixed 20% of drawable width (susFactor)
            //   release: 0..35% of drawable width (0 to rlsFactor)

            // Calculate actual widths based on knob fractions
            int atkWidth = Math.round(knobAttack * atkFactor * drawableWidth);
            int dkWidth  = Math.round(knobDecay * dkFactor * drawableWidth);
            int susWidth = Math.round(susFactor * drawableWidth);
            int rlsWidth = Math.round(knobRelease * rlsFactor * drawableWidth);

            // Position each segment left to right
            int attackX  = left + atkWidth;
            int decayX   = attackX + dkWidth;
            int sustainX = decayX + susWidth;
            int releaseX = sustainX + rlsWidth;

            // Clamp release end to stay within drawable area
            if (releaseX > right) {
                releaseX = right;
            }

            int peakY = top;
            int finalY = bottom;
            int sustainY = top + Math.round((1f - knobSustain) * (bottom - top));

            // Build polygon: left baseline -> attack peak -> decay to sustain -> sustain hold -> release to baseline -> right
            Polygon poly = new Polygon();
            poly.addPoint(left, bottom);
            poly.addPoint(left, finalY);
            poly.addPoint(attackX, peakY);
            poly.addPoint(decayX, sustainY);
            poly.addPoint(sustainX, sustainY);
            poly.addPoint(releaseX, finalY);
            poly.addPoint(right, finalY);
            poly.addPoint(right, bottom);

            g.setColor(new Color(100, 180, 255, 60));
            g.fillPolygon(poly);

            g.setStroke(ENV_STROKE);
            g.setColor(new Color(200, 240, 255));
            // attack ramp
            g.drawLine(left, finalY, attackX, peakY);
            // decay curve from peak to sustain point
            drawDecayCurve(g, attackX, peakY, decayX, sustainY);
            // sustain horizontal
            g.drawLine(decayX, sustainY, sustainX, sustainY);
            // release curve from sustain to finalY at right edge
            drawDecayCurve(g, sustainX, sustainY, releaseX, finalY);

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
