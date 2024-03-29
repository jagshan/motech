package org.motechproject.osgi.web;

import org.motechproject.osgi.web.util.BundleHeaders;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

public class UIServiceTracker extends ServiceTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIServiceTracker.class);

    private ModuleRegistrationData moduleRegistrationData;

    public UIServiceTracker(BundleContext context, ModuleRegistrationData moduleRegistrationData) {
        super(context, UIFrameworkService.class.getName(), null);

        this.moduleRegistrationData = moduleRegistrationData;
        Bundle bundle = context.getBundle();
        BundleHeaders headers = new BundleHeaders(bundle);

        moduleRegistrationData.setBundle(bundle);
        moduleRegistrationData.setResourcePath(headers.getResourcePath());
    }

    public UIServiceTracker(BundleContextWrapper wrapper, ModuleRegistrationData moduleRegistrationData) {
        this(wrapper.getBundleContext(), moduleRegistrationData);
    }

    @Override
    public Object addingService(ServiceReference ref) {
        Object service = super.addingService(ref);
        register((UIFrameworkService) service);
        return service;
    }

    @Override
    public void removedService(ServiceReference ref, Object service) {
        serviceRemoved((UIFrameworkService) service);
        super.removedService(ref, service);
    }

    @PostConstruct
    public void start() {
        registerServiceIfAvailable();
        open();
    }

    private void registerServiceIfAvailable() {
        ServiceReference serviceReference = context.getServiceReference(UIFrameworkService.class.getName());
        if (serviceReference != null) {
            register((UIFrameworkService) context.getService(serviceReference));
        }
    }

    private void register(UIFrameworkService service) {
        if (service.isModuleRegistered(moduleRegistrationData.getModuleName())) {
            return;
        }

        service.registerModule(moduleRegistrationData);
        LOGGER.debug(String.format("%s registered in UI framework", moduleRegistrationData.getModuleName()));
    }


    private void serviceRemoved(UIFrameworkService service) {
        service.unregisterModule(moduleRegistrationData.getModuleName());
        LOGGER.debug(String.format("%s unregistered from ui framework", moduleRegistrationData.getModuleName()));
    }
}
