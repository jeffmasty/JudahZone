// Java
package net.judah.gui.drums.views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import javax.swing.JPanel;

import judahzone.gui.Gui;
import judahzone.widgets.Knob;
import net.judah.drums.DrumUpdate;
import net.judah.drums.DrumUpdate.Mode;
import net.judah.drums.fm.Algorithm;
import net.judah.drums.fm.FMOsc;

/**
 * Compact operator-grid renderer for DX9-style algorithms.
 *
 * Carriers painted in carrierColor, modulators in modColor.
 * Heavy layout is cached and only recomputed on algo/size changes.
 *
 * This variant embeds a Knob Swing component at each operator node
 * instead of painting simple circles. Knobs are positioned using
 * the same layout computation; arrows are still painted in the panel
 * background so they appear behind the knobs.
 */
public class DX9Grid extends JPanel {

    private static final Color CARRIER_COLOR = new Color(0x4DA6FF); // blue
    private static final Color MOD_COLOR = new Color(0x3FAF6A); // green
    private static final Color FEEDBACK_COLOR = new Color(0xFFA060);
    private static final Color LABEL = Color.BLACK;
    private static final int PREF_W = 340;
    private static final int PREF_H = 210;

    private static final int PADDING = 32; // ensure knobs stay at least this many px from edges

    private final FMView view;
    private final FMOsc fm;
    private volatile Algorithm algo;

    // cached layout
    private Point2D.Float[] cachedPos;
    private int cachedW = -1;
    private int cachedH = -1;
    private Algorithm cachedAlgo;

    // UI knobs (one per operator).
    private Knob[] knobs;

    // Dynamic node radius derived from knob preferred size. Fallback to default.
    private static final int DEFAULT_NODE_RADIUS = 16;
    private volatile int nodeRadius = DEFAULT_NODE_RADIUS;

    public DX9Grid(FMView view) {
    	this.view = view;
        this.fm = view.getDrum();
        Dimension pref = new Dimension(PREF_W, PREF_H);
        Gui.resize(this, pref);
        setLayout(null);
        ensureKnobs(fm.getOpCount());
        setAlgo(fm.getAlgo());
    }

