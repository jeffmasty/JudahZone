package net.judah.mixer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import judahzone.api.Effect;
import net.judah.fx.Chorus;
import net.judah.fx.Compressor;
import net.judah.fx.Convolution;
import net.judah.fx.Delay;
import net.judah.fx.Filter;
import net.judah.fx.LFO;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;
import net.judah.gui.Bindings;

/**
 * effect indicator lights: red reverb, orange delay, yellow distortion, green chorus,
 * blue LFO
 * @author judah
 */
public class FxLEDs extends JPanel {

	/**Visual positions for LEDs.*/
	private enum FxSlot {
		REVERB, DELAY, OVERDRIVE, CHORUS, CABSIM, CUTFILTER, COMPRESSION, LFO
	}
	private final static int UNITS = FxSlot.values().length;
	/**Map each slot to the effect class it represents (for color lookup). */
	private static final EnumMap<FxSlot, Class<? extends Effect>> colorBySlot = new EnumMap<>(FxSlot.class);
	/**Map concrete effect instances (or their runtime class) to a slot index. */
	private static final Map<Class<? extends Effect>, FxSlot> slotByEffectClass = new HashMap<>();
	private static void bind(FxSlot slot, Class<? extends Effect> effectClass) {
		colorBySlot.put(slot, effectClass);
		slotByEffectClass.put(effectClass, slot);
	}
	static {
		// Define which effect class uses which slot (for color + sync(Effect))
		bind(FxSlot.REVERB,    Reverb.class);
		bind(FxSlot.DELAY,     Delay.class);
		bind(FxSlot.OVERDRIVE, Overdrive.class);
		bind(FxSlot.CHORUS,    Chorus.class);
		bind(FxSlot.CABSIM,    Convolution.class);
		bind(FxSlot.CUTFILTER, Filter.class);
		bind(FxSlot.COMPRESSION, Compressor.class);
		bind(FxSlot.LFO,       LFO.class);
	}

	private final Channel channel;
	private final boolean[] model = new boolean[UNITS];

	public FxLEDs(Channel channel) {
		this.channel = channel;
	}

	@Override public void paint(Graphics g) {
		final Dimension d = getSize();
		float unit = d.width / (float) UNITS;

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, d.width, d.height);
		g.setColor(Color.GRAY);
		g.drawLine(0, 0, d.width, 0);
		g.drawLine(0, d.height - 1, d.width, d.height - 1);

		for (FxSlot slot : FxSlot.values()) {
			int idx = slot.ordinal();
			if (!model[idx]) continue;

			Class<? extends Effect> fxClass = colorBySlot.get(slot);
			if (fxClass == null) continue;

			g.setColor(Bindings.getFx(fxClass));
			int x = (int) (idx * unit);
			g.fillRect(x, 1, (int) Math.ceil(unit), d.height - 2);
		}
	}

	/* @return true if the model changed (needs repaint) */
	private boolean check(FxSlot slot, Effect fx) {
		boolean isActive = active(fx);
		int idx = slot.ordinal();
		if (isActive != model[idx]) {
			model[idx] = isActive;
			return true;
		}
		return false;
	}

	/**For "duo" effects where a single LED represents multiple effects.
	 * LED lights if either is active.
	 * @return true if the model changed */
	private boolean checkDuo(FxSlot slot, Effect fx1, Effect fx2) {
		boolean isActive = active(fx1) || active(fx2);
		int idx = slot.ordinal();
		if (isActive != model[idx]) {
			model[idx] = isActive;
			return true;
		}
		return false;
	}

	private boolean active(Effect fx) {
		return channel.isActive(fx);
	}

	/**
	 * Sync a single effect instance; resolves the correct slot via its class.
	 * If we know which slot that effect maps to, we update just that one LED.
	 */
	public void sync(Effect fx) {
		if (fx == null) return;

		FxSlot slot = slotByEffectClass.get(fx.getClass());
		if (slot == null) {
			// Walk superclasses until we find a mapped one (e.g. Reverb, Convolution)
			Class<?> c = fx.getClass().getSuperclass();
			while (c != null && Effect.class.isAssignableFrom(c)) {
				@SuppressWarnings("unchecked")
				Class<? extends Effect> ec = (Class<? extends Effect>) c;
				slot = slotByEffectClass.get(ec);
				if (slot != null) break;
				c = c.getSuperclass();
			}
		}
		if (slot == null) return; // Not represented by any LED

		if (check(slot, fx)) {
			repaint();
		}
	}

	/**
	 * Full sync of all LEDs from the channel state, using check() and checkDuo()
	 * only.
	 */
	public void sync() {
		boolean needsRepaint = false;

		needsRepaint |= check(FxSlot.REVERB, channel.getReverb());
		needsRepaint |= check(FxSlot.DELAY, channel.getDelay());
		needsRepaint |= check(FxSlot.OVERDRIVE, channel.getOverdrive());
		needsRepaint |= check(FxSlot.CHORUS, channel.getChorus());

		// Duo: LFO 1 & 2 share one LED
		needsRepaint |= checkDuo(FxSlot.LFO, channel.getLfo(), channel.getLfo2());

		// Duo: HiCut & LoCut share one LED
		needsRepaint |= checkDuo(FxSlot.CUTFILTER, channel.getHiCut(), channel.getLoCut());

		needsRepaint |= check(FxSlot.COMPRESSION, channel.getCompression());
		needsRepaint |= check(FxSlot.CABSIM, channel.getIR());

		if (needsRepaint) {
			repaint();
		}
	}

}