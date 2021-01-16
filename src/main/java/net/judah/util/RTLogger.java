package net.judah.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.RequiredArgsConstructor;

public class RTLogger {

    @RequiredArgsConstructor private static class Log {
        final String clazz;
        final String msg;
        final boolean warn;
    }

    private static final BlockingQueue<Log> debugQueue = new LinkedBlockingQueue<>(); // logging for Realtime threads

    public static void poll() {
        try {
            Log dat = debugQueue.take();

            if (dat.warn)
                Console.info(dat.clazz + " RT WARN: " + dat.msg);
            else
                Console.info(dat.clazz + " RT: " + dat.msg);

        } catch (InterruptedException e) {}
    }

    public static void log(Object o, String msg) {
        debugQueue.offer(new Log(o.getClass().getSimpleName(), msg, false));
    }

    public static void warn(Object o, String msg) {
        debugQueue.offer(new Log(o.getClass().getCanonicalName(), msg, true));
    }

    public static void warn(Object o, Throwable e) {
        warn(o, e.getLocalizedMessage());
        e.printStackTrace();
    }


}
