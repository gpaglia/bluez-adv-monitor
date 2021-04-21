package com.gpaglia.bt.examples.advmon;

import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.gpaglia.bt.examples.advmon.Commons.*;

/**
 * Implementation of the python example in bluez sources (test/example-adv-monitor)
 *
 * @see <a href="https://git.kernel.org/pub/scm/bluetooth/bluez.git/tree/test/example-adv-monitor">Python example from bluez source tree</a>
 */
public class MonitorApp implements ObjectManager, DBusInterface {

  private static final Logger LOGGER = LoggerFactory.getLogger(MonitorApp.class);

  private static final int CONNECTION_TIMEOUT = 5000; // in ms

  private final int appId;
  private final DBusPath appPath;

  private DBusConnection connection;
  private ObjectManager om;

  private Map<String, Monitor> monitors = new HashMap<>();
  private int lastMonitorId = 0;

  public static void main(String[] args) {
    final MonitorApp app = new MonitorApp((args.length == 0 || !args[1].matches("[1-9]+")) ? 1 : Integer.valueOf(args[1]));
  }

  private MonitorApp(final int appId) {
    this.appId = appId;
    this.appPath = new DBusPath(appPath(appId));

    try {
      LOGGER.info("Starting with appPaht={}\n", appPath);
      
      connection = DBusConnection.getConnection(DBusConnection.DBusBusType.SYSTEM, false, CONNECTION_TIMEOUT);

      LOGGER.info("Connected to dbus with uniqueName={}", connection.getUniqueName());

      connection.addSigHandler(new DBusMatchRule((String) null, ADV_MONITOR_MANAGER_IFACE, null), new DBusSigHandler<>() {

        @Override
        public void handle(DBusSignal s) {
          LOGGER.info("Got signal {}", s);
        }
        
      });

      om = connection.getRemoteObject(BLUEZ_SERVICE_NAME, "/", ObjectManager.class);
      if (om == null) {
        LOGGER.error("Could not get a reference to ObjectManager for service {} in path /", BLUEZ_SERVICE_NAME);
        System.exit(1);
      }

      showObjectsAndInterfaces(om);

      final DBusPath adapterPath = findAdapter(om);
      if (om == null) {
        LOGGER.error("Could not find any supportive adapter object");
        System.exit(1);
      }

      final AdvertisementMonitorManager1 mgr = connection.getRemoteObject(
          BLUEZ_SERVICE_NAME,
          adapterPath.toString(),
          AdvertisementMonitorManager1.class
      );

      if (mgr == null) {
        LOGGER.error("Could not find any AdvertisementMonitorManager1 on path {}", adapterPath.toString());
        System.exit(1);
      } 

      connection.exportObject(appPath.getPath(), this);

      LOGGER.info("Application {} exported", appPath);

      mgr.RegisterMonitor(this.appPath);

      LOGGER.info("Application {} registered with AdvertisementMonitorManager1", appPath);

      final String monitorPath = addMonitor();
      LOGGER.info("Created monitor {}", monitorPath);

      try(Scanner in = new Scanner(System.in)) {
        System.out.print("Press <RET> to end program ... ");
        in.nextLine();
      } finally {
        // nop
      }

      mgr.UnregisterMonitor(this.appPath);
      LOGGER.info("Application {} unregistered with AdvertisementMonitorManager1", appPath);

      connection.unExportObject(appPath.getPath());
      LOGGER.info("Application {} unexported", appPath);

    } catch (DBusException de) {
      LOGGER.error("Got DBusException", de);
      System.exit(1);
    } finally {
      if (connection != null && connection.isConnected()) {
        connection.disconnect();
      }
    }
  }
  
  // ObjectManager

  @Override
  public boolean isRemote() { return false; }


  @Override
  public String getObjectPath() { return this.appPath.getPath(); }


  @Override
  public Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects() {
    final Map<DBusPath, Map<String, Map<String, Variant<?>>>> result = new HashMap<>();

    LOGGER.info("GetManagedObjects called on {}", appPath.getPath());

    for (Monitor m : monitors.values()) {
      result.put(m.getDBusPath(), Map.of(ADV_MONITOR_IFACE, m.GetAll(ADV_MONITOR_IFACE)));
    }

    return result;
  }

  // Local public methods

  public String addMonitor() throws DBusException {
    final int id = lastMonitorId + 1;
    final Monitor monitor = new Monitor(
      connection, 
      monitorPath(appId, id), 
      List.of(new AdFilter((byte) 0x00, (byte) 0x16, new byte[] { (byte) 0x95, (byte) 0xfe }))
    );

    monitors.put(monitor.getObjectPath(), monitor);

    LOGGER.info("Monitor {} wired", monitor.getObjectPath());

    connection.exportObject(monitor.getObjectPath(), monitor);

    LOGGER.info("Monitor {} exported", monitor.getObjectPath());

    connection.sendMessage(
      new ObjectManager.InterfacesAdded(
        this.getObjectPath(), 
        monitor.getDBusPath(), 

        Map.of(ADV_MONITOR_IFACE, monitor.GetAll(ADV_MONITOR_IFACE))
      )
    );

    return monitor.getObjectPath();
  }

  // private methods

  private DBusPath findAdapter(final ObjectManager om) {
    final Map.Entry<DBusPath, Map<String, Map<String, Variant<?>>>> found = om
        .GetManagedObjects()
        .entrySet()
        .stream()
        .filter(e -> e.getValue().containsKey(ADV_MONITOR_MANAGER_IFACE))
        .findFirst()
        .orElse(null);

    if (found != null) {
      LOGGER.info("Found adapter with AdvertisementMonitorManager support, listing manager properties ...");
      for (Map.Entry<String, Variant<?>> prop : found.getValue().get(ADV_MONITOR_MANAGER_IFACE).entrySet()) {
        LOGGER.info("\t property >> key: {}, value: {}", prop.getKey(), prop.getValue().toString());
      }
    }

    return found.getKey();
  }

  private void showObjectsAndInterfaces(final ObjectManager om) {
    final Map<DBusPath, Map<String, Map<String, Variant<?>>>> objects = om.GetManagedObjects();

    // list all paths discovered
    for (DBusPath p: objects.keySet()) {
      final Map<String, Map<String, Variant<?>>> ifaces = objects.get(p);

      LOGGER.info("\t >> discovered path {}", p.toString());

      for (String iface: ifaces.keySet()) {
        LOGGER.info("\t\t >> with interface {}", iface);
      }
    }

  }


}