    /** Update algorithm to display (call from UI thread) */
    public void setAlgo(Algorithm algo) {
        if (this.algo == algo)
            return;
        this.algo = algo;
        // mark cached layout invalid; repaint will recompute lazily
        cachedAlgo = null;
        cachedPos = null;
        updateAll();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 11f));

            Algorithm a = this.algo;
            if (a == null) {
                g.setColor(LABEL);
                g.drawString("No Algorithm", 10, 20);
                return;
            }

            int opCount = Math.min(fm.getOpCount(), a.modulatorsFor().length);

            // recompute layout only when necessary
            int w = getWidth();
            int h = getHeight();
            if (cachedPos == null || cachedW != w || cachedH != h || cachedAlgo != a) {
                cachedPos = computeLayout(a, opCount, w, h);
                cachedW = w;
                cachedH = h;
                cachedAlgo = a;
                // reposition knobs whenever cachedPos changes
                ensureKnobs(opCount);
                positionKnobs(cachedPos, opCount);
            }

            Point2D.Float[] pos = cachedPos;
            int[][] mods = a.modulatorsFor();
            boolean[][] fbs = a.feedbackFlags();

            // Draw connections first (nodes / knobs paint on top)
            for (int dst = 0; dst < Math.min(mods.length, opCount); dst++) {
                int[] srcs = mods[dst];
                if (srcs == null) continue;
                for (int j = 0; j < srcs.length; j++) {
                    int src = srcs[j];
                    if (src < 0 || src >= opCount) continue;
                    boolean isFb = isFeedbackEdge(fbs, dst, j);
                    drawArrow(g, pos[src], pos[dst], 6, isFb);
                }
            }

            drawOperatorLabels(g, pos, opCount, w, h, mods);
            /*// for snapshots, uncomment:
            for (int i = 0; i < opCount; i++)
                drawNode(g, pos[i], i, Topology.isCarrier(a, i)); */

        } finally {
            g.dispose();
        }
    }

    private void drawOperatorLabels(Graphics2D g, Point2D.Float[] pos, int opCount, int w, int h, int[][] mods) {
        for (int i = 0; i < opCount; i++) {
            Point2D.Float p = pos[i];
            if (p == null) continue;


            int paramIndex = view.encode(i);
            String label = fm.val(paramIndex);

            int labelW = g.getFontMetrics().stringWidth(label);
            int labelH = g.getFontMetrics().getHeight();

            Rectangle east = new Rectangle(Math.round(p.x + nodeRadius + 4), Math.round(p.y - labelH / 2f), labelW, labelH);
            Rectangle north = new Rectangle(Math.round(p.x - labelW / 2f), Math.round(p.y - nodeRadius - labelH - 4), labelW, labelH);
            Rectangle west = new Rectangle(Math.round(p.x - nodeRadius - labelW - 4), Math.round(p.y - labelH / 2f), labelW, labelH);

            Rectangle chosen = null;
            if (isRectWithin(east, w, h) && !segmentIntersectsAny(east, pos, mods, opCount))
                chosen = east;
            else if (isRectWithin(north, w, h) && !segmentIntersectsAny(north, pos, mods, opCount))
                chosen = north;
            else if (isRectWithin(west, w, h) && !segmentIntersectsAny(west, pos, mods, opCount))
                chosen = west;
            else if (isRectWithin(east, w, h))
                chosen = east;
            else if (isRectWithin(north, w, h))
                chosen = north;
            else
                chosen = west;


            // draw text with baseline computed from font metrics
            int tx = chosen.x;
            int ty = chosen.y + g.getFontMetrics().getAscent();
            g.setColor(LABEL);
            g.setFont(g.getFont().deriveFont(11f));
            g.drawString(label, tx, ty);
        }
    }

    private boolean isRectWithin(Rectangle r, int w, int h) {
        return r.x >= 0 && r.y >= 0 && r.x + r.width <= w && r.y + r.height <= h;
    }

    private boolean segmentIntersectsAny(Rectangle r, Point2D.Float[] pos, int[][] mods, int opCount) {
        if (mods == null || pos == null) return false;
        for (int dst = 0; dst < Math.min(mods.length, opCount); dst++) {
            int[] srcs = mods[dst];
            if (srcs == null) continue;
            for (int j = 0; j < srcs.length; j++) {
                int src = srcs[j];
                if (src < 0 || src >= opCount) continue;
                Point2D.Float p0 = pos[src];
                Point2D.Float p1 = pos[dst];
                if (p0 == null || p1 == null) continue;
                if (segmentIntersectsRect(p0.x, p0.y, p1.x, p1.y, r))
                    return true;
            }
        }
        return false;
    }

    private boolean segmentIntersectsRect(double x1, double y1, double x2, double y2, Rectangle r) {
        // if either endpoint inside rect -> intersects
        if (r.contains(x1, y1) || r.contains(x2, y2)) return true;
        // check intersection with each rectangle side via Line2D
        double rx = r.x, ry = r.y, rw = r.width, rh = r.height;
        if (Line2D.linesIntersect(x1, y1, x2, y2, rx, ry, rx + rw, ry)) return true; // top
        if (Line2D.linesIntersect(x1, y1, x2, y2, rx + rw, ry, rx + rw, ry + rh)) return true; // right
        if (Line2D.linesIntersect(x1, y1, x2, y2, rx + rw, ry + rh, rx, ry + rh)) return true; // bottom
        if (Line2D.linesIntersect(x1, y1, x2, y2, rx, ry + rh, rx, ry)) return true; // left
        return false;
    }

    // Position or move knobs to match node coordinates (call on layout change)
    private void positionKnobs(Point2D.Float[] pos, int count) {
        if (knobs == null) return;

        // try to refresh nodeRadius based on visible knob size if available
        refreshNodeRadiusFromKnobs();

        for (int i = 0; i < knobs.length; i++) {
            Knob k = knobs[i];
            if (i < count && pos != null && pos.length > i && pos[i] != null) {
                Dimension pref = k.getPreferredSize();
                int w = pref.width;
                int h = pref.height;
                int x = Math.round(pos[i].x - w * 0.5f);
                int y = Math.round(pos[i].y - h * 0.5f);
                k.setBounds(x, y, w, h);
                k.setVisible(true);
                k.setToolTipText("Op " + (i + 1));
                // update knob visual hint for carrier / mod state
                boolean carrier = net.judah.drums.fm.Topology.isCarrier(this.algo, i);
                k.setKnobColor(carrier ? CARRIER_COLOR : MOD_COLOR);
            } else {
                if (k != null) k.setVisible(false);
            }
        }
        repaint(); // ensure arrows redraw under knobs if needed
    }

    // create/ensure knobs array is large enough and added to the panel
    private void ensureKnobs(int required) {
        if (knobs != null && knobs.length >= required) return;
        // remove old knobs if any
        if (knobs != null) {
            for (Knob k : knobs) remove(k);
        }
        knobs = new Knob[Math.max(required, 1)];
        for (int i = 0; i < knobs.length; i++) {
            final int idx = i;
            Knob k = new Knob(val -> {
            	fm.getDrama().set(new DrumUpdate(fm, Mode.FM, view.encode(idx), val));
            	repaint();
			});
            k.setOpaque(false);
            knobs[i] = k;
            add(k);
        }
        // derive node radius from knob preferred size, if possible
        refreshNodeRadiusFromKnobs();
        revalidate();
        repaint();
    }

    // update nodeRadius from knobs[0] preferred size, fallback to default
    private void refreshNodeRadiusFromKnobs() {
        if (knobs != null && knobs.length > 0 && knobs[0] != null) {
            Dimension p = knobs[0].getPreferredSize();
            if (p != null && p.width > 0) {
                // approximate node radius as half the knob width (rounded)
                nodeRadius = Math.max(4, Math.round(p.width * 0.5f));
                return;
            }
        }
        nodeRadius = DEFAULT_NODE_RADIUS;
    }

    // other drawing helpers unchanged (drawNode, drawArrow, isFeedbackEdge)
    private boolean isFeedbackEdge(boolean[][] fbs, int dst, int edgeIdx) {
        if (fbs == null || fbs.length <= dst || fbs[dst] == null)
            return false;
        return edgeIdx < fbs[dst].length && fbs[dst][edgeIdx];
    }

    @SuppressWarnings("unused") // maybe for snapshots...  future DX7 reference
    private void drawNode(Graphics2D g, Point2D.Float p, int idx, boolean carrier) {
        final int R = nodeRadius;
        Color fill = carrier ? CARRIER_COLOR : MOD_COLOR;
        g.setColor(fill);
        g.fillOval(Math.round(p.x - R), Math.round(p.y - R), R * 2, R * 2);
        g.setColor(fill.darker());
        g.setStroke(new BasicStroke(2f));
        g.drawOval(Math.round(p.x - R), Math.round(p.y - R), R * 2, R * 2);

        g.setColor(LABEL);
        String s = Integer.toString(idx + 1);
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, Math.round(p.x - sw * 0.5f), Math.round(p.y + 4));
    }

    private void drawArrow(Graphics2D g, Point2D.Float p0, Point2D.Float p1, int headSize, boolean feedback) {
        double dx = p1.x - p0.x;
        double dy = p1.y - p0.y;
        double len = Math.hypot(dx, dy);
        if (len < 1e-3) return;
        double ux = dx / len, uy = dy / len;
        float sx = p0.x;
        float sy = p0.y;
        float ex = p1.x;
        float ey = p1.y;

        Color prevColor = g.getColor();
        Stroke prevStroke = g.getStroke();

        g.setColor(feedback ? FEEDBACK_COLOR : LABEL);
        if (feedback) {
            g.setStroke(
                    new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] { 6f, 6f }, 0f));
        } else {
            g.setStroke(new BasicStroke(2f));
        }
        g.drawLine(Math.round(sx), Math.round(sy), Math.round(ex), Math.round(ey));

        int pad = nodeRadius + 2;

        float ax = (float) (p1.x - ux * pad);
        float ay = (float) (p1.y - uy * pad);

        AffineTransform at = g.getTransform();
        g.translate(ax, ay);
        double angle = Math.atan2(ey - sy, ex - sx);
        g.rotate(angle);
        int hs = headSize;
        int[] xs = { 0, -hs, -hs };
        int[] ys = { 0, -hs / 2, hs / 2 };
        g.fillPolygon(xs, ys, 3);
        g.setTransform(at);

        g.setColor(prevColor);
        g.setStroke(prevStroke);
    }

	private Point2D.Float[] computeLayout(Algorithm a, int n, int w, int h) {
		Point2D.Float[] out = new Point2D.Float[n];

		// Build targets list (src -> list of dst) ignoring feedback edges for
		// ordering/levels
		@SuppressWarnings("unchecked")
		List<Integer>[] targets = new List[n];
		for (int i = 0; i < n; i++)
			targets[i] = new ArrayList<>();
		int[][] modulatorsFor = a.modulatorsFor();
		boolean[][] fbs = a.feedbackFlags();
		for (int dst = 0; dst < Math.min(modulatorsFor.length, n); dst++) {
			int[] srcs = modulatorsFor[dst];
			if (srcs == null)
				continue;
			for (int j = 0; j < srcs.length; j++) {
				int src = srcs[j];
				if (src < 0 || src >= n)
					continue;
				boolean isFb = (fbs != null && fbs.length > dst && fbs[dst] != null && j < fbs[dst].length
						&& fbs[dst][j]);
				if (isFb)
					continue;
				targets[src].add(dst);
			}
		}

		// Compute depth for each node: carriers (no outgoing targets) -> depth 0
		// (bottom), others = 1 + max(depth of targets)
		Integer[] depth = new Integer[n];
		IntUnaryOperator computeDepth = new IntUnaryOperator() {
			@Override
			public int applyAsInt(int node) {
				if (depth[node] != null)
					return depth[node];
				List<Integer> t = targets[node];
				if (t == null || t.isEmpty()) {
					depth[node] = 0;
					return 0;
				}
				int m = 0;
				for (int dst : t)
					m = Math.max(m, applyAsInt(dst));
				depth[node] = m + 1;
				return depth[node];
			}
		};
		for (int i = 0; i < n; i++)
			computeDepth.applyAsInt(i);

		// Group nodes by depth
		int maxDepth = 0;
		Map<Integer, List<Integer>> rows = new HashMap<>();
		for (int i = 0; i < n; i++) {
			int d = depth[i];
			maxDepth = Math.max(maxDepth, d);
			rows.computeIfAbsent(d, k -> new ArrayList<>()).add(i);
		}

		// bottom row (depth 0) = carriers. distribute evenly across width.
		int marginX = PADDING;
		int cols = Math.max(1, rows.getOrDefault(0, List.of()).size());
		// If all operators are carriers (single depth row), center vertically;
		// otherwise keep bottom padding.
		float bottomY = (maxDepth == 0) ? (h * 0.5f) : (h - PADDING);
		float cellW = (w - 2 * marginX) / (float) cols;
		List<Integer> bottomNodes = rows.getOrDefault(0, new ArrayList<>());
		bottomNodes.sort(Comparator.naturalOrder());
		for (int i = 0; i < bottomNodes.size(); i++) {
			float cx = marginX + i * cellW + cellW * 0.5f;
			out[bottomNodes.get(i)] = new Point2D.Float(cx, bottomY);
		}

		// higher depths
		for (int d = 1; d <= maxDepth; d++) {
			List<Integer> row = rows.getOrDefault(d, new ArrayList<>());
			if (row.isEmpty())
				continue;
			List<NodeX> desired = new ArrayList<>();
			for (int node : row) {
				List<Integer> t = targets[node];
				float sumx = 0f;
				int cnt = 0;
				for (int dst : t) {
					Point2D.Float p = out[dst];
					if (p != null) {
						sumx += p.x;
						cnt++;
					}
				}
				float dx = (cnt > 0) ? (sumx / cnt) : (w * 0.5f);
				desired.add(new NodeX(node, dx));
			}
			desired.sort(Comparator.comparingDouble(nx -> nx.x));
			int rowCols = desired.size();
			float rowMargin = PADDING;
			float rowCellW = (w - 2 * rowMargin) / Math.max(1, rowCols);
			// distribute rows between top padding and bottom padding
			float usableHeight = h - 2 * PADDING;
			float rowY = h - PADDING - d * (usableHeight / Math.max(1, maxDepth));
			for (int i = 0; i < desired.size(); i++) {
				float cx = rowMargin + i * rowCellW + rowCellW * 0.5f;
				out[desired.get(i).idx] = new Point2D.Float(cx, rowY);
			}
		}

		for (int i = 0; i < n; i++)
			if (out[i] == null)
				out[i] = new Point2D.Float(w / 2f, h / 2f);
		return out;

	}
    private static final class NodeX {
        final int idx;
        final float x;
        NodeX(int idx, float x) { this.idx = idx; this.x = x; }
    }

	public void updateAll() {
		if (knobs == null) {
			repaint();
			return;
		}
		for (int i = 0; i < knobs.length; i++) {
			int paramIndex = view.encode(i);
			int val = fm.get(paramIndex);
			if (val != -1 && val != knobs[i].getValue())
				knobs[i].setValue(val);
		}
		repaint();
	}
}
