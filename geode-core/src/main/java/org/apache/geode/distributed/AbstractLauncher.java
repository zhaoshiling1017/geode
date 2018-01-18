/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.distributed;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.lowerCase;
import static org.apache.geode.distributed.ConfigurationProperties.NAME;
import static org.apache.geode.internal.lang.ClassUtils.forName;
import static org.apache.geode.internal.lang.ObjectUtils.defaultIfNull;
import static org.apache.geode.internal.lang.StringUtils.defaultString;
import static org.apache.geode.internal.lang.SystemUtils.CURRENT_DIRECTORY;
import static org.apache.geode.internal.util.IOUtils.tryGetCanonicalPathElseGetAbsolutePath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.distributed.internal.unsafe.RegisterSignalHandlerSupport;
import org.apache.geode.internal.AvailablePort;
import org.apache.geode.internal.GemFireVersion;
import org.apache.geode.internal.OSProcess;
import org.apache.geode.internal.i18n.LocalizedStrings;
import org.apache.geode.internal.net.SocketCreator;
import org.apache.geode.internal.process.ConnectionFailedException;
import org.apache.geode.internal.process.MBeanInvocationFailedException;
import org.apache.geode.internal.process.PidUnavailableException;
import org.apache.geode.internal.process.ProcessController;
import org.apache.geode.internal.process.ProcessControllerFactory;
import org.apache.geode.internal.process.ProcessControllerParameters;
import org.apache.geode.internal.process.ProcessType;
import org.apache.geode.internal.process.ProcessUtils;
import org.apache.geode.internal.process.UnableToControlProcessException;
import org.apache.geode.internal.util.ArgumentRedactor;
import org.apache.geode.internal.util.SunAPINotFoundException;
import org.apache.geode.management.internal.cli.json.GfJsonObject;

/**
 * The AbstractLauncher class is a base class for implementing various launchers to construct and
 * run different GemFire processes, like Cache Servers, Locators, Managers, HTTP servers and so on.
 *
 * @see java.lang.Comparable
 * @see java.lang.Runnable
 * @see org.apache.geode.lang.Identifiable
 * @since GemFire 7.0
 */
public abstract class AbstractLauncher<T extends Comparable<T>> implements Runnable {

  protected static final Boolean DEFAULT_FORCE = Boolean.FALSE;

  /**
   * @deprecated This timeout is no longer needed.
   */
  @Deprecated
  protected static final long READ_PID_FILE_TIMEOUT_MILLIS = 2 * 1000;

  public static final String DEFAULT_WORKING_DIRECTORY = CURRENT_DIRECTORY;

  public static final String SIGNAL_HANDLER_REGISTRATION_SYSTEM_PROPERTY =
      DistributionConfig.GEMFIRE_PREFIX + "launcher.registerSignalHandlers";

  protected static final String OPTION_PREFIX = "-";

  private static final String SUN_SIGNAL_API_CLASS_NAME = "sun.misc.Signal";

  private volatile boolean debug;

  protected final transient AtomicBoolean running = new AtomicBoolean(false);

  ProcessControllerParameters controllerParameters;

  private static final AtomicReference<AbstractLauncher> INSTANCE = new AtomicReference<>();

  // TODO: use log4j logger instead of JUL
  protected Logger logger = Logger.getLogger(getClass().getName());

  /**
   * Gets the instance of the AbstractLauncher used to launch the GemFire service, or null if this VM
   * does not have an instance of AbstractLauncher indicating no GemFire service is running.
   *
   * @return the instance of AbstractLauncher used to launcher a GemFire service in this VM.
   */
  public static AbstractLauncher getInstance() {
    return INSTANCE.get();
  }

  abstract public Properties getProperties(); // TODO: GEODE-3584

  public AbstractLauncher() {
    try {
      if (Boolean.getBoolean(SIGNAL_HANDLER_REGISTRATION_SYSTEM_PROPERTY)) {
        forName(SUN_SIGNAL_API_CLASS_NAME, new SunAPINotFoundException(
            "WARNING!!! Not running a Sun JVM.  Could not find the sun.misc.Signal class; Signal handling disabled."));
        RegisterSignalHandlerSupport.registerSignalHandlers();
      }
    } catch (SunAPINotFoundException handled) {
      info(handled.getMessage());
    }
  }

  /**
   * Asserts that the specified port is available on all network interfaces on this local system.
   *
   * @param port an integer indicating the network port to listen for client network requests.
   * @throws BindException if the network port is not available.
   */
  protected static void assertPortAvailable(final int port) throws BindException {
    assertPortAvailable(null, port);
  }

  /**
   * Asserts that the specified port is available on the specified network interface, indicated by
   * it's assigned IP address, on this local system.
   *
   * @param bindAddress an InetAddress indicating the bounded network interface to determine whether
   *        the service port is available or not.
   * @param port an integer indicating the network port to listen for client network requests.
   * @throws BindException if the network address and port are not available. Address defaults to
   *         localhost (or all network interfaces on the local system) if null.
   * @see org.apache.geode.internal.AvailablePort
   */
  protected static void assertPortAvailable(final InetAddress bindAddress, final int port)
      throws BindException {
    if (!AvailablePort.isPortAvailable(port, AvailablePort.SOCKET, bindAddress)) {
      throw new BindException(
          String.format("Network is unreachable; port (%1$d) is not available on %2$s.", port,
              bindAddress != null ? bindAddress.getCanonicalHostName() : "localhost"));
    }
  }

  /**
   * Determines whether the specified property with name is set to a value in the referenced
   * Properties. The property is considered "set" if the String value of the property is not
   * non-null, non-empty and non-blank. Therefore, the Properties may "have" a property with name,
   * but having no value as determined by this method.
   *
   * @param properties the Properties used in determining whether the given property is set.
   * @param propertyName a String indicating the name of the property to check if set.
   * @return a boolean indicating whether the specified property with name has been given a value in
   *         the referenced Properties.
   * @see java.util.Properties
   */
  protected static boolean isSet(final Properties properties, final String propertyName) {
    return isNotBlank(properties.getProperty(propertyName));
  }

  /**
   * Loads the GemFire properties at the specified URL.
   *
   * @param url the URL to the gemfire.properties to load.
   * @return a Properties instance populated with the gemfire.properties.
   * @see java.net.URL
   */
  protected static Properties loadGemFireProperties(final URL url) {
    if (url == null) {
      return new Properties();
    }
    Properties properties = new Properties();

    try {
      properties.load(new FileReader(new File(url.toURI())));
    } catch (IOException | URISyntaxException handled) {
      // not in the file system, try the classpath
      loadGemFirePropertiesFromClassPath(properties);
    }

    return properties;
  }

