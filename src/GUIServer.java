import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;
/** 
 * @author yw79641760 
 * 
 * @func GUI for server 
 */
public class GUIServer extends Form implements CommandListener{
	BTChatter btChatter;
	BTServer btServer;
	TextField receiveTF;
	TextField sendTF;
	private Command SEND_COMMAND = new Command("������Ϣ",Command.SCREEN,1);
	private Command CANCEL_COMMAND = new Command("�˳�",Command.CANCEL,1);
	
	public GUIServer(BTChatter btChatter){
		super("�����������촰��");
		this.btChatter = btChatter;
		Ticker thicker = new Ticker("״̬����ʼ��");
		TextField receiveTF = new TextField("����","",50,TextField.UNEDITABLE);
		TextField sendTF = new TextField("����","",50,TextField.ANY);
		this.setTicker(thicker);
		this.append(receiveTF);
		this.append(sendTF);
		this.addCommand(SEND_COMMAND);
		this.addCommand(CANCEL_COMMAND);
		this.setCommandListener(this);
		if(btServer == null){
			 btServer = new BTServer(this);
		}
		btChatter.setCurrent(this);
	}

	public void commandAction(Command cmd, Displayable disp) {
		if(cmd == CANCEL_COMMAND){
			btServer.close();
			btChatter.setCurrent(btChatter.list);
		}
		if(cmd == SEND_COMMAND){
			btServer.send(btServer.sendText);
		}
	}
}
