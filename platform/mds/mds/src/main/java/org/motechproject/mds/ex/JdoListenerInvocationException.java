package org.motechproject.mds.ex;

/**
 * Exception, that signalizes problems invoking method by the JDO lifecycle listener.
 */
public class JdoListenerInvocationException extends RuntimeException {
    private static final long serialVersionUID = -2504841346403362564L;

    public JdoListenerInvocationException(String message) {
        super(message);
    }

    public JdoListenerInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
