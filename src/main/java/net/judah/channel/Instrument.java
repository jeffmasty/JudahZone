package net.judah.channel;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Custom;
import judahzone.fx.Convolution;
import judahzone.fx.MonoFilter;
import judahzone.fx.MonoFilter.Type;
import judahzone.gui.Icons;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import lombok.Getter;
import lombok.Setter;

/** An Instrument is a specific Jack input source with pass filters if mono */
@Getter
public class Instrument extends LineIn {

    @Setter protected JackPort leftPort;
    @Setter protected JackPort rightPort;

    private MonoFilter lp;
    private MonoFilter hp;

    public Instrument(Custom user) {
    	super(user.name(), user.stereo());
    	setUser(user);
    	if (user.iconName() == null	)
			icon = Icons.get("Gear.png");
		else
			icon = Icons.get(user.iconName());
    	if (user.highCutHz() != null) {
    		lp = new MonoFilter(Type.HiCut, user.highCutHz(), 1);
    		effects.add(lp);
			setActive(lp, true);
				}
		if (user.lowCutHz() != null) {
			hp = new MonoFilter(Type.LoCut, user.lowCutHz(), 1);
			effects.add(hp);
			setActive(hp, true);
		}
    }

    /** Mono channel with additional/default HiCut/LoCut */
	@Deprecated public Instrument(String name, String sourcePort, JackPort left, String icon, int lowCut, int hiCut) {
		super(name, Constants.MONO);
		if (icon == null)
			icon = "Gear.png";
		this.icon = Icons.get(icon);
		this.leftPort = left;

		hp = new MonoFilter(Type.LoCut, lowCut, 1);
		lp = new MonoFilter(Type.HiCut, hiCut, 1);
		effects.add(hp);
		effects.add(lp);
		setActive(hp, true);
		setActive(lp, true);
	}

	/** Stereo channel */
	@Deprecated public Instrument(String name, String leftSource, String rightSource, String icon) {
		super(name, Constants.STEREO);
		if (icon != null)
			this.icon = Icons.get(icon);
	}

	@Deprecated public Instrument(String name, String leftSource, String rightSource, String icon, JackPort left, JackPort right) {
		this(name, leftSource, rightSource, icon);
		leftPort = left;
		rightPort = right;
	}

	@Override public final void processImpl() {
		if (isOnMute())
			return;

		// get raw data
		AudioTools.copy(leftPort.getFloatBuffer(), left);
		if (isStereo) {
			AudioTools.copy(rightPort.getFloatBuffer(), right);
			fx();
		}
		else { // Mono: apply custom filter then Convolution (on or off) splits to stereo
			if (hp != null)
				hp.process(left);
			if (lp != null)
				lp.process(left);

			if (effects.contains(IR))
				((Convolution.Mono)IR).process(left);
			gain.monoToStereo(left, left, right);
			hotSwap(); // activate FX
			for (int i = 0, n = active.size(); i < n; i++)
				active.get(i).process(left, right);
		}
	}

}
