package net.judah.gui.fx;

import java.awt.Component;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import judahzone.api.FX;
import judahzone.gui.Updateable;
import lombok.RequiredArgsConstructor;
import net.judah.channel.Channel;

@RequiredArgsConstructor
public class Row implements Updateable {

	protected final Channel channel;

	private final ArrayList<Component> controls = new ArrayList<>();

	public void add(FXAware component) {
		if (component instanceof Component c)
			controls.add(c);
		else
			throw new InvalidParameterException("Must be Component");
	}

	public List<Component> list() { return new ArrayList<>(controls); }

	public FX getFx(int idx) {
		return ((FXAware)controls.get(idx)).getFx();
	}


	@Override
	public final void update() {
    	for (Component c : controls)
			if (c instanceof Updateable)
				((Updateable)c).update();
	}

	public Component get(int idx) {
		return controls.get(idx);
	}


}