  private static void loadGemFirePropertiesFromClassPath(Properties properties) {
    try {
      properties
          .load(AbstractLauncher.class.getResourceAsStream(DistributedSystem.getPropertiesFile()));
    } catch (IOException | NullPointerException handled) {
      // leave the properties empty
    }
  }

  /**
   * Determines whether the launcher is in debug mode.
   *
   * @return a boolean to indicate whether the launcher is in debug mode.
   * @see #setDebug(boolean)
   */
  public boolean isDebugging() {
    return this.debug;
  }

  /**
   * Sets the debug mode of the GemFire launcher class. This mutable property of the launcher
   * enables the user to turn the debug mode on and off programmatically.
   *
   * @param debug a boolean used to enable or disable debug mode.
   * @see #isDebugging()
   */
  public void setDebug(final boolean debug) {
    this.debug = debug;
  }

  /**
   * Determines whether the service referenced by this launcher is running.
   *
   * @return a boolean valued indicating if the referenced service is running.
   */
  public boolean isRunning() {
    return this.running.get();
  }

  /**
   * Creates a Properties object with configuration settings that the launcher has that should take
   * precedence over anything the user has defined in their gemfire properties file.
   *
   * @return a Properties object with GemFire properties that the launcher has defined.
   * @see #getDistributedSystemProperties(java.util.Properties)
   * @see java.util.Properties
   */
  protected Properties getDistributedSystemProperties() {
    return getDistributedSystemProperties(null);
  }

  /**
   * Creates a Properties object with configuration settings that the launcher has that should take
   * precedence over anything the user has defined in their gemfire properties file.
   *
   * @param defaults default GemFire Distributed System properties as configured in the Builder.
   * @return a Properties object with GemFire properties that the launcher has defined.
   * @see java.util.Properties
   */
  protected Properties getDistributedSystemProperties(final Properties defaults) {
    final Properties distributedSystemProperties = new Properties();

    if (defaults != null) {
      distributedSystemProperties.putAll(defaults);
    }

    if (isNotBlank(getMemberName())) {
      distributedSystemProperties.setProperty(NAME, getMemberName());
    }

    return distributedSystemProperties;
  }

  /**
   * Gets a File reference with the path to the log file for the process.
   *
   * @return a File reference to the path of the log file for the process.
   */
  protected File getLogFile() {
    return new File(getWorkingDirectory(), getLogFileName());
  }

  /**
   * Gets the fully qualified canonical path of the log file for the process.
   *
   * @return a String value indicating the canonical path of the log file for the process.
   */
  protected String getLogFileCanonicalPath() {
    try {
      return getLogFile().getCanonicalPath();
    } catch (IOException handled) {
      return getLogFileName();
    }
  }

  /**
   * Gets the name of the log file used to log information about this GemFire service.
   *
   * @return a String value indicating the name of this GemFire service's log file.
   */
  public abstract String getLogFileName();

  /**
   * Gets the name or ID of the member in the GemFire distributed system. This method prefers name
   * if specified, otherwise the ID is returned. If name was not specified to the Builder that
   * created this Launcher and this call is not in-process, then null is returned.
   *
   * @return a String value indicating the member's name if specified, otherwise the member's ID is
   *         returned if this call is made in-process, or finally, null is returned if neither name
   *         name was specified or the call is out-of-process.
   * @see #getMemberName()
   * @see #getMemberId()
   */
  public String getMember() {
    if (isNotBlank(getMemberName())) {
      return getMemberName();
    }
    if (isNotBlank(getMemberId())) {
      return getMemberId();
    }
    return null;
  }

  /**
   * Gets the ID of the member in the GemFire distributed system as determined and assigned by
   * GemFire when the member process joins the distributed system. Note, this call only works if the
   * API is used in-process.
   *
   * @return a String value indicating the ID of the member in the GemFire distributed system.
   */
  public String getMemberId() {
    final InternalDistributedSystem distributedSystem =
        InternalDistributedSystem.getConnectedInstance();
    return distributedSystem != null ? distributedSystem.getMemberId() : null;
  }

  /**
   * Gets the name of the member in the GemFire distributed system as determined by the 'name'
   * GemFire property. Note, this call only works if the API is used in-process.
   *
   * @return a String value indicating the name of the member in the GemFire distributed system.
   */
  public String getMemberName() {
    final InternalDistributedSystem distributedSystem =
        InternalDistributedSystem.getConnectedInstance();
    return distributedSystem != null ? distributedSystem.getConfig().getName() : null;
  }

  /**
   * Gets the user-specified process ID (PID) of the running GemFire service that AbstractLauncher
   * implementations can use to determine status, or stop the service.
   *
   * @return an Integer value indicating the process ID (PID) of the running GemFire service.
   */
  public abstract Integer getPid();

  /**
   * Gets the name of the GemFire service.
   *
   * @return a String indicating the name of the GemFire service.
   */
  public abstract String getServiceName();

  /**
   * Gets the working directory pathname in which the process will be run.
   *
   * @return a String value indicating the pathname of the Server's working directory.
   */
  public String getWorkingDirectory() {
    return DEFAULT_WORKING_DIRECTORY;
  }

  /**
   * Prints the specified debug message to standard err, replacing any placeholder values with the
   * specified arguments on output, if debugging has been enabled.
   *
   * @param message the String value written to standard err.
   * @param args an Object array containing arguments to replace the placeholder values in the
   *        message.
   * @see java.lang.System#err
   * @see #isDebugging()
   * @see #debug(Throwable)
   * @see #info(Object, Object...)
   */
  protected void debug(final String message, final Object... args) {
    if (isDebugging()) {
      if (args != null && args.length > 0) {
        System.err.printf(message, args);
      } else {
        System.err.print(message);
      }
    }
  }

  /**
   * Prints the stack trace of the given Throwable to standard err if debugging has been enabled.
   *
   * @param t the Throwable who's stack trace is printed to standard err.
   * @see java.lang.System#err
   * @see #isDebugging()
   * @see #debug(String, Object...)
   */
  protected void debug(final Throwable t) {
    if (isDebugging()) {
      t.printStackTrace(System.err);
    }
  }

