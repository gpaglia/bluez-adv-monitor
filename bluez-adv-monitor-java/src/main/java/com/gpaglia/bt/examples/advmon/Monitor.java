package com.gpaglia.bt.examples.advmon;

import static com.gpaglia.bt.examples.advmon.Commons.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Monitor implements AdvertisementMonitor1, Properties {
  private static final Logger LOGGER = LoggerFactory.getLogger(Monitor.class);

  private final DBusPath objectPath;
  private final Map<String, Variant<?>> properties;
  private final DBusConnection connection;

  public Monitor(final DBusConnection connection, final String objectPath, final List<AdFilter> filters) {
    if (filters.isEmpty()) {
      throw new IllegalArgumentException("Filters cannot be empty");
    }
    this.connection = connection;
    this.objectPath = new DBusPath(objectPath);
    this.properties = new HashMap<>();
    this.properties.put("Type", new Variant<String>("or_patterns"));
    for (AdFilter filter: filters) {
      this.properties.put("Patterns", new Variant<>(List.of(filter.getPosition(), filter.getAdType(), filter.getAdData()), "yyay"));
    }
  }

  // local public methods
  public DBusPath getDBusPath() { return objectPath; }

  // methods from dbus interfaces

  @Override
  public boolean isRemote() { return false; }

  @Override
  public String getObjectPath() { return objectPath.getPath(); }

  @Override
  @SuppressWarnings("unchecked")
  public <A> A Get(String interfaceName, String propertyName) {
    if (interfaceName.equals(ADV_MONITOR_IFACE) && properties.containsKey(propertyName)) {
      return  (A) properties.get(propertyName);
    } else {
      throw new DBusExecutionException("Invalid interface or unknown property " + interfaceName + ":" + propertyName);
    }
  }

  @Override
  public Map<String, Variant<?>> GetAll(String interfaceName) {
    return Collections.unmodifiableMap(properties);
  }

  @Override
  public <A> void Set(String interfaceName, String propertyName, A value) {
    throw new DBusExecutionException("Properties cannot be written to");      
  }

  @Override
  public void Release() {
    LOGGER.info("Monitor {} released", objectPath);
  }

  @Override
  public void Activate() {
    LOGGER.info("Monitor {} acivated", objectPath);
  }

  @Override
  public void DeviceFound(DBusInterface device) {
    LOGGER.info("Device {} was found", device.getObjectPath());
  }

  @Override
  public void DeviceLost(DBusInterface device) {
    LOGGER.info("Device {} was lost", device.getObjectPath());
  }

}