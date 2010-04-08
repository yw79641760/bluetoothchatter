import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
/** 
 * @author yw79641760 
 *  
 * @func Bluetooth Utility for server  
 */
public class BTServer implements Runnable{
	
	public static final String uuidString = "F0E0D0C0B0A000908070605040302010";
	public static UUID uuid;
	private LocalDevice localDevice;//�����豸ʵ��
	String localBTAddress;//����������ַ
	String localBTName;//������������
	DiscoveryAgent agent;
	GUIServer guiServer;
	Thread thread;
	Thread receiveTh;//��ȡ���ݵ��߳�
	Thread sendTh;//д�����ݵ��߳�
	private StreamConnectionNotifier notifier;
	StreamConnection conn;
	boolean exitFlag;
	boolean BTReady;
	DataInputStream dis;
	DataOutputStream dos;
	String sendText = "";
	
	public BTServer(GUIServer server) {
		this.guiServer = server;
		thread = new Thread(this);
		thread.start();
	}
	//��ʼ�������豸
	public boolean initBT(){
		boolean isSucceed = false;
		uuid = new UUID(uuidString, false);
		try {
			//ȡ�ñ����豸ʵ��
			localDevice = LocalDevice.getLocalDevice();
			//���÷������ɷ��֣����ɹ��򷵻�
			if(!localDevice.setDiscoverable(DiscoveryAgent.GIAC)){
				return false;
			}
		} catch (BluetoothStateException e) {
			guiServer.getTicker().setString(e.toString());
			System.out.println(e.toString());
		}
		localBTAddress = localDevice.getBluetoothAddress();//��¼������ַ
		localBTName = localDevice.getFriendlyName();//��¼��������
		agent = localDevice.getDiscoveryAgent();//ȡ����������
		isSucceed = true;
		return isSucceed;		
	}
	public void run() {
		if(!initBT()){
			guiServer.getTicker().setString("��ʼ��ʧ��");
			return;
		}
		StringBuffer url = new StringBuffer("btspp://");
		url.append("localhost").append(':');
		url.append(uuid.toString());
		url.append(";name=p2pChatServer");
		url.append(";authorize=false");
		try {//�������������
			notifier = (StreamConnectionNotifier)Connector.open(url.toString());
			guiServer.getTicker().setString("�ȴ��ͻ�������");
			conn = notifier.acceptAndOpen();//�ȴ��û����ӣ�������ӳɹ��������������
			dis = conn.openDataInputStream();
			dos = conn.openDataOutputStream();
			receiveTh = new ReceiveThread();
			receiveTh.start();
			sendTh = new SendThread();
			sendTh.start();
			guiServer.getTicker().setString("����");
			BTReady = true;
		} catch (IOException e) {
			guiServer.getTicker().setString("��ʼ��ʧ��");
			System.out.println(e.toString());
		}catch (SecurityException e){
			guiServer.getTicker().setString("��ʼ��ʧ��");
			System.out.println(e.toString());
		}
//		thread = null;
	}
	//�ر����Ӻ��߳�
	public void close(){
		exitFlag = true;
		if(sendTh != null){
			synchronized(sendTh){
				sendTh.notify();
			}
		}
		try {
			if(dis != null){
				dis.close();
			}
			if(dos != null){
				dos.close();
			}
			if(conn != null){
				conn.close();
			}
			if(receiveTh != null){
				receiveTh.join();
			}
			if(sendTh != null){
				sendTh.join();
			}
			if(thread != null){
				thread.join();
			}
		} catch (IOException e) {
			System.out.println(e.toString());
		} catch (InterruptedException e) {
			System.out.println(e.toString());
		}
	}
	//��������
	public void send(String str){
		if(sendTh == null)
			return;
		sendText = str;
		synchronized(sendTh){
			sendTh.notify();
		}
	}
	class ReceiveThread extends Thread{
		public void run(){
			try {
				while(!exitFlag){
					String str = dis.readUTF();
					if(str != null){
						guiServer.receiveTF.setString(str);
					}
				}
			} catch (IOException e) {
				if(!exitFlag){
					guiServer.getTicker().setString("��ȡ����ʧ��");
					System.out.println(e.toString());
				}
			}
		}
	}
	class SendThread extends Thread{
		public void run(){
			try {
				while(!exitFlag){
					synchronized(this){
						try {
							wait();
						} catch (InterruptedException e) {
							System.out.println(e.toString());
						}
						if(exitFlag){//������Ϊ�رղ��������
							break;
						}
						if(sendTh != null){
								dos.writeUTF(sendText);
						}
					}
				}
			} catch (IOException e) {
				if(!exitFlag){
					guiServer.getTicker().setString("д����ʧ��");
				}
			}
		}
	}
}
