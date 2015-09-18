/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.jline.utils.Preconditions.checkNotNull;

/**
 * Signals helpers.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 3.0
 */
public final class Signals {

    private Signals() {
    }

    /**
     *
     * @param name the signal, CONT, STOP, etc...
     * @param handler the callback to run
     *
     * @return an object that needs to be passed to the {@link #unregister(String, Object)}
     *         method to unregister the handler
     */
    public static Object register(String name, Runnable handler) {
        checkNotNull(handler);
        return register(name, handler, handler.getClass().getClassLoader());
    }

    public static Object register(String name, final Runnable handler, ClassLoader loader) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            // Implement signal handler
            Object signalHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{signalHandlerClass}, new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            // only method we are proxying is handle()
                            handler.run();
                            return null;
                        }
                    });
            doRegister(name, signalHandler);
        } catch (Exception e) {
            // Ignore this one too, if the above failed, the signal API is incompatible with what we're expecting
        }
        return null;
    }

    public static Object registerDefault(String name) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            doRegister(name, signalHandlerClass.getField("SIG_DFL").get(null));
        } catch (Exception e) {
            // Ignore this one too, if the above failed, the signal API is incompatible with what we're expecting
        }
        return null;
    }

    public static Object registerIgnore(String name) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            doRegister(name, signalHandlerClass.getField("SIG_IGN").get(null));
        } catch (Exception e) {
            // Ignore this one too, if the above failed, the signal API is incompatible with what we're expecting
        }
        return null;
    }

    public static void unregister(String name, Object previous) {
        try {
            // We should make sure the current signal is the one we registered
            if (previous != null) {
                doRegister(name, previous);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static Object doRegister(String name, Object handler) throws Exception {
        Class<?> signalClass = Class.forName("sun.misc.Signal");
        Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
        Object signal = signalClass.getConstructor(String.class).newInstance(name);
        return signalClass.getMethod("handle", signalClass, signalHandlerClass)
                .invoke(null, signal, handler);
    }

}