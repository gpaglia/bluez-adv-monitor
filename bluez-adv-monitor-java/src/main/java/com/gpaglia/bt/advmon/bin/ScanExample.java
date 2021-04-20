package com.gpaglia.bt.advmon.bin;

import org.bluez.Adapter1;
import org.bluez.Device1;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractInterfacesAddedHandler;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.interfaces.ObjectManager.InterfacesAdded;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

/**
 * Implementation of a scan application using dbus api in java
 *
 */
public class ScanExample {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScanExample.class);

  private static final String ADDR = "58:2D:34:32:5A:38";
  private static final String SERV_DATA_UUID = "0000fe95-0000-1000-8000-00805f9b34fb";

  private static final String DBUS_OM_INTERFACE = "org.freedesktop.DBus.ObjectManager";
  private static final String DBUS_PROPERTIES_INTERFACE = "org.freedesktop.DBus.Properties";
  private static final String BLUEZ_SERVICE_NAME = "org.bluez";

  private static final String BLUEZ_ADAPTER_IF = BLUEZ_SERVICE_NAME + ".Adapter1";
  private static final String BLUEZ_DEVICE_IF = BLUEZ_SERVICE_NAME + ".Device1";

  private static final String SCAN_APP_BASE_PATH = "/org/bluez/example/scan_app/";

  private static final int CONNECTION_TIMEOUT = 5000; // in ms

  public static void main(String[] args) {
    final int appId = (args.length == 0 || !args[1].matches("[0-9]+")) ? 0 : Integer.parseInt(args[1]);
    final String appPath = SCAN_APP_BASE_PATH + appId;

    // noinspection EmptyFinallyBlock
    try (DBusConnection conn = DBusConnection.getConnection(DBusConnection.DBusBusType.SYSTEM, false,
        CONNECTION_TIMEOUT)) {
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

      final Adapter1 adapter = conn.getRemoteObject(BLUEZ_SERVICE_NAME, adapterPath.toString(), Adapter1.class);

      if (adapter == null) {
        LOGGER.error("Could not find any Adapter1 on path {}", adapterPath.toString());
        System.exit(1);
      }

      LOGGER.info("Getting properties for adapter {}", adapterPath.toString());

      final Properties adapterPropsObj = conn.getRemoteObject(BLUEZ_SERVICE_NAME, adapterPath.toString(),
          Properties.class);

      final Map<String, Variant<?>> adapterProps = adapterPropsObj.GetAll("org.bluez.Adapter1");
      if (adapterProps != null) {
        LOGGER.info("Adapter {} properties ({}):", adapterPath.toString(), adapterProps.size());
        for (Map.Entry<String, Variant<?>> e : adapterProps.entrySet()) {
          LOGGER.info("\tproperty key: {} -> value: {}", e.getKey(), e.getValue().toString());
        }
      }

      LOGGER.info("Setting discovery filter for adapter {}", adapterPath);
      final Map<String, Variant<?>> filter = Map.of("Pattern", new Variant<String>(ADDR), "DuplicateData",
          new Variant<Boolean>(true));
      adapter.SetDiscoveryFilter(filter);

      LOGGER.info("Setting interface added callback");
      conn.addSigHandler(ObjectManager.InterfacesAdded.class, new AbstractInterfacesAddedHandler() {
        final Logger logger = LoggerFactory.getLogger(AbstractInterfacesAddedHandler.class);
        @Override
        public void handle(InterfacesAdded s) {
          logger.info(
            "\n *** Source {}, path {}, Interfaces added {}\n", 
            s.getSignalSource().toString(),
            s.getObjectPath(), 
            s.getInterfaces().keySet().toString()
          );
        }
        
      });

      LOGGER.info("Starting discovery, then sleeping ... ");
      adapter.StartDiscovery();
      try {
        Thread.sleep(20 * 1000);
      } catch (InterruptedException ignored) {
        // no op
      }

      adapter.StopDiscovery();
      LOGGER.info("Stopped discovery, showing objects and interfaces again ... ");

      showObjectsAndInterfaces(om);

      LOGGER.info("Getting device remote object and printing its properties ...");
      final DBusPath devicePath = findDevice(om, ADDR);
      if (devicePath == null) {
        LOGGER.info("Device {} not found!", ADDR);
        System.exit(1);
      }

      final Device1 device = conn.getRemoteObject(
        BLUEZ_SERVICE_NAME,
        devicePath.toString(),
        Device1.class
      );

      final Properties devicePropsObj = conn.getRemoteObject(
        BLUEZ_SERVICE_NAME,
        devicePath.toString(),
        Properties.class
      );

      final Map<String, Variant<?>> deviceProps = devicePropsObj.GetAll(BLUEZ_DEVICE_IF);
      if (deviceProps != null) {
        LOGGER.info("Device {} properties:", devicePath.toString());
        for(Map.Entry<String, Variant<?>> e : deviceProps.entrySet()) {
          LOGGER.info("\t Property key: {} -> value: {}", e.getKey(), e.getValue());
        }
      }

      LOGGER.info("About to connect...");
      device.Connect();
      LOGGER.info("Device {} connected, show (another time) objects and interfaces", devicePath.toString());

      try {
        Thread.sleep(1300);
      } catch(InterruptedException ignored) {
        // no op
      }

      showObjectsAndInterfaces(om);

      /*
      LOGGER.info("Monitoring service data {} for device {}...", SERV_DATA_UUID, devicePath);

      for (int i = 0; i < 30; i++) {
        try {
          Thread.sleep(1 * 1000);
        } catch (InterruptedException ignored) {
          // no op
        }
        LOGGER.info("Loop # {}", i);
        final Map<String, Variant<?>> props = devicePropsObj.GetAll(BLUEZ_DEVICE_IF);
        if (props == null || props.isEmpty()) {
          LOGGER.info(" --- No properties, no service data ---");
        } else {
          Map<String, Variant<?>> sd = (Map<String, Variant<?>>) props.get("ServiceData").getValue();
          if (sd.containsKey(SERV_DATA_UUID)) {
            final Variant<?> v = sd.get(SERV_DATA_UUID);
            final Type t = v.getType();
            final String val = Arrays.toString((byte []) v.getValue());
            LOGGER.info("Value type: {}, value: {}", t.toString(), val);
          } else {
            LOGGER.info(" No Service data uuid {}", SERV_DATA_UUID);
          }
        }
      }
      */

      conn.close();

      LOGGER.info("Exit normally");

    } catch (DBusException de) {
      LOGGER.error("Got DBusException", de);
    } catch (Exception e2) {
      LOGGER.error("Got  Generic Exception", e2);
    } finally {
      // no op
    }
  }

  private static void showDeviceHierarchy(DBusConnection conn, DBusPath devicePath) throws DBusException {
    LOGGER.info("Getting object manager for device {}", devicePath);
    final ObjectManager dom = conn.getRemoteObject(
      BLUEZ_SERVICE_NAME,
      devicePath.toString(),
      ObjectManager.class
    );

    if (dom == null) {
      LOGGER.error("Device Object Manager is null!");
      return;
    }

    LOGGER.info("Object hyerarchy for device {}:", devicePath.toString());
    showObjectsAndInterfaces(dom);
  }

  private static DBusPath findDevice(final ObjectManager om, final String address) {
    final Map.Entry<DBusPath, Map<String, Map<String, Variant<?>>>> found = om
        .GetManagedObjects()
        .entrySet()
        .stream()
        .filter(e -> e.getValue().containsKey(BLUEZ_DEVICE_IF))
        .filter(e -> {
          LOGGER.info(
            "Device {} with address {}", 
            e.getKey().toString(), 
            e.getValue().get(BLUEZ_DEVICE_IF).getOrDefault("Address", new Variant<String>("<null>")).toString()
          );
          return true;
        })
        .filter(e -> address.equalsIgnoreCase((String) e.getValue().get(BLUEZ_DEVICE_IF).get("Address").getValue()))
        .findFirst()
        .orElse(null);

    if (found != null) {
      LOGGER.info("Found device with address {} ...", address);

      for (Map.Entry<String, Variant<?>> prop : found.getValue().get(BLUEZ_DEVICE_IF).entrySet()) {
        LOGGER.info("\t property >> key: {}, value: {}", prop.getKey(), prop.getValue().toString());
      }
    }

    return found == null ? null : found.getKey();

  }

  private static DBusPath findAdapter(final ObjectManager om, final String pattern) {
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
