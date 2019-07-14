package net.obive.onkyoremote;

import de.csmp.jeiscp.EiscpConnector;
import de.csmp.jeiscp.EiscpDevice;
import de.csmp.jeiscp.eiscp.Command;
import de.csmp.jeiscp.eiscp.EiscpCommandsParser;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SwingOnkyoRemote extends OnkyoRemote{
	private final Image icon;
	private final Image pressedIcon;
	
	private EiscpDevice device;
	private Map<String, JCheckBoxMenuItem> inputMap;
	
	public SwingOnkyoRemote(EiscpConnector connector) throws HeadlessException {
		super(connector);
		
		device = connector.getDevice();
		deviceCommandSets = device.getCapableEiscpParserModelsets();
		inputMap = new HashMap<>();
		
		
		List<Command> inputCommands = getInputCommands(EiscpCommandsParser.getCommandBlocks("main").stream().filter(c -> c.getCommand().equals("SLI")).findFirst().get());
		List<Command> inputZone2Commands = getInputCommands(EiscpCommandsParser.getCommandBlocks("zone2").stream().filter(c -> c.getCommand().equals("SLZ")).findFirst().get());
		List<Command> inputZone3Commands = getInputCommands(EiscpCommandsParser.getCommandBlocks("zone3").stream().filter(c -> c.getCommand().equals("SL3")).findFirst().get());
		
		
		final JPopupMenu popup = new JPopupMenu();
		final JMenu zone2 = new JMenu("Zone 2");
		final JMenu zone3 = new JMenu("Zone 3");
		
		final TrayIcon trayIcon =
				new TrayIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		final SystemTray tray = SystemTray.getSystemTray();
		
		JMenuItem info = new JMenuItem(device.getDeviceType());
		info.setEnabled(false);
		popup.add(info);
		
		wireUpPowerItems(popup::add, popup::addSeparator, "PWR");
		wireUpPowerItems(zone2::add, zone2::addSeparator, "ZPW");
		wireUpPowerItems(zone3::add, zone3::addSeparator, "PW3");
		
		addInputs(connector, inputCommands, popup::add);
		addInputs(connector, inputZone2Commands, zone2::add);
		addInputs(connector, inputZone3Commands, zone3::add);
		
		popup.addSeparator();
		popup.add(zone2);
		popup.add(zone3);
		
		JMenuItem quitItem = new JMenuItem("Quit");
		quitItem.addActionListener(status -> System.exit(0));
		popup.addSeparator();
		
		popup.add(quitItem);
		
		
		JFrame f = new JFrame();
		f.setUndecorated(true);
		f.setOpacity(0);
		f.setVisible(true);
		EventQueue.invokeLater(()->f.setVisible(false));
		popup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				trayIcon.setImage(icon);
				
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			
			}
		});
		//trayIcon.setPopupMenu(popup);
		trayIcon.addMouseListener(new MouseListener() {
			@Override
			public void mousePressed(MouseEvent e) {
				trayIcon.setImage(pressedIcon);
				System.out.println("mousePressed");
				
				
				f.setVisible(true);
				Point l = MouseInfo.getPointerInfo().getLocation();
				popup.show(f, l.x-16, 0);
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("mouseClicked");
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				System.out.println("mouseReleased");
				popup.setVisible(false);
				f.setVisible(false);
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				System.out.println("mouseEntered");
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				System.out.println("mouseExited");
			}
		});
//		trayIcon.addMouseMotionListener(new MouseMotionListener() {
//			@Override
//			public void mouseDragged(MouseEvent e) {
//				System.out.println("mouseDragged");
//
//			}
//
//			@Override
//			public void mouseMoved(MouseEvent e) {
//				System.out.println("mouseMoved");
//
//			}
//		});
		
		icon = getIcon("black");
		pressedIcon = getIcon("white");
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

	
	private void wireUpPowerItems(Consumer<JMenuItem> popupAdder, Runnable separatorAdder, String prefix) {
		JMenuItem powerOn = new JMenuItem("Turn on");
		JMenuItem powerOff = new JMenuItem("Turn off");
		
		powerOff.addActionListener(e -> sendCommand(prefix + "00"));
		powerOn.addActionListener(e -> sendCommand(prefix + "01"));
		
		connector.addListener(message -> togglePowerItems(powerOn, powerOff, message, prefix));
		popupAdder.accept(powerOn);
		popupAdder.accept(powerOff);
		separatorAdder.run();
	}
	
	private void togglePowerItems(JMenuItem powerOn, JMenuItem powerOff, String message, String commandPrefix) {
		if (message.equals(commandPrefix + "00")) {
			powerOn.setVisible(true);
			powerOff.setVisible(false);
		}
		if (message.equals(commandPrefix + "01")) {
			powerOn.setVisible(false);
			powerOff.setVisible(true);
		}
	}
	
	private int getPopupIndex(JPopupMenu popup, JMenuItem item) {
		for (int i = 0; i < popup.getSubElements().length; i++) {
			JMenuItem m = (JMenuItem) popup.getComponent(i);
			if (m == item)
				return i;
		}
		return -1;
	}
	
	private void addInputs(EiscpConnector connector, List<Command> inputCommands, Consumer<JMenuItem> menuAdder) {
		for (Command command : inputCommands) {
			String c = command.getCommand();
			if (Arrays.stream(new String[]{"UP", "DOWN", "QSTN", "27", "28", "2C"}).noneMatch(s -> s.equals(c))) {
				
				JCheckBoxMenuItem m = new JCheckBoxMenuItem(getInputName(command));
				String iscpCommand = command.getIscpCommand();
				inputMap.put(iscpCommand, m);
				m.addActionListener(e -> sendCommand(iscpCommand));
				menuAdder.accept(m);
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
		
		new SwingOnkyoRemote(connector);
	}
}
