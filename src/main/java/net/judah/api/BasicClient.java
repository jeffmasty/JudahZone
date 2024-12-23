package net.judah.api;

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
import net.judah.util.RTLogger;

/**Creators of Jack clients must manually {@link #start()} the client.  Once started the client will
 * connect to Jack and call lifecycle events: {@link #initialize()} and {@link #makeConnections()} */
public abstract class BasicClient extends Thread implements JackProcessCallback, JackShutdownCallback  {

	enum Status {
		NEW, INITIALISING, ACTIVE, CLOSING, TERMINATED, OVERDUBBED;
	}

    static final EnumSet<JackOptions> OPTIONS = EnumSet.of(JackOptions.JackNoStartServer);
    static final EnumSet<JackStatus> STATUS = EnumSet.noneOf(JackStatus.class);

    protected final String clientName;
    protected final Jack jack;
    protected JackClient jackclient;
    protected final AtomicReference<Status> state = new AtomicReference<>(Status.NEW);
    @Getter private int bufferSize;
	@Getter private int sampleRate;

    public BasicClient(String name) throws Exception {
    	clientName = name;
    	setPriority(Thread.MAX_PRIORITY);
    	setName(name);
    	jack = Jack.getInstance();
    }

    /** Jack Client created but not started. Register ports in implementation. */
	protected abstract void initialize() throws Exception;
	/** Jack Client has been started */
	protected abstract void makeConnections() throws JackException;


    /** NOTE: blocks while Midi jack client is initialized */
	public JackClient getJackclient() {
	    try {
	    	while (jackclient == null) // wait for initialization
	    		Thread.sleep(10);
    	} catch(Throwable t) {System.err.println(t.getMessage());}
    	return jackclient;
    }

	/** Create a Thread to run our client. All clients require a Thread to run in. */
	@Override public void run() {
        if (!state.compareAndSet(Status.NEW, Status.INITIALISING)) {
            throw new IllegalStateException("" + state.get());
        }
        try {
        	jackclient = jack.openClient(clientName, OPTIONS, STATUS);
        	sampleRate = jackclient.getSampleRate();
        	bufferSize = jackclient.getBufferSize();
            initialize();
	        if (state.compareAndSet(Status.INITIALISING, Status.ACTIVE)) {
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
	}

	public void close() {
		if (Status.TERMINATED == state.get()) return;
		state.set(Status.CLOSING);
		System.out.println("Closing Jack client " + clientName);
		if (jackclient != null)
	        try {
	            jackclient.close();
	            state.set(Status.TERMINATED);
	            jackclient = null;
	        } catch (Throwable t) {System.err.println(t.getMessage());}
    }

    @Override
	public final void clientShutdown(JackClient client) {
    	System.out.println("---- " + client.getName() + " / " + this.getClass().getCanonicalName() + " disposed by Jack. ----");
    	close();
    }

}
