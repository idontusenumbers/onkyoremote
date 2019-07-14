package net.obive.onkyoremote;

import de.csmp.jeiscp.EiscpConnector;
import de.csmp.jeiscp.EiscpDevice;
import de.csmp.jeiscp.eiscp.Command;
import de.csmp.jeiscp.eiscp.EiscpCommandsParser;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MacOnkyoRemote extends OnkyoRemote {
	private final Image icon;
	
	private EiscpDevice device;
	private Map<String, CheckboxMenuItem> inputMap;
	
	public MacOnkyoRemote(EiscpConnector connector) throws HeadlessException {
		super(connector);
		
		device = connector.getDevice();
		deviceCommandSets = device.getCapableEiscpParserModelsets();
		inputMap = new HashMap<>();
		
		
		List<Command> inputCommands = getInputCommands(EiscpCommandsParser.getCommandBlocks("main").stream().filter(c -> c.getCommand().equals("SLI")).findFirst().get());
		List<Command> inputZone2Commands = getInputCommands(EiscpCommandsParser.getCommandBlocks("zone2").stream().filter(c -> c.getCommand().equals("SLZ")).findFirst().get());
		List<Command> inputZone3Commands = getInputCommands(EiscpCommandsParser.getCommandBlocks("zone3").stream().filter(c -> c.getCommand().equals("SL3")).findFirst().get());
		
		
		final PopupMenu popup = new PopupMenu();
		final PopupMenu zone2 = new PopupMenu("Zone 2");
		final PopupMenu zone3 = new PopupMenu("Zone 3");
		
		final TrayIcon trayIcon =
				new TrayIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		final SystemTray tray = SystemTray.getSystemTray();
		
		MenuItem info = new MenuItem(device.getDeviceType());
		info.setEnabled(false);
		popup.add(info);
		
		wireUpPowerItems(popup, "PWR");
		wireUpPowerItems(zone2, "ZPW");
		wireUpPowerItems(zone3, "PW3");
		
		addInputs(connector, inputCommands, popup);
		addInputs(connector, inputZone2Commands, zone2);
		addInputs(connector, inputZone3Commands, zone3);
		
		popup.addSeparator();
		popup.add(zone2);
		popup.add(zone3);
		
		MenuItem quitItem = new MenuItem("Quit");
		quitItem.addActionListener(status -> System.exit(0));
		popup.addSeparator();
		
		popup.add(quitItem);
		
		popup.addActionListener(e -> System.out.println("popup action"));
		trayIcon.setPopupMenu(popup);
		trayIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				System.out.println("press");
			}
		});
		icon = getIcon("black");
		trayIcon.setImage(icon);
		connector.addListener(message -> {
			System.out.println(">>>" + message);
			if (message.startsWith("SLI") || message.startsWith("SLZ") || message.startsWith("SL3")) {
				inputMap.forEach((key, value) -> {
					if (key.startsWith(message.substring(0, 3)))
						value.setState(key.equals(message));
				});
			}
			
			
		});
		
		refreshStatus(connector);
		
		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			System.out.println("TrayIcon could not be added.");
		}
	}
	
	private void wireUpPowerItems(PopupMenu popup, String prefix) {
		MenuItem powerOn = new MenuItem("Turn on");
		MenuItem powerOff = new MenuItem("Turn off");
		
		powerOff.addActionListener(e -> sendCommand(prefix + "00"));
		powerOn.addActionListener(e -> sendCommand(prefix + "01"));
		
		connector.addListener(message -> togglePowerItems(popup, powerOn, powerOff, message, prefix));
		popup.add(powerOn);
		popup.add(powerOff);
		popup.addSeparator();
	}
	
	private void togglePowerItems(PopupMenu popup, MenuItem powerOn, MenuItem powerOff, String message, String commandPrefix) {
		if (message.equals(commandPrefix + "00")) {
			int i = getPopupIndex(popup, powerOff);
			popup.remove(powerOff);
			
			if (i != -1) {
				popup.insert(powerOn, i);
			}
		}
		if (message.equals(commandPrefix + "01")) {
			int i = getPopupIndex(popup, powerOn);
			popup.remove(powerOn);
			if (i != -1) {
				popup.insert(powerOff, i);
			}
		}
	}
	
	private int getPopupIndex(PopupMenu popup, MenuItem item) {
		for (int i = 0; i < popup.getItemCount(); i++) {
			MenuItem m = popup.getItem(i);
			if (m == item)
				return i;
		}
		return -1;
	}
	
	private void addInputs(EiscpConnector connector, List<Command> inputCommands, PopupMenu popup) {
		for (Command command : inputCommands) {
			String c = command.getCommand();
			if (Arrays.stream(new String[]{"UP", "DOWN", "QSTN", "27", "28", "2C"}).noneMatch(s -> s.equals(c))) {
				
				CheckboxMenuItem m = new CheckboxMenuItem(getInputName(command));
				String iscpCommand = command.getIscpCommand();
				inputMap.put(iscpCommand, m);
				m.addItemListener(e -> sendCommand(iscpCommand));
				m.addActionListener(e -> {
				});
				popup.add(m);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		//Check the SystemTray is supported
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		}
		
		EiscpConnector connector = EiscpConnector.autodiscover();
		
		new MacOnkyoRemote(connector);
	}
}