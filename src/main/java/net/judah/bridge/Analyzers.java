package net.judah.bridge;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import judahzone.api.FX.Calc;
import judahzone.api.FX.Registrar;
import judahzone.util.AudioTools;
import judahzone.util.Memory;
import lombok.RequiredArgsConstructor;
import net.judah.channel.Channel;

@RequiredArgsConstructor
public class Analyzers extends CopyOnWriteArrayList<Calc<?>> implements Registrar {

	private final ArrayList<Channel> selected;

	/** Register/unregister analyzers (e.g. tuner::process or transformer::process). */
	@Override public void register(Calc<?> a) { addIfAbsent(a); }
	@Override public void unregister(Calc<?> a) { remove(a); }


	public void process() {
		if (isEmpty())
			return;

		// active analyzers
	    final float[][] buf = Memory.STEREO.getFrame();
        final float[] l = buf[0];
        final float[] r = buf[1];
        for (int i = 0; i < selected.size(); i++) {
        	Channel ch = selected.get(i);
            AudioTools.mix(ch.getLeft(), l);
            AudioTools.mix(ch.getRight(), r);
        }
        for (int i = 0; i < size(); i++)
        	get(i).process(l, r);
        Memory.STEREO.release(buf);
	}

}
