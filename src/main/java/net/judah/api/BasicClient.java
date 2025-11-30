package net.judah.api;

import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackShutdownCallback;
import org.jaudiolibs.jnajack.JackStatus;

import lombok.RequiredArgsConstructor;
import net.judah.omni.Threads;
import net.judah.util.RTLogger;

/**Creators of Jack clients must manually {@link #start()} the client.  Once started the client will
 * connect to Jack and call lifecycle events: {@link #initialize()} and {@link #makeConnections()} */
public abstract class BasicClient extends Thread implements JackProcessCallback, JackShutdownCallback  {

	enum Status { NEW, INITIALISING, ACTIVE, CLOSING, TERMINATED, OVERDUBBED ;}
    static final EnumSet<JackOptions> OPTIONS = EnumSet.of(JackOptions.JackNoStartServer);
    static final EnumSet<JackStatus> STATUS = EnumSet.noneOf(JackStatus.class);
	public static final EnumSet<JackPortFlags> OUTS = EnumSet.of(JackPortIsOutput);
	public static final EnumSet<JackPortFlags> INS = EnumSet.of(JackPortIsInput);

    public static record Connect(PortBack callback, JackPort ours, String regEx, JackPortType type, EnumSet<JackPortFlags> inOut) {};
	public static record Request(PortBack callback, String portName, JackPortType type, JackPortFlags inOut) {}
    public static interface PortBack {
    	void ready(Request req, JackPort port); }

	@RequiredArgsConstructor public class Requests extends ArrayList<Object> {
    	private final JackClient jack;
    	public void process() {
    		if (isEmpty())
    			return;
    		Object o = getFirst();
    		try {
	    		if (o instanceof Request req) {
	    			registerPort(req);
		    		remove(o);
	    		}
	    		else if (o instanceof Connect con) {
	    			for (String name :  Jack.getInstance().getPorts(jack, con.regEx, con.type, con.inOut)) {
	    				if (name.endsWith("left")) {
	    					if (con.ours.getShortName().endsWith("_R"))
	    						continue;
	    					connect(name, con.ours.getName(), con);
	    				}
	    				else if (name.endsWith("right")) {
	    					if (con.ours.getShortName().endsWith("_L"))
	    						continue;
	    					connect(name, con.ours.getName(), con);
	    				}
	    				else
	    					connect(con.ours.getName(), name, con);
	    			}
	        		remove(o);
	    		}
			} catch (JackException e) { RTLogger.warn(o.toString(), e); }
    	}

    	private void connect(String source, String destination, Connect con) throws JackException {
    		RTLogger.debug("connecting " + source + " to " + destination);
			Jack.getInstance().connect(jack, source, destination);
			Threads.execute(()->con.callback.ready(null, con.ours));
			removeFirst();

    	}
    }


    protected final String clientName;
    protected final Jack jack;
    protected JackClient jackclient;
    protected final AtomicReference<Status> state = new AtomicReference<>(Status.NEW);

    public BasicClient(String name) throws Exception {
    	clientName = name;
    	setPriority(Thread.MAX_PRIORITY);
    	setName(name);
    	jack = Jack.getInstance();
    }

    protected abstract void registerPort(Request req) throws JackException;

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
