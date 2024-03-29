package org.motechproject.mds.ex.entity;

import org.motechproject.mds.ex.MdsException;

/**
 * The <code>EntityNotFoundException</code> exception signals a situation in which an entity with
 * a given id does not exist in database.
 */
public class EntityNotFoundException extends MdsException {
    private static final long serialVersionUID = -4030249523587627059L;

    /**
     * Constructs a new EntityNotFoundException with <i>mds.error.entityNotFound</i> as
     * a message key.
     */
    public EntityNotFoundException() {
        super("mds.error.entityNotFound");
    }
}
