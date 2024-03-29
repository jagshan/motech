package org.motechproject.mds.ex.lookup;

import org.motechproject.mds.ex.MdsException;

/**
 * The <code>LookupReferencedException</code> exception signals a situation in which a lookup is
 * used somewhere
 */
public class LookupReferencedException extends MdsException {

    private static final long serialVersionUID = -2666225480961955432L;

    public LookupReferencedException(String lookups) {
        super("mds.error.lookupReferenced", lookups);
    }
}
