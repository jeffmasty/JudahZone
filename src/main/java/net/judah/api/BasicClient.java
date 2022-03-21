package net.judah.api;

import static net.judah.api.Status.ACTIVE;
import static net.judah.api.Status.CLOSING;
import static net.judah.api.Status.INITIALISING;
import static net.judah.api.Status.NEW;
import static net.judah.api.Status.TERMINATED;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackShutdownCallback;
import org.jaudiolibs.jnajack.JackStatus;

import lombok.Getter;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Creators of BasicClients must manually {@link #start()} the client */ 
public abstract class BasicClient extends Thread implements JackProcessCallback, JackShutdownCallback  {

    static final EnumSet<JackOptions> OPTIONS = EnumSet.of(JackOptions.JackNoStartServer);
    static final EnumSet<JackStatus> STATUS = EnumSet.noneOf(JackStatus.class);

    protected final String clientName;
    protected final Jack jack;
    protected JackClient jackclient;
    protected final AtomicReference<Status> state = new AtomicReference<>(NEW);
    @Getter private int bufferSize;
	@Getter private int sampleRate;
    
	/**Once you manually start your BasicClient thread, it should 
	 * connect to Jack and call lifecycle events: {@link #initialize()} and {@link #makeConnections()}
	 * 
	 * @param name
	 * @throws JackException */
    public BasicClient(String name) throws JackException {
    	clientName = name;
    	setPriority(Thread.MAX_PRIORITY);
    	setName(name);
    	jack = Jack.getInstance();
    }

    /** NOTE: blocks while Midi jack client is initialized */
	public JackClient getJackclient() {
    	while (jackclient == null) { // wait for initialization
    		Constants.sleep(10);
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
        	sampleRate = jackclient.getSampleRate();
        	bufferSize = jackclient.getBufferSize();
            initialize();
	        if (state.compareAndSet(INITIALISING, ACTIVE)) {
	                jackclient.setProcessCallback(this);
	                jackclient.onShutdown(this);
	                jackclient.activate();
	                makeConnections();
		            while (state.get() == Status.ACTIVE) {
		                Thread.sleep(251); // @TODO switch to wait()
		            }
	        }
        } catch (Exception e) {
        	RTLogger.warn(this, e);
        }

        close();
        state.set(TERMINATED);
	}

	public void close() {
		if (TERMINATED == state.get()) return;
		state.set(CLOSING);
		System.out.println("Closing Jack client " + clientName);
		if (jackclient != null)
	        try {
	            jackclient.close();
	        } catch (Throwable t) {System.err.println(t.getMessage());}
        state.set(TERMINATED);
    }

    @Override
	public final void clientShutdown(JackClient client) {
    	System.out.println("---- " + client.getName() + " / " + this.getClass().getCanonicalName() + " disposed by Jack. ----");
    	jackclient = null;
    	close();
    }

	/** Jack Client created but not started. Register ports. */
	protected abstract void initialize() throws JackException;
	/** Jack Client has been started */
	protected abstract void makeConnections() throws JackException;


}
