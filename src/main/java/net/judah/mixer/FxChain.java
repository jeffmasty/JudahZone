package net.judah.mixer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.judah.fx.Effect;
import net.judah.fx.Reverb;

public class FxChain implements Iterable<Effect>, Comparator<Effect> {

    protected List<Effect> effects;
    protected final List<Effect> active = new ArrayList<Effect>();;


	public FxChain() {

	}

	public FxChain(Reverb custom) {

	}

	public FxChain(int hzLow, int hzHigh) {

	}

	// sort based on effects.indexOf(a) vs indexOf(b)

	// process(FloatBuffer* outs) {
	//   for(Fx : this)  process(outs)   }
		// deliver for mix





	public void activate(Effect e) {
		// add
		// sort
		active.add(e);
		active.sort(this);
	}

	public void deactivate(Effect e) {
		active.remove(e);

	}

	@Override
	public Iterator<Effect> iterator() {
		return active.iterator();
	}

    public Iterable<Effect> all() {
    	return effects;
    }

	@Override
	public int compare(Effect o1, Effect o2) {
		return effects.indexOf(o1) - effects.indexOf(o2);
	}


}
