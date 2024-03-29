================================
Event and Scheduler Architecture
================================

Event Handling and Scheduling among Modules
===========================================

The following diagram provides three examples of Motech modules: 

* Motech module A publishes events, schedules new jobs and listens for events. An example of a Motech platform that has these three responsibilities is the Message Campaign module.

* Motech module B schedules new jobs.

* Motech module C listens to events of subject X and Y: listener X listens to events of subject X, and listener Y listens to events of subject Y.

	.. image:: events_modules.png
			:scale: 100 %
	  		:alt: Events and Modules Diagram
	   		:align: center

Event Handling and Scheduling Architecture
==========================================

To schedule a job in MOTECH, the core platform exposes the MotechSchedulerService. Clients of the service have an instance of it injected into the class that uses it. This service employs Quartz to schedule MotechScheduledJobs. When triggered, MotechScheduledJobs raise events that are sent through an event relay to the event queue. These messages are dequeued and then received by a consumer, the event listener registry, which in turn discovers all of the listeners on the event and invokes the appropriate method that was listening on that event.

	.. image:: scheduler_arch.png
			:scale: 100 %
	  		:alt: Scheduler Architecture Diagram
	   		:align: center

Notes
-----

* The scheduler service retrieves the Quartz Scheduler from the SchedulerFactoryBean. The service can only schedule MotechScheduledJobs, which all must include a MotechEvent.

* When the trigger for the scheduled job is satisfied, the job is executed and the accompanying Motech event is sent to the SchedulerFireEventGateway interface. This gateway is defined in schedulerFiredEventChannelAdapter.xml and a proxy is generated by Spring at runtime.

* The SchedulerFireEventGateway serves as an outbound message gateway to shuttle messages to the event relay, which then sends JMS messages to the event queue.

* The event queue dequeues messages and routes them to the event listener registry. The core platform has one consumer for events, a channel that routes messages to an event listener registry.

* The event listener registry retrieves the listeners that are listening on the motech event it is relaying (listening is based on the value bound to the motech event's subject key).

* The core platform scans new bundles for any method annotated as a MotechListener, and registers those methods with the listener registry.

* If the event has a specific destination or only one listener, the listener's handle method is called. This will invoke the method that was annotated as a MotechListener. These listener classes can be project specific and will reside outside of the core platform.

* If the event has multiple listeners, a separate event is raised for each listener. The original event object is not handled by a listener in this case.

* The EventListener's handle method will invoke the method that was annotated as a MotechListener. The MotechEvent will be passed to that method as a parameter.

* If you wish to use ActiveMQ web console to view MotechEvents, please note that the server running the console must have the motech-platform-event bundle in its classpath.
  Therefore, either place the jar in the default location, or add a classpath that will contain the motech-platform-event jar.