  /**
   * Prints the specified informational message to standard err, replacing any placeholder values
   * with the specified arguments on output.
   *
   * @param message the String value written to standard err.
   * @param args an Object array containing arguments to replace the placeholder values in the
   *        message.
   * @see java.lang.System#err
   * @see #debug(String, Object...)
   */
  protected void info(final Object message, final Object... args) {
    if (args != null && args.length > 0) {
      System.err.printf(message.toString(), args);
    } else {
      System.err.print(message);
    }
  }

  /**
   * Redirects the standard out and standard err to the configured log file as specified in the
   * GemFire distributed system properties.
   *
   * @param distributedSystem the GemFire model for a distributed system.
   * @throws IOException if the standard out and err redirection was unsuccessful.
   */
  protected void redirectOutput(final DistributedSystem distributedSystem) throws IOException {
    if (distributedSystem instanceof InternalDistributedSystem) {
      OSProcess
          .redirectOutput(((InternalDistributedSystem) distributedSystem).getConfig().getLogFile());
    }
  }

  /**
   * Attempts to determine the state of the service. 
   */
  abstract public ServiceState<T> status();

  abstract boolean isStoppable();

  abstract ServiceState<T> stopInProcess();

  abstract public ServiceState<T> start();

  ServiceState<T> stopWithPid() {
    try {
      final ProcessController controller = createProcessController();
      controller.checkPidSupport();
      controller.stop();
      return createStoppedState();
    } catch (ConnectionFailedException handled) {
      // failed to attach to locator JVM
      return createNoResponseState(handled,
          "Failed to connect to locator with process id " + getPid());
    } catch (IOException | MBeanInvocationFailedException
        | UnableToControlProcessException handled) {
      return createNoResponseState(handled,
          "Failed to communicate with locator with process id " + getPid());
    }
  }

  /**
   * Stop shuts the running service down. Using the API, the service is requested to stop by calling
   * the service object's 'stop' method. Internally, this method is no different than using the
   * AbstractLauncher class from the command-line or from within GemFire shell (Gfsh). In every
   * single case, stop sends a TCP/IP 'shutdown' request on the configured address/port to which the
   * service is bound and listening.
   *
   * If the "shutdown" request is successful, then the service will be 'STOPPED'. Otherwise, the
   * service is considered 'OFFLINE' since the actual state cannot be fully assessed (as in the
   * application process in which the service was hosted may still be running and the service object
   * may still exist even though it is no longer responding to location-based requests). The later
   * is particularly important in cases where the system resources (such as Sockets) may not have
   * been cleaned up yet. Therefore, by returning a status of 'OFFLINE', the value is meant to
   * reflect this in-deterministic state.
   *
   * @return a ServiceState indicating the state of the service after stop has been requested.
   * @see #start()
   * @see #status()
   * @see org.apache.geode.distributed.AbstractLauncher.ServiceState
   * @see org.apache.geode.distributed.AbstractLauncher.Status#NOT_RESPONDING
   * @see org.apache.geode.distributed.AbstractLauncher.Status#STOPPED
   */
  public ServiceState<T> stop() {
    final AbstractLauncher launcher = getInstance();
    // if this instance is running then stop it
    if (isStoppable()) {
      return stopInProcess();
    }
    // if in-process but difference instance of LocatorLauncher
    else if (isPidInProcess() && launcher != null) {
      return launcher.stopInProcess();
    }
    // attempt to stop Locator using pid...
    else if (getPid() != null) {
      return stopWithPid();
    }
    // attempt to stop Locator using the working directory...
    else if (getWorkingDirectory() != null) {
      return stopWithWorkingDirectory();
    } else {
      return createNotRespondingState();
    }
  }

  ServiceState<T> stopWithWorkingDirectory() {
    int parsedPid = 0;
    ProcessType pt = getProcessType();
    try {
      final ProcessController controller =
          new ProcessControllerFactory().createProcessController(this.controllerParameters,
              new File(getWorkingDirectory()), pt.getPidFileName());
      parsedPid = controller.getProcessId();

      // NOTE in-process request will go infinite loop unless we do the following
      if (parsedPid == identifyPid()) {
        final AbstractLauncher runningLauncher = getInstance();
        if (runningLauncher != null) {
          return runningLauncher.stopInProcess();
        }
      }

      controller.stop();
      return createStoppedState();
    } catch (ConnectionFailedException handled) {
      // failed to attach to server JVM
      return createNoResponseState(handled,
          "Failed to connect to service with process id " + parsedPid);
    } catch (FileNotFoundException handled) {
      // could not find pid file
      return createNoResponseState(handled, "Failed to find process file "
          + pt.getPidFileName() + " in " + getWorkingDirectory());
    } catch (IOException | MBeanInvocationFailedException
        | UnableToControlProcessException handled) {
      return createNoResponseState(handled,
          "Failed to communicate with service with process id " + parsedPid);
    } catch (InterruptedException handled) {
      Thread.currentThread().interrupt();
      return createNoResponseState(handled,
          "Interrupted while trying to communicate with service with process id " + parsedPid);
    } catch (PidUnavailableException handled) {
      // couldn't determine pid from within server JVM
      return createNoResponseState(handled, "Failed to find usable process id within file "
          + pt.getPidFileName() + " in " + getWorkingDirectory());
    } catch (TimeoutException handled) {
      return createNoResponseState(handled,
          "Timed out trying to find usable process id within file "
              + pt.getPidFileName() + " in " + getWorkingDirectory());
    }
  }

  abstract ServiceState<T> createNoResponseState(final Exception cause, final String msg);

  abstract ServiceState<T> createNotRespondingState();

  abstract ServiceState<T> createStoppedState();

  abstract ProcessController createProcessController();

  abstract ProcessType getProcessType();

  /**
   * Gets the version of GemFire currently running.
   *
   * @return a String representation of GemFire's version.
   */
  public String version() {
    return GemFireVersion.getGemFireVersion();
  }

  int identifyPid() throws PidUnavailableException {
    return ProcessUtils.identifyPid();
  }

  int identifyPidOrNot() {
    try {
      return identifyPid();
    } catch (PidUnavailableException handled) {
      return -1;
    }
  }

  boolean isPidInProcess() {
    Integer pid = getPid();
    return pid != null && pid == identifyPidOrNot();
  }

  /**
   * The ServiceState is an immutable type representing the state of the specified service at any
   * given moment in time. The ServiceState associates the service with it's state at the exact
   * moment an instance of this class is constructed.
   */
  public abstract static class ServiceState<T extends Comparable<T>> {

