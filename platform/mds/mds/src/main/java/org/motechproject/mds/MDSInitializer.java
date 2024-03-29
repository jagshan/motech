package org.motechproject.mds;

import org.motechproject.mds.osgi.EntitiesBundleMonitor;
import org.motechproject.mds.osgi.MdsBundleWatcher;
import org.motechproject.mds.osgi.MdsWeavingHook;
import org.motechproject.server.osgi.PlatformConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;

/**
 * The purpose of this class is to build classes for all entities that are in MDS database at startup.
 * It uses the {@link org.motechproject.mds.builder.MDSConstructor} for generation. Since @PostConstruct does
 * not work with @Transactional, we use a {@link org.springframework.transaction.support.TransactionCallbackWithoutResult}
 * implementation.
 */
@Component("mdsInitializer")
public class MDSInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDSInitializer.class);

    private MdsBundleWatcher mdsBundleWatcher;
    private BundleContext bundleContext;
    private MdsWeavingHook mdsWeavingHook;
    private EntitiesBundleMonitor monitor;
    private EventAdmin eventAdmin;

    @PostConstruct
    public void initMDS() throws IOException {
        LOGGER.info("Initializing MOTECH Data Services");

        // First register the weaving hook
        bundleContext.registerService(WeavingHook.class.getName(), mdsWeavingHook, null);
        LOGGER.info("MDS weaving hook registered");

        try {
            monitor.init();
            LOGGER.info("The entities bundle monitor started");
        } catch (Exception e) {
            LOGGER.error("Error while starting the entities bundle monitor", e);
        }

        // start the bundle watcher
        LOGGER.info("Starting MDS Bundle Watcher");
        try {
            mdsBundleWatcher.start();
            LOGGER.info("Existing bundles have been processed and refreshed");
        } catch (Exception e) {
            LOGGER.error("Error while starting MDS Annotation Processor", e);
        }

        // signal that the startup can commence
        eventAdmin.postEvent(new Event(PlatformConstants.MDS_STARTUP_TOPIC, new HashMap<String, Object>()));

        LOGGER.info("MOTECH data services initialization complete");
    }

    @Autowired
    public void setMdsBundleWatcher(MdsBundleWatcher mdsBundleWatcher) {
        this.mdsBundleWatcher = mdsBundleWatcher;
    }

    @Autowired
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Autowired
    public void setMdsWeavingHook(MdsWeavingHook mdsWeavingHook) {
        this.mdsWeavingHook = mdsWeavingHook;
    }

    @Autowired
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Autowired
    public void setMonitor(EntitiesBundleMonitor monitor) {
        this.monitor = monitor;
    }
}
