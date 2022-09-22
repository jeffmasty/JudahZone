package net.judah.carla;

import static net.judah.util.Constants.*;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.mixer.Instrument;
import net.judah.util.RTLogger;

@Data @AllArgsConstructor @RequiredArgsConstructor
public class Plugin {

	private final String name;
	private final int index;
	private final LineType type;
	private String[] inports;
	private String[] outports;
	private String midiport;
	private boolean active;

	public boolean isStereo() {
		return inports.length == 2 && outports.length == 2;
	}

	public void activate(Instrument ch) {
		new Thread() {

			@Override
			public void run() {
				active = !active;
				try {
					Jack jack = Jack.getInstance();
					JackClient client = JudahZone.getClient();
					if (active) {
						// disconnect standard stage
						jack.disconnect(client, ch.getLeftSource(), ch.getLeftPort().getName());
						if (ch.isStereo())
							jack.disconnect(client, ch.getRightSource(), ch.getRightPort().getName());

						// connect plugin to System port.
						jack.connect(client, ch.getLeftSource(), getInports()[LEFT_CHANNEL]);
						if (isStereo()) {
							if (ch.isStereo())
								jack.connect(client, ch.getRightSource(), getInports()[RIGHT_CHANNEL]);
							else
								jack.connect(client, ch.getLeftSource(), getInports()[RIGHT_CHANNEL]);
						}
						else if (ch.isStereo())
							jack.connect(client, ch.getRightSource(), getInports()[LEFT_CHANNEL]);

						// connect plugin to JudahZone channel
						jack.connect(client, getOutports()[LEFT_CHANNEL], ch.getLeftPort().getName());
						if (isStereo()) {
							if (ch.isStereo())
								jack.connect(client, getOutports()[RIGHT_CHANNEL], ch.getRightPort().getName());
							else
								jack.connect(client, getOutports()[RIGHT_CHANNEL], ch.getLeftPort().getName());
						}
						else if (ch.isStereo())
							jack.connect(client, getOutports()[LEFT_CHANNEL], ch.getRightPort().getName());

					}
					else {
						// disconnect plugin from JudahZone
						jack.disconnect(client, getOutports()[LEFT_CHANNEL], ch.getLeftPort().getName());
						if (isStereo()) {
							if (ch.isStereo())
								jack.disconnect(client, getOutports()[RIGHT_CHANNEL], ch.getRightPort().getName());
							else
								jack.disconnect(client, getOutports()[RIGHT_CHANNEL], ch.getLeftPort().getName());
						}
						else if (ch.isStereo())
							jack.disconnect(client, getOutports()[LEFT_CHANNEL], ch.getRightPort().getName());

						// disconnect plugin from system
						jack.disconnect(client, ch.getLeftSource(), getInports()[LEFT_CHANNEL]);
						if (isStereo()) {
							if (ch.isStereo())
								jack.disconnect(client, ch.getRightSource(), getInports()[RIGHT_CHANNEL]);
							else
								jack.disconnect(client, ch.getLeftSource(), getInports()[RIGHT_CHANNEL]);
						}
						else if (ch.isStereo())
							jack.disconnect(client, ch.getRightSource(), getInports()[LEFT_CHANNEL]);

						// connect standard stage
						jack.connect(client, ch.getLeftSource(), ch.getLeftPort().getName());
						if (ch.isStereo())
							jack.connect(client, ch.getRightSource(), ch.getRightPort().getName());
					}
					JudahZone.getCarla().setActive(index, active);
				} catch (Exception e) {
					RTLogger.warn(this, name + " - " + ch + " : " + e.getMessage());
				}
			}
		}.start();
	}

}
