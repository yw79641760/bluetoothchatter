import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
/** 
 * @author yw79641760 
 * @version 0.0.1 
 * @MIDlet 
 */
public class BTChatter extends MIDlet implements CommandListener{
	
	GUIServer guiServer;
	GUIClient guiClient;
	private Display display;
	List list;
	private Command OK_COMMAND = new Command("确定",Command.OK,1);
	private Command EXIT_COMMAND = new Command("退出",Command.EXIT,1);
	static final String[] menuList = { "服务器端","客户端"};	

	protected void destroyApp(boolean uncinditional) throws MIDletStateChangeException {
	}

	protected void pauseApp() {
	}

	protected void startApp() throws MIDletStateChangeException {
		if (display ==null){
			display = Display.getDisplay(this);
			list = new List("主菜单",List.IMPLICIT,menuList,null);
			list.addCommand(OK_COMMAND);
			list.addCommand(EXIT_COMMAND);
			list.setCommandListener(this);
			display.setCurrent(list);
		}
		display.setCurrent(list);
	}

	public void commandAction(Command cmd, Displayable disp) {
		if(cmd == EXIT_COMMAND){
			try {
				destroyApp(true);
			} catch (MIDletStateChangeException e) {
				e.printStackTrace();
			}
			notifyDestroyed();
		}else if(list.getSelectedIndex() == 0){//服务器端
			guiServer = new GUIServer(this);
			display.setCurrent(guiServer);
		}else if(list.getSelectedIndex() == 1){//客户端
			guiClient = new GUIClient(this);
			display.setCurrent(guiClient);
		}else
			System.err.println("Unexpected Choice...");
	}
	protected void setCurrent(Displayable disp){
		display.setCurrent(disp);
	}
//	protected void displayError(String message, Displayable displayable){
//		Alert alert = new Alert("错误提示");
//		alert.setType(AlertType.ERROR);
//		alert.setString(message);
//		alert.setTimeout(2000);
//		display.setCurrent(alert, displayable);
//	}
}
