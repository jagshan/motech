package org.motechproject.osgi.web.it.osgi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.osgi.web.HttpServiceTracker;
import org.motechproject.osgi.web.HttpServiceTrackers;
import org.motechproject.osgi.web.UIServiceTracker;
import org.motechproject.osgi.web.UIServiceTrackers;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.wait.Wait;
import org.motechproject.testing.osgi.wait.WaitCondition;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class BlueprintContextTrackerBundleIT extends BasePaxIT {

    @Inject
    private BundleContext bundleContext;
    @Inject
    private HttpServiceTrackers httpServiceTrackers;
    @Inject
    private UIServiceTrackers uiServiceTrackers;

    @ProbeBuilder
    public TestProbeBuilder build(TestProbeBuilder builder) {
        return builder.setHeader("Blueprint-Enabled", "true")
                .setHeader("Context-File", "META-INF/spring/testWebUtilApplicationContext.xml")
                .setHeader("Context-Path", "/test");
    }

    @Test
    public void testThatHttpServiceTrackerWasAdded() throws InterruptedException {
        final Bundle testBundle = bundleContext.getBundle();

        new Wait(new WaitCondition() {
            @Override
            public boolean needsToWait() {
                return !httpServiceTrackers.isBeingTracked(testBundle);
            }
        }, 5000).start();

        HttpServiceTracker removedHttpServiceTracker = httpServiceTrackers.removeTrackerFor(testBundle);

        assertNotNull(removedHttpServiceTracker);
    }

    @Test
    public void testThatUIServiceTrackerWasAdded() throws InterruptedException {
        final Bundle testBundle = bundleContext.getBundle();

        new Wait(new WaitCondition() {
            @Override
            public boolean needsToWait() {
                return !uiServiceTrackers.isBeingTracked(testBundle);
            }
        }, 5000).start();

        UIServiceTracker removedUiServiceTracker = uiServiceTrackers.removeTrackerFor(testBundle);

        assertNotNull(removedUiServiceTracker);
    }
}
