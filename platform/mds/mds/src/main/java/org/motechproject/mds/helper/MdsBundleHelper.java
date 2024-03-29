package org.motechproject.mds.helper;

import org.apache.commons.lang.StringUtils;
import org.eclipse.gemini.blueprint.util.OsgiBundleUtils;
import org.eclipse.gemini.blueprint.util.OsgiStringUtils;
import org.motechproject.mds.util.Constants;
import org.motechproject.osgi.web.util.BundleHeaders;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jdo.JDOFatalUserException;
import javax.jdo.spi.JDOImplHelper;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Helper class, that provides utility methods for MDS OSGi bundles.
 */
public final class MdsBundleHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdsBundleHelper.class);

    public static boolean isBundleMdsDependent(Bundle bundle) {
        if (bundle == null) {
            return false;
        } else {
            BundleHeaders headers = new BundleHeaders(bundle);

            // check for Mds imports
            String imports = headers.getStringValue(org.osgi.framework.Constants.IMPORT_PACKAGE);
            if (StringUtils.contains(imports, Constants.Packages.BASE)) {
                return true;
            }

            // finally check for dynamic imports, if someone imports dynamically everything
            // we have to assume it can use MDS
            String dynamicImport = headers.getStringValue(org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE);
            return StringUtils.contains(dynamicImport, '*') ||
                    StringUtils.contains(dynamicImport, Constants.Packages.BASE);
        }
    }

    public static Bundle findMdsEntitiesBundle(BundleContext bundleContext) {
        return OsgiBundleUtils.findBundleBySymbolicName(bundleContext,
                Constants.BundleNames.MDS_ENTITIES_SYMBOLIC_NAME);
    }

    public static Bundle findMdsBundle(BundleContext bundleContext) {
        return OsgiBundleUtils.findBundleBySymbolicName(bundleContext,
                Constants.BundleNames.MDS_BUNDLE_SYMBOLIC_NAME);
    }

    public static boolean isMdsEntitiesBundle(Bundle bundle) {
        return Constants.BundleNames.MDS_ENTITIES_SYMBOLIC_NAME.equals(
                OsgiStringUtils.nullSafeSymbolicName(bundle));
    }

    public static boolean isMdsBundle(Bundle bundle) {
        return Constants.BundleNames.MDS_BUNDLE_SYMBOLIC_NAME.equals(
                OsgiStringUtils.nullSafeSymbolicName(bundle));
    }

    public static boolean isFrameworkBundle(Bundle bundle) {
        return bundle != null && bundle.getBundleId() == 0;
    }

    /**
     * Unregisters all entity classes registered to JDO that are accessible from bundle class loader. This method
     * should be called after bundle that registers MDS entities gets unresolved, so that they are removed from
     * JDO cache. Not doing this might produce hard to track exception when refreshing MDS Entities Bundle after
     * bundle removal.
     *
     * @param bundle the bundle for which entity classes are to be unregistered
     */
    public static void unregisterBundleJDOClasses(Bundle bundle) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (null == bundleWiring) {
            LOGGER.warn("Cannot unregister JDO entity classes: bundle wiring for {} bundle is unavailable.",
                    bundle.getSymbolicName());
        } else {
            ClassLoader bundleClassLoader = bundleWiring.getClassLoader();
            getJDOImplHelper().unregisterClasses(bundleClassLoader);
            LOGGER.info("Unregistered JDO entity classes for bundle {}.", bundle.getSymbolicName());
        }
    }

    private static JDOImplHelper getJDOImplHelper() {
        return (JDOImplHelper) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    return JDOImplHelper.getInstance();
                } catch (SecurityException e) {
                    throw new JDOFatalUserException("Insufficient access granted to org.motechproject.*", e);
                }
            }
        });
    }

    private MdsBundleHelper() {
    }
}
