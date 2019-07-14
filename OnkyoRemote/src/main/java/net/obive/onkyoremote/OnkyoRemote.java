package net.obive.onkyoremote;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElementException;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.animation.AnimationElement;
import de.csmp.jeiscp.EiscpConnector;
import de.csmp.jeiscp.eiscp.Command;
import de.csmp.jeiscp.eiscp.CommandBlock;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OnkyoRemote extends JFrame {
	protected EiscpConnector connector;
	protected List<String> deviceCommandSets;
	
	public OnkyoRemote(EiscpConnector connector) {
		this.connector = connector;
	}
	
	protected void refreshStatus(EiscpConnector connector) {
		try {
			connector.sendIscpCommand("PWRQSTN");
			connector.sendIscpCommand("ZPWQSTN");
			connector.sendIscpCommand("PW3QSTN");
			connector.sendIscpCommand("SLIQSTN");
			connector.sendIscpCommand("SLZQSTN");
			connector.sendIscpCommand("SL3QSTN");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	protected List<Command> getInputCommands(CommandBlock inputCommandBlock) {
		return inputCommandBlock.getValues().stream()
				.filter(c -> deviceCommandSets.contains(c.getModels()))
				.filter(c -> c.getDescription().startsWith("sets "))
				.sorted(Comparator.comparing(this::getInputName))
				.collect(Collectors.toList());
	}
	
	protected void sendCommand(String iscpCommand) {
		try {
			connector.sendIscpCommand(iscpCommand);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	protected String getInputName(Command command) {
		return command.getDescription().substring(command.getDescription().lastIndexOf(" "));
	}
	
	
	protected Image getIcon(String color) {
		BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		try {
			
			SVGUniverse u = new SVGUniverse();
			URL icon = getClass().getResource("/icon.svg");
			u.loadSVG(icon);
			SVGDiagram diagram = u.getDiagram(icon.toURI());
			Graphics2D g2 = result.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			diagram.setDeviceViewport(new Rectangle(0, 0, 16, 16));
			diagram.getRoot().getChildren(null).forEach(e -> {
				try {
					e.addAttribute("fill", AnimationElement.AT_XML, color);
				} catch (SVGElementException e1) {
					e1.printStackTrace();
				}
			});
			diagram.render(g2);
			g2.dispose();
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (SVGException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
}
