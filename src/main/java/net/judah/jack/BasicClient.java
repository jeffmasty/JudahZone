package net.judah.jack;

import static net.judah.jack.Status.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackShutdownCallback;
import org.jaudiolibs.jnajack.JackStatus;
import org.jaudiolibs.jnajack.JackXrunCallback;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.util.Constants;

@Log4j
public abstract class BasicClient extends Thread implements JackXrunCallback, JackProcessCallback, JackShutdownCallback  {

    public static final EnumSet<JackOptions> OPTIONS = EnumSet.of(JackOptions.JackNoStartServer);
    public static final EnumSet<JackStatus> STATUS = EnumSet.noneOf(JackStatus.class);

	protected static final HashMap<JackClient, Integer> xruns = new HashMap<>();

	protected final Jack jack;
    protected JackClient jackclient;
    protected AtomicReference<Status> state = new AtomicReference<>(NEW);
    protected final String clientName;
    @Getter private int buffersize;
	@Getter private int samplerate;
    
    public BasicClient(String name) throws JackException {
    	clientName = name;
		jack = Jack.getInstance();
    	setPriority(Thread.MAX_PRIORITY);
    }

    /** NOTE: blocks while Midi jack client is initialized */
	public JackClient getJackclient() {
    	while (jackclient == null) { // wait for initialization
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) { }
    	}
    	return jackclient;
    }

	/* Create a Thread to run our server. All servers require a Thread to run in. */
	@Override public void run() {
        if (!state.compareAndSet(NEW, INITIALISING)) {
            throw new IllegalStateException("" + state.get());
        }
        try {
        	jackclient = jack.openClient(clientName, OPTIONS, STATUS);
        	samplerate = jackclient.getSampleRate();
        	buffersize = jackclient.getBufferSize();
            initialize();
	        if (state.compareAndSet(INITIALISING, ACTIVE)) {
	        		jackclient.setXrunCallback(this);
	                jackclient.setProcessCallback(this);
	                jackclient.onShutdown(this);
	                jackclient.activate();
	                makeConnections();
		            while (state.get() == Status.ACTIVE) {
		                Thread.sleep(100); // @TODO switch to wait()
		            }
	        }
        } catch (InterruptedException e) {
        } catch (JackException e) {
        	log.error(e.getMessage(), e);
        	log.warn("clientName: " + clientName);
        }

        close();
        state.set(TERMINATED);
	}

	public void close() {
		if (TERMINATED == state.get()) return;
		state = new AtomicReference<>(CLOSING);
		log.warn("Closing Jack client " + clientName);
		if (jackclient != null)
	        try {
	            jackclient.close();
	        } catch (Throwable t) {log.error(t);}
        state = new AtomicReference<>(TERMINATED);
    }

    @Override
	public final void clientShutdown(JackClient client) {
    	log.warn("---- " + client.getName() + " " + this.getClass().getSimpleName() + " disposed by Jack. ----");
    	jackclient = null;
    	close();
    }

	@Override
	public final void xrunOccured(JackClient client) {

		if (xruns.get(client) == null) {
			xruns.put(client, 1);
		}
		else {
			xruns.put(client, xruns.get(client) + 1);
		}
		// log.warn("Xrun by " + client.getName() + " total: " + xruns.get(client));
	}

	public String xrunsToString() {
		int total = 0;
		String result = "";
		for (Map.Entry<JackClient, Integer> entry : xruns.entrySet()) {
			total += entry.getValue();
			result += entry.getKey().getName() + ": " + entry.getValue() + Constants.NL;
		}
		return "size: " + total + Constants.NL + result;
	}

	/** Jack Client created but not started. Register ports. */
	protected abstract void initialize() throws JackException;
	/** Jack Client has been started */
	protected abstract void makeConnections() throws JackException;


}
