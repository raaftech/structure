/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

import org.openpcf.universal.*;

import java.awt.*;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.image.BufferedImage;

/**
 * The main class and method, possibly referenced in Ant or Maven.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class TestComponentInstancing {
    public static void testInstance() {

        String name = "foohost";
        Image icon = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        String vendor = "Some Corp";
        String model = "Master 9000";
        String revision = "REV1.0";


        NetworkDevice networkDeviceForOS = new NetworkDevice("eth0", icon, "Another Corp", "Ethernet 53 Card", "REV1.0", "00:34:52:C0:FF:EE");
        Listener listenerForSSHonOS = new Listener("10.10.10.1", 22);
        Service servicesForOS = new TextualAccessor("this-openssh", icon, "OpenSSH", "5.0", "/usr/sbin", "sshd", listenerForSSHonOS);

        StorageDevice storageDevice = new StorageDevice("hd0", icon, "Hitachi", "Deathstar", "5000", 4932049);
        MemoryModule memoryModule = new MemoryModule("dimm0", icon, "Kingston", "PC4500-333", "REV0.1", 2349823);
        ProcessingUnit processingUnit = new ProcessingUnit("cpu0", icon, "Intel", "Xeon 5600", "REV1", 5000, 3072, 4);
        org.openpcf.universal.GraphicsDevice graphicsDevice = new org.openpcf.universal.GraphicsDevice("gfx0", icon, "NVIDIA", "GTX8800", "REV4", 2300, 1333, 8192, 32489238);

        Configuration configuredStuffOnComputer = new ConfiguredNetworkDevice("configged-card", networkDeviceForOS, "1.2.3.4");
        OperatingSystem osForComputer = new OperatingSystem("myos", icon, "Canonical", "Ubuntu Linux 4", Boolean.FALSE, servicesForOS, configuredStuffOnComputer);


        NetworkDevice networkDevicesForConsole = new NetworkDevice("x1", icon, "YET Another Corp", "Ethernet q0e0 Card", "REV1.0", "00:34:52:C0:FF:EF");
        Listener listenerForSSHonConsole = new Listener("10.10.10.2", 22);
        Service servicesOnConsole = new TextualAccessor("some-embedded-ssh", icon, "MehSSH", "1.0", "/sbin", "sshd", listenerForSSHonConsole);

        Configuration configuredStuffOnConsole = new ConfiguredNetworkDevice("another-configged-card", networkDevicesForConsole, "1.2.3.5");
        OperatingSystem osForManageDev = new OperatingSystem("some-ilo-os", icon, "Embedded Somthing", "Probably Loenix", Boolean.FALSE, servicesOnConsole, configuredStuffOnConsole);

        ManagementConsole managementConsole = new ManagementConsole("ilo0", icon, "HP", "iLO", "5", networkDevicesForConsole, osForManageDev);

        org.openpcf.universal.Component foo = new ComputerSystem(name, icon, vendor, model, revision, networkDeviceForOS, osForComputer, storageDevice, memoryModule, processingUnit, graphicsDevice, managementConsole);

        System.out.println(foo);
    }
}