    protected static final String JSON_CLASSPATH = "classpath";
    protected static final String JSON_GEMFIREVERSION = "gemFireVersion";
    protected static final String JSON_HOST = "bindAddress";
    protected static final String JSON_JAVAVERSION = "javaVersion";
    protected static final String JSON_JVMARGUMENTS = "jvmArguments";
    protected static final String JSON_LOCATION = "location";
    protected static final String JSON_LOGFILE = "logFileName";
    protected static final String JSON_MEMBERNAME = "memberName";
    protected static final String JSON_PID = "pid";
    protected static final String JSON_PORT = "port";
    protected static final String JSON_STATUS = "status";
    protected static final String JSON_STATUSMESSAGE = "statusMessage";
    protected static final String JSON_TIMESTAMP = "timestamp";
    protected static final String JSON_UPTIME = "uptime";
    protected static final String JSON_WORKINGDIRECTORY = "workingDirectory";

    private static final String DATE_TIME_FORMAT_PATTERN = "MM/dd/yyyy hh:mm a";

    private final Integer pid;

    // NOTE the mutable non-Thread safe List is guarded by a call to Collections.unmodifiableList on
    // initialization
    private final List<String> jvmArguments;

    private final Long uptime;

    private final Status status;

    private final String classpath;
    private final String gemfireVersion;
    private final String host;
    private final String javaVersion;
    private final String logFile;
    private final String memberName;
    private final String port;
    private final String serviceLocation;
    private final String statusMessage;
    private final String workingDirectory;

    private final Timestamp timestamp;

    protected static String format(final Date timestamp) {
      return timestamp == null ? ""
          : new SimpleDateFormat(DATE_TIME_FORMAT_PATTERN).format(timestamp);
    }

    protected static Integer identifyPid() {
      try {
        return ProcessUtils.identifyPid();
      } catch (PidUnavailableException handled) {
        return null;
      }
    }

    protected static String toDaysHoursMinutesSeconds(final Long milliseconds) {
      final StringBuilder buffer = new StringBuilder();

      if (milliseconds != null) {
        long millisecondsRemaining = milliseconds;

        final long days = TimeUnit.MILLISECONDS.toDays(millisecondsRemaining);

        millisecondsRemaining -= TimeUnit.DAYS.toMillis(days);

        final long hours = TimeUnit.MILLISECONDS.toHours(millisecondsRemaining);

        millisecondsRemaining -= TimeUnit.HOURS.toMillis(hours);

        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millisecondsRemaining);

        millisecondsRemaining -= TimeUnit.MINUTES.toMillis(minutes);

        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millisecondsRemaining);

        if (days > 0) {
          buffer.append(days).append(days > 1 ? " days " : " day ");
        }

        if (hours > 0) {
          buffer.append(hours).append(hours > 1 ? " hours " : " hour ");
        }

        if (minutes > 0) {
          buffer.append(minutes).append(minutes > 1 ? " minutes " : " minute ");
        }

        buffer.append(seconds).append(seconds == 0 || seconds > 1 ? " seconds" : " second");
      }

      return buffer.toString();
    }

    @SuppressWarnings("unchecked")
    protected ServiceState(final Status status, final String statusMessage, final long timestamp,
        final String serviceLocation, final Integer pid, final Long uptime,
        final String workingDirectory, final List<String> jvmArguments, final String classpath,
        final String gemfireVersion, final String javaVersion, final String logFile,
        final String host, final String port, final String memberName) {
      assert status != null : "The status of the GemFire service cannot be null!";
      this.status = status;
      this.statusMessage = statusMessage;
      this.timestamp = new Timestamp(timestamp);
      this.serviceLocation = serviceLocation;
      this.pid = pid;
      this.uptime = uptime;
      this.workingDirectory = workingDirectory;
      this.jvmArguments = defaultIfNull(Collections.unmodifiableList(jvmArguments),
          Collections.<String>emptyList());
      this.classpath = classpath;
      this.gemfireVersion = gemfireVersion;
      this.javaVersion = javaVersion;
      this.logFile = logFile;
      this.host = host;
      this.port = port;
      this.memberName = memberName;
    }

    /**
     * Marshals this state object into a JSON String.
     *
     * @return a String value containing the JSON representation of this state object.
     */
    public String toJson() {
      final Map<String, Object> map = new HashMap<>();
      map.put(JSON_CLASSPATH, getClasspath());
      map.put(JSON_GEMFIREVERSION, getGemFireVersion());
      map.put(JSON_HOST, getHost());
      map.put(JSON_JAVAVERSION, getJavaVersion());
      map.put(JSON_JVMARGUMENTS, getJvmArguments());
      map.put(JSON_LOCATION, getServiceLocation());
      map.put(JSON_LOGFILE, getLogFile());
      map.put(JSON_MEMBERNAME, getMemberName());
      map.put(JSON_PID, getPid());
      map.put(JSON_PORT, getPort());
      map.put(JSON_STATUS, getStatus().getDescription());
      map.put(JSON_STATUSMESSAGE, getStatusMessage());
      map.put(JSON_TIMESTAMP, getTimestamp().getTime());
      map.put(JSON_UPTIME, getUptime());
      map.put(JSON_WORKINGDIRECTORY, getWorkingDirectory());
      return new GfJsonObject(map).toString();
    }

    public static boolean isStartingNotRespondingOrNull(final ServiceState serviceState) {
      return serviceState == null || serviceState.isStartingOrNotResponding();
    }

    public boolean isStartingOrNotResponding() {
      return Status.NOT_RESPONDING == getStatus() || Status.STARTING == getStatus();
    }

    public boolean isVmWithProcessIdRunning() {
      // note: this will use JNA if available or return false
      return ProcessUtils.isProcessAlive(this.getPid());
    }

    /**
     * Gets the Java classpath used when launching the GemFire service.
     *
     * @return a String value indicating the Java classpath used when launching the GemFire service.
     * @see java.lang.System#getProperty(String) with 'java.class.path'
     */
    public String getClasspath() {
      return classpath;
    }

    /**
     * Gets the version of GemFire used to launch and run the GemFire service.
     *
     * @return a String indicating the version of GemFire used in the running GemFire service.
     */
    public String getGemFireVersion() {
      return gemfireVersion;
    }

    /**
     * Gets the version of Java used to launch and run the GemFire service.
     *
     * @return a String indicating the version of the Java runtime used in the running GemFire
     *         service.
     * @see java.lang.System#getProperty(String) with 'java.verson'
     */
    public String getJavaVersion() {
      return javaVersion;
    }

    /**
     * Gets the arguments passed to the JVM process that is running the GemFire service.
     *
     * @return a List of String value each representing an argument passed to the JVM of the GemFire
     *         service.
     * @see java.lang.management.RuntimeMXBean#getInputArguments()
     */
    public List<String> getJvmArguments() {
      return jvmArguments;
    }

    /**
     * Gets GemFire member's name for the process.
     *
     * @return a String indicating the GemFire member's name for the process.
     */
    public String getMemberName() {
      return this.memberName;
    }

    /**
     * Gets the process ID of the running GemFire service if known, otherwise returns null.
     *
     * @return a integer value indicating the process ID (PID) of the running GemFire service, or
     *         null if the PID cannot be determined.
     */
    public Integer getPid() {
      return pid;
    }

    /**
     * Gets the location of the GemFire service (usually the host in combination with the port).
     *
     * @return a String indication the location (such as host/port) of the GemFire service.
     */
    public String getServiceLocation() {
      return this.serviceLocation;
    }

    /**
     * Gets the name of the GemFire service.
     *
     * @return a String indicating the name of the GemFire service.
     */
    protected abstract String getServiceName();

    /**
     * Gets the state of the GemFire service.
     *
     * @return a Status enumerated type representing the state of the GemFire service.
     * @see org.apache.geode.distributed.AbstractLauncher.Status
     */
    public Status getStatus() {
      return status;
    }

    /**
     * Gets description of the the service's current state.
     *
     * @return a String describing the service's current state.
     */
    public String getStatusMessage() {
      return statusMessage;
    }

    /**
     * The date and time the GemFire service was last in this state.
     *
     * @return a Timestamp signifying the last date and time the GemFire service was in this state.
     * @see java.sql.Timestamp
     */
    public Timestamp getTimestamp() {
      return (Timestamp) timestamp.clone();
    }

    /**
     * Gets the amount of time in milliseconds that the JVM process with the GemFire service has
     * been running.
     *
     * @return a long value indicating the number of milliseconds that the GemFire service JVM has
     *         been running.
     * @see java.lang.management.RuntimeMXBean#getUptime()
     */
    public Long getUptime() {
      return uptime;
    }

    /**
     * Gets the directory in which the GemFire service is running. This is also the location where
     * all GemFire service files (log files, the PID file, and so on) are written.
     *
     * @return a String value indicating the GemFire service's working (running) directory.
     */
    public String getWorkingDirectory() {
      return defaultIfNull(workingDirectory, DEFAULT_WORKING_DIRECTORY);
    }

    /**
     * Gets the path of the log file for the process.
     *
     * @return a String value indicating the path of the log file for the process.
     */
    public String getLogFile() {
      return this.logFile;
    }

    /**
     * Gets the host or IP address for the process and its service.
     *
     * @return a String value representing the host or IP address for the process and its service.
     */
    public String getHost() {
      return this.host;
    }

    /**
     * Gets the port for the process and its service.
     *
     * @return an Integer value indicating the port for the process and its service.
     */
    public String getPort() {
      return this.port;
    }

    /**
     * Gets a String describing the state of the GemFire service.
     *
     * @return a String describing the state of the GemFire service.
     */
    @Override
    public String toString() {
      switch (getStatus()) {
        case STARTING:
          return LocalizedStrings.Launcher_ServiceStatus_STARTING_MESSAGE.toLocalizedString(
              getServiceName(), getWorkingDirectory(), getServiceLocation(), getMemberName(),
              toString(getTimestamp()), toString(getPid()), toString(getGemFireVersion()),
              toString(getJavaVersion()), getLogFile(), ArgumentRedactor.redact(getJvmArguments()),
              toString(getClasspath()));
        case ONLINE:
          return LocalizedStrings.Launcher_ServiceStatus_RUNNING_MESSAGE.toLocalizedString(
              getServiceName(), getWorkingDirectory(), getServiceLocation(), getMemberName(),
              getStatus(), toString(getPid()), toDaysHoursMinutesSeconds(getUptime()),
              toString(getGemFireVersion()), toString(getJavaVersion()), getLogFile(),
              ArgumentRedactor.redact(getJvmArguments()), toString(getClasspath()));
        case STOPPED:
          return LocalizedStrings.Launcher_ServiceStatus_STOPPED_MESSAGE
              .toLocalizedString(getServiceName(), getWorkingDirectory(), getServiceLocation());
        default: // NOT_RESPONDING
          return LocalizedStrings.Launcher_ServiceStatus_MESSAGE.toLocalizedString(getServiceName(),
              getWorkingDirectory(), getServiceLocation(), getStatus());
      }
    }

    // a timestamp (date/time) formatted as MM/dd/yyyy hh:mm a
    protected String toString(final Date dateTime) {
      return format(dateTime);
    }

    // the value of a Number as a String, or "" if null
    protected String toString(final Number value) {
      return defaultString(value);
    }

    // a String concatenation of all values separated by " "
    protected String toString(final Object... values) {
      return values == null ? "" : join(values, " ");
    }

    // the value of the String, or "" if value is null
    protected String toString(final String value) {
      return defaultIfNull(value, "");
    }
  }

  /**
   * The Status enumerated type represents the various lifecycle states of a GemFire service (such
   * as a Cache Server, a Locator or a Manager).
   */
  public enum Status {
    NOT_RESPONDING(LocalizedStrings.Launcher_Status_NOT_RESPONDING.toLocalizedString()),
    ONLINE(LocalizedStrings.Launcher_Status_ONLINE.toLocalizedString()),
    STARTING(LocalizedStrings.Launcher_Status_STARTING.toLocalizedString()),
    STOPPED(LocalizedStrings.Launcher_Status_STOPPED.toLocalizedString());

    private final String description;

    Status(final String description) {
      assert isNotBlank(description) : "The Status description must be specified!";
      this.description = lowerCase(description);
    }

    /**
     * Looks up the Status enum type by description. The lookup operation is case-insensitive.
     *
     * @param description a String value describing the service's status.
     * @return a Status enumerated type matching the description.
     */
    public static Status valueOfDescription(final String description) {
      for (Status status : values()) {
        if (status.getDescription().equalsIgnoreCase(description)) {
          return status;
        }
      }

      return null;
    }

    /**
     * Gets the description of the Status enum type.
     *
     * @return a String describing the Status enum type.
     */
    public String getDescription() {
      return description;
    }

    /**
     * Gets a String representation of the Status enum type.
     *
     * @return a String representing the Status enum type.
     * @see #getDescription()
     */
    @Override
    public String toString() {
      return getDescription();
    }
  }

  /**
   * Following the Builder design pattern, the AbstractLauncher Builder is used to configure and
   * create a properly initialized instance of a subclass of AbstractLauncher class for running
   * the Launcher and performing other Launcher operations.
   */
  public abstract static class Builder {

    /* protected static final Command DEFAULT_COMMAND = Command.UNSPECIFIED;
     * GEODE-3584: Command is an enumeration that has to be refactored as well */

    private Boolean debug;
    private Boolean deletePidFileOnStop;
    private Boolean force;
    private Boolean help;
    private Boolean redirectOutput;
    /* private Command command; // GEODE-3584; see above */

    private InetAddress bindAddress;
    private boolean bindAddressSetByUser;

    private Integer pid;
    protected Integer port;

    private final Properties distributedSystemProperties = new Properties();

    private String hostNameForClients;
    private String memberName;
    protected String workingDirectory;
    private static final String SERVICE_NAME = "Service";
    // should always be overridden by subclass

    /**
     * Default constructor used to create an instance of the Builder class for programmatical
     * access.
     */
    public Builder() {}

    /**
     * Constructor used to create and configure an instance of the Builder class with the specified
     * arguments, often passed from the command-line when launching an instance of this class from
     * the command-line using the Java launcher.
     *
     * @param args the array of arguments used to configure the Builder.
     */
    public Builder(final String... args) {
      parseArguments(args != null ? args : new String[0]);
    }

    abstract protected void parseArguments(final String... args);

    /**
     * Iterates the list of arguments in search of the target launcher command.
     *
     * @param args an array of arguments from which to search for the service launcher command.
     * @see org.apache.geode.distributed.AbstractLauncher.Command#valueOfName(String)
     * @see #parseArguments(String...)
     */
    abstract protected void parseCommand(final String... args); // TODO
    /** GEODE-3584: Command needs refactoring as well
    protected void parseCommand(final String... args) {
      // search the list of arguments for the command; technically, the command should be the first
      // argument in the
      // list, but does it really matter? stop after we find one valid command.
      if (args != null) {
        for (String arg : args) {
          final Command command = Command.valueOfName(arg);
          if (command != null) {
            setCommand(command);
            break;
          }
        }
      }
    }
    */

    /**
     * Iterates the list of arguments in search of the Launcher's GemFire member name. If the
     * argument does not start with '-' or is not the name of a Launcher launcher command, then the
     * value is presumed to be the member name for the Launcher in GemFire.
     *
     * @param args the array of arguments from which to search for the Launcher's member name in
     *        GemFire.
     * @see org.apache.geode.distributed.AbstractLauncher.Command#isCommand(String)
     * @see #parseArguments(String...)
     */
    abstract protected void parseMemberName(final String... args); // TODO: GEODE-3584
    /** GEODE-3584: Command needs refactoring as well
    protected void parseMemberName(final String... args) {
      if (args != null) {
        for (String arg : args) {
          if (!(arg.startsWith(OPTION_PREFIX) || Command.isCommand(arg))) {
            setMemberName(arg);
            break;
          }
        }
      }
    }
    */

    /** TODO: GEODE-3584
     * Gets the launcher command used during the invocation of the AbstractLauncher.
     *
     * @return the launcher command used to invoke (run) the AbstractLauncher class.
     * @see #setCommand(org.apache.geode.distributed.AbstractLauncher.Command)
     * @see AbstractLauncher.Command
     */
    /** TODO: GEODE-3584 // Command is an enumeration that needs refactoring
    public Command getCommand() {
      return defaultIfNull(this.command, DEFAULT_COMMAND);
    } */

    /**
     * Sets the launcher command used during the invocation of the subclass of AbstractLauncher
     *
     * @param command the targeted launcher command used during the invocation (run) of
     *        AbstractLauncher.
     * @return this Builder instance.
     * @see #getCommand()
     * @see AbstractLauncher.Command
     */
    /** TODO: GEODE-3584 // Command is an enumeration that needs refactoring
    public Builder setCommand(final Command command) {
      this.command = command;
      return this;
    } */

    /**
     * Determines whether the new instance of the AbstractLauncher will be set to debug mode.
     *
     * @return a boolean value indicating whether debug mode is enabled or disabled.
     * @see #setDebug(Boolean)
     */
    public Boolean getDebug() {
      return this.debug;
    }

    /**
     * Sets whether the new instance of the AbstractLauncher will be set to debug mode.
     *
     * @param debug a boolean value indicating whether debug mode is to be enabled or disabled.
     * @return this Builder instance.
     * @see #getDebug()
     */
    public Builder setDebug(final Boolean debug) {
      this.debug = debug;
      return this;
    }

    /**
     * Determines whether the Geode service should delete the pid file when its service stops or
     * when the JVM exits.
     *
     * @return a boolean value indicating if the pid file should be deleted when this service stops
     *         or when the JVM exits.
     * @see #setDeletePidFileOnStop(Boolean)
     */
    public Boolean getDeletePidFileOnStop() {
      return this.deletePidFileOnStop;
    }

    /**
     * Sets whether the Geode service should delete the pid file when its service stops or when the
     * JVM exits.
     *
     * @param deletePidFileOnStop a boolean value indicating if the pid file should be deleted when
     *        this service stops or when the JVM exits.
     * @return this Builder instance.
     * @see #getDeletePidFileOnStop()
     */
    public Builder setDeletePidFileOnStop(final Boolean deletePidFileOnStop) {
      this.deletePidFileOnStop = deletePidFileOnStop;
      return this;
    }

    /**
     * Gets the GemFire Distributed System (cluster) Properties configuration.
     *
     * @return a Properties object containing configuration settings for the GemFire Distributed
     *         System (cluster).
     * @see java.util.Properties
     */
    public Properties getDistributedSystemProperties() {
      return this.distributedSystemProperties;
    }

    /**
     * Gets the boolean value used by the service to determine if it should overwrite the PID file
     * if it already exists.
     *
     * @return the boolean value specifying whether or not to overwrite the PID file if it already
     *         exists.
     * @see #setForce(Boolean)
     */
    public Boolean getForce() {
      return defaultIfNull(this.force, DEFAULT_FORCE);
    }

    /**
     * Sets the boolean value used by the service to determine if it should overwrite the PID file
     * if it already exists.
     *
     * @param force a boolean value indicating whether to overwrite the PID file when it already
     *        exists.
     * @return this Builder instance.
     * @see #getForce()
     */
    public Builder setForce(final Boolean force) {
      this.force = force;
      return this;
    }

    /**
     * Determines whether the new instance of AbstractLauncher will be used to output help
     * information for either a specific command, or for using AbstractLauncher in general.
     *
     * @return a boolean value indicating whether help will be output during the invocation of
     *         AbstractLauncher.
     * @see #setHelp(Boolean)
     */
    public Boolean getHelp() {
      return this.help;
    }

    /**
     * Determines whether help has been enabled.
     *
     * @return a boolean indicating if help was enabled.
     */
    boolean isHelping() {
      return Boolean.TRUE.equals(getHelp());
    }

    /**
     * Sets whether the new instance of AbstractLauncher will be used to output help information for
     * either a specific command, or for using AbstractLauncher in general.
     *
     * @param help a boolean indicating whether help information is to be displayed during
     *        invocation of AbstractLauncher.
     * @return this Builder instance.
     * @see #getHelp()
     */
    public Builder setHelp(final Boolean help) {
      this.help = help;
      return this;
    }

    /** GEODE-3584: This is similar to isServerBindAddressByUser in ServerLauncher */
    /**
    boolean isBindAddressSpecified() {
      return (getBindAddress() != null);

    }*/

    /**
     * Gets the IP address to which the service has bound itself listening for client requests.
     *
     * @return an InetAddress with the IP address or host name on which the service is bound and
     *         listening.
     * @see #setBindAddress(String)
     * @see java.net.InetAddress
     */
    public InetAddress getBindAddress() {
      return this.bindAddress;
    }

    /**
     * Sets the IP address as an java.net.InetAddress to which the service has bound itself
     * listening for client requests.
     *
     * @param bindAddress the InetAddress with the IP address or host name on which the service is
     *        bound and listening.
     * @return this Builder instance.
     * @throws IllegalArgumentException wrapping the UnknownHostException if the IP address or
     *         host name for the bind address is unknown.
     * @see #getBindAddress()
     * @see java.net.InetAddress
     */
    public Builder setBindAddress(final String bindAddress) {
      if (isBlank(bindAddress)) {
        this.bindAddress = null;
        return this;
      }
        // NOTE only set the 'bind address' if the user specified a value
        else {
        try {
          InetAddress address = InetAddress.getByName(bindAddress);
          if (SocketCreator.isLocalHost(address)) {
            this.bindAddress = address;
            this.bindAddressSetByUser = true;
            return this;
          } else {
            throw new IllegalArgumentException(
                bindAddress + " is not an address for this machine.");
          }
        } catch (UnknownHostException e) {
          throw new IllegalArgumentException(
              LocalizedStrings.Launcher_Builder_UNKNOWN_HOST_ERROR_MESSAGE
                  .toLocalizedString(SERVICE_NAME),
              e);
        }
      }
    }

    boolean isBindAddressSetByUser() {
      return this.bindAddressSetByUser;
    }

    /**
     * Gets the host name used by clients to lookup the service.
     *
     * @return a String indicating the host name service binding used in client lookups.
     * @see #setHostNameForClients(String)
     */
    public String getHostNameForClients() {
      return this.hostNameForClients;
    }

    /**
     * Sets the host name used by clients to lookup the Launcher.
     *
     * @param hostNameForClients a String indicating the host name Launcher binding used in client
     *        lookups.
     * @return this Builder instance.
     * @throws IllegalArgumentException if the host name was not specified (is blank or empty), such
     *         as when the --hostname-for-clients command-line option may have been specified
     *         without any argument.
     * @see #getHostNameForClients()
     */
    public Builder setHostNameForClients(final String hostNameForClients) {
      if (isBlank(hostNameForClients)) {
        throw new IllegalArgumentException(
            LocalizedStrings.LocatorLauncher_Builder_INVALID_HOSTNAME_FOR_CLIENTS_ERROR_MESSAGE
                .toLocalizedString());
      }
      this.hostNameForClients = hostNameForClients;
      return this;
    }

    /**
     * Gets the member name of this service in GemFire.
     *
     * @return a String indicating the member name of this service in GemFire.
     * @see #setMemberName(String)
     */
    public String getMemberName() {
      return this.memberName;
    }

    /**
     * Sets the member name of the service in GemFire.
     *
     * @param memberName a String indicating the member name of this service in GemFire.
     * @return this Builder instance.
     * @throws IllegalArgumentException if the member name is invalid.
     * @see #getMemberName()
     */
    public Builder setMemberName(final String memberName) {
      if (isBlank(memberName)) {
        throw new IllegalArgumentException(
            LocalizedStrings.Launcher_Builder_MEMBER_NAME_ERROR_MESSAGE
                .toLocalizedString(SERVICE_NAME));
      }
      this.memberName = memberName;
      return this;
    }

    /**
     * Gets the process ID (PID) of the running service indicated by the user as an argument to the
     * AbstractLauncher. This PID is used by the launcher to determine the service's status,
     * or invoke shutdown on the Launcher.
     *
     * @return a user specified Integer value indicating the process ID of the running Launcher.
     * @see #setPid(Integer)
     */
    public Integer getPid() {
      return this.pid;
    }

    /**
     * Sets the process ID (PID) of the running Launcher indicated by the user as an argument to the
     * AbstractLauncher. This PID will be used by the Launcher launcher to determine the Launcher's
     * status, or invoke shutdown on the Launcher.
     *
     * @param pid a user specified Integer value indicating the process ID of the running Launcher.
     * @return this Builder instance.
     * @throws IllegalArgumentException if the process ID (PID) is not valid (greater than zero if
     *         not null).
     * @see #getPid()
     */
    public Builder setPid(final Integer pid) {
      if (pid != null && pid < 0) {
        throw new IllegalArgumentException(
            LocalizedStrings.Launcher_Builder_PID_ERROR_MESSAGE.toLocalizedString());
      }
      this.pid = pid;
      return this;
    }

    /**
     * Gets the port number used by the service to listen for client requests. If the port was not
     * specified, then the default service port (10334) is returned.
     *
     * @return the specified service port or the default port if unspecified.
     * @see #setPort(Integer)
     */
    abstract public Integer getPort();

    /**
     * Sets the port number used by the service to listen for client requests. The port number must
     * be between 1 and 65535 inclusive.
     *
     * @param port an Integer value indicating the port used by the service to listen for client
     *        requests.
     * @return this Builder instance.
     * @throws IllegalArgumentException if the port number is not valid.
     * @see #getPort()
     */
    public Builder setPort(final Integer port) {
      // NOTE if the user were to specify a port number of 0, then java.net.ServerSocket will pick
      // an ephemeral port
      // to bind the socket, which we do not want.
      if (port != null && (port < 0 || port > 65535)) {
        throw new IllegalArgumentException(
            LocalizedStrings.Launcher_Builder_INVALID_PORT_ERROR_MESSAGE
                .toLocalizedString(SERVICE_NAME));
      }
      this.port = port;
      return this;
    }

    /**
     * Determines whether the new instance of AbstractLauncher will redirect output to system logs
     * when starting a service.
     *
     * @return a boolean value indicating if output will be redirected to system logs when starting
     *         a service
     * @see #setRedirectOutput(Boolean)
     */
    public Boolean getRedirectOutput() {
      return this.redirectOutput;
    }

    /**
     * Determines whether redirecting of output has been enabled.
     *
     * @return a boolean indicating if redirecting of output was enabled.
     */
    private boolean isRedirectingOutput() {
      return Boolean.TRUE.equals(getRedirectOutput());
    }

    /**
     * Sets whether the new instance of AbstractLauncher will redirect output to system logs when
     * starting a service.
     *
     * @param redirectOutput a boolean value indicating if output will be redirected to system logs
     *        when starting a service.
     * @return this Builder instance.
     * @see #getRedirectOutput()
     */
    public Builder setRedirectOutput(final Boolean redirectOutput) {
      this.redirectOutput = redirectOutput;
      return this;
    }

    boolean isWorkingDirectorySpecified() {
      return isNotBlank(this.workingDirectory);
    }

    /**
     * Gets the working directory pathname in which the service will be ran. If the directory is
     * unspecified, then working directory defaults to the current directory.
     *
     * @return a String indicating the working directory pathname.
     * @see #setWorkingDirectory(String)
     */
    public String getWorkingDirectory() {
      return tryGetCanonicalPathElseGetAbsolutePath(
          new File(defaultIfBlank(this.workingDirectory, DEFAULT_WORKING_DIRECTORY)));
    }

    /**
     * Sets the working directory in which the service will be ran. This also the directory in which
     * all service's files (such as log and license files) will be written. If the directory is
     * unspecified, then the working directory defaults to the current directory.
     *
     * @param workingDirectory a String indicating the pathname of the directory in which the
     *        service will be ran.
     * @return this Builder instance.
     * @throws IllegalArgumentException wrapping a FileNotFoundException if the working directory
     *         pathname cannot be found.
     * @see #getWorkingDirectory()
     * @see java.io.FileNotFoundException
     */
    public Builder setWorkingDirectory(final String workingDirectory) {
      if (!new File(defaultIfBlank(workingDirectory, DEFAULT_WORKING_DIRECTORY)).isDirectory()) {
        throw new IllegalArgumentException(
            LocalizedStrings.Launcher_Builder_WORKING_DIRECTORY_NOT_FOUND_ERROR_MESSAGE
                .toLocalizedString(SERVICE_NAME),
            new FileNotFoundException(workingDirectory));
      }
      this.workingDirectory = workingDirectory;
      return this;
    }

    /**
     * Sets a GemFire Distributed System Property.
     *
     * @param propertyName a String indicating the name of the GemFire Distributed System property
     *        as described in {@link ConfigurationProperties}
     * @param propertyValue a String value for the GemFire Distributed System property.
     * @return this Builder instance.
     */
    public Builder set(final String propertyName, final String propertyValue) {
      this.distributedSystemProperties.setProperty(propertyName, propertyValue);
      return this;
    }

    /**
     * Validates the configuration settings and properties of this Builder, ensuring that all
     * invariants have been met. Currently, the only invariant constraining the Builder is that the
     * user must specify the member name for the service in the GemFire distributed system as a
     * command-line argument, or by setting the memberName property programmatically using the
     * corresponding setter method. If the member name is not given, then the user must have
     * specified the pathname to the gemfire.properties file before validate is called. It is then
     * assumed, but not further validated, that the user has specified the service's member name in
     * the properties file.
     *
     * @throws IllegalStateException if the Builder is not properly configured.
     */
    protected void validate() {
      if (!isHelping()) {
        validateOnStart();
        validateOnStatus();
        validateOnStop();
        // no validation for 'version' required
      }
    }

    /**
     * Validates the arguments passed to the Builder when the 'start' command has been issued.
     *
     * @see org.apache.geode.distributed.AbstractLauncher.Command#START
     */
    abstract protected void validateOnStart();
    /** GEODE-3584: Command needs to be refactored
    protected void validateOnStart() {
      if (Command.START == getCommand()) {
        if (isBlank(getMemberName())
            && !isSet(System.getProperties(), DistributionConfig.GEMFIRE_PREFIX + NAME)
            && !isSet(getDistributedSystemProperties(), NAME)
            && !isSet(loadGemFireProperties(DistributedSystem.getPropertyFileURL()), NAME)) {
          throw new IllegalStateException(
              LocalizedStrings.Launcher_Builder_MEMBER_NAME_VALIDATION_ERROR_MESSAGE
                  .toLocalizedString(SERVICE_NAME));
        }

        if (!CURRENT_DIRECTORY.equals(getWorkingDirectory())) {
          throw new IllegalStateException(
              LocalizedStrings.Launcher_Builder_WORKING_DIRECTORY_OPTION_NOT_VALID_ERROR_MESSAGE
                  .toLocalizedString(SERVICE_NAME));
        }
      }
    }
    */

    /**
     * Validates the arguments passed to the Builder when the 'status' command has been issued.
     *
     * @see org.apache.geode.distributed.AbstractLauncher.Command#STATUS
     */
    abstract protected void validateOnStatus();
    /** GEODE-3584: Command needs to be refactored
    protected void validateOnStatus() {
      if (Command.STATUS == getCommand()) {
      }
    }
    */

    /**
     * Validates the arguments passed to the Builder when the 'stop' command has been issued.
     *
     * @see org.apache.geode.distributed.AbstractLauncher.Command#STOP
     */
    abstract protected void validateOnStop();
    /** GEODE-3584: Command needs to be refactored
    protected void validateOnStop() {
      if (Command.STOP == getCommand()) {
      }
    }
    */

    /**
     * Validates the Builder configuration settings and then constructs an instance of the
     * service class to invoke operations on a GemFire service.
     *
     * @return a newly constructed instance of service configured with this Builder.
     * @see #validate()
     * @see org.apache.geode.distributed.AbstractLauncher
     */
    abstract public AbstractLauncher build();
  }
}
