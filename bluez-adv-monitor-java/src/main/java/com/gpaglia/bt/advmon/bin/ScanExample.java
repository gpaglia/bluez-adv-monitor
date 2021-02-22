package com.gpaglia.bt.advmon.bin;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Implementation of a scan application using dbus api in java
 *
 */
public class ScanExample {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScanExample.class);

  private static final String DBUS_OM_INTERFACE = "org.freedesktop.DBus.ObjectManager";
  private static final String DBUS_PROPERTIES_INTERFACE = "org.freedesktop.DBus.Properties";
  private static final String BLUEZ_SERVICE_NAME = "org.bluez";

  private static final String BLUEZ_ADAPTER_IF = BLUEZ_SERVICE_NAME + ".Adapter1";

  private static final String SCAN_APP_BASE_PATH = "/org/bluez/example/scan_app/";

  private static final int CONNECTION_TIMEOUT = 5000; // in ms

  public interface Adapter1 extends DBusInterface, ObjectManager, Properties {
    void StartDiscovery();
    void StopDiscovery();
    void RemoveDevice(DBusPath device);
    void SetDiscoveryFilter(Map<String, Variant<?>> filter);
  }

  public static void main(String[] args) {
    final int appId = (args.length == 0 || !args[1].matches("[0-9]+")) ? 0 : Integer.parseInt(args[1]);
    final String appPath = SCAN_APP_BASE_PATH + appId;


    //noinspection EmptyFinallyBlock
    try (DBusConnection conn = DBusConnection.getConnection(
        DBusConnection.DBusBusType.SYSTEM,
        false,
        CONNECTION_TIMEOUT)
    ) {
      LOGGER.info("Starting with uniqueName={}, appId={}, appTaht={}\n", conn.getUniqueName(), appId, appPath);

      ObjectManager om = conn.getRemoteObject(BLUEZ_SERVICE_NAME, "/", ObjectManager.class);
      if (om == null) {
        LOGGER.error("Could not get a reference to ObjectManager for service {} in path /", BLUEZ_SERVICE_NAME);
        System.exit(1);
      }

      showObjectsAndInterfaces(om);

      final DBusPath adapterPath = findAdapter(om, null);
      if (adapterPath == null) {
        LOGGER.error("Could not find any supportive adapter object");
        System.exit(1);
      }

      final Adapter1 adapter = conn.getRemoteObject(
          BLUEZ_SERVICE_NAME,
          adapterPath.toString(),
          Adapter1.class
      );

      if (adapter == null) {
        LOGGER.error("Could not find any Adapter1 on path {}", adapterPath.toString());
        System.exit(1);
      }

      LOGGER.info("Exit normally");

    } catch (DBusException de) {
      LOGGER.error("Got DBusException", de);
    } finally {
      // no op
    }
  }

  private static DBusPath findAdapter(final ObjectManager om, String pattern) {
    final Map.Entry<DBusPath, Map<String, Map<String, Variant<?>>>> found = om
        .GetManagedObjects()
        .entrySet()
        .stream()
        .filter(e -> e.getValue().containsKey(BLUEZ_ADAPTER_IF))
        .filter(e -> pattern == null
            || pattern.equalsIgnoreCase(e.getValue().get(BLUEZ_ADAPTER_IF).get("Address").toString())
            || e.getKey().toString().endsWith(pattern)
        )
        .findFirst()
        .orElse(null);

    if (found != null) {
      LOGGER.info("Found adapter with pattern {} ...", pattern == null ? "<null>" : pattern);

      for (Map.Entry<String, Variant<?>> prop : found.getValue().get(BLUEZ_ADAPTER_IF).entrySet()) {
        LOGGER.info("\t property >> key: {}, value: {}", prop.getKey(), prop.getValue().toString());
      }
    }

    return found == null ? null : found.getKey();
  }

  private static void showObjectsAndInterfaces(final ObjectManager om) {
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
