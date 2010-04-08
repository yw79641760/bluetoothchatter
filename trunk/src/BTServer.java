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
	private LocalDevice localDevice;//本地设备实例
	String localBTAddress;//本地蓝牙地址
	String localBTName;//本地蓝牙名称
	DiscoveryAgent agent;
	GUIServer guiServer;
	Thread thread;
	Thread receiveTh;//读取数据的线程
	Thread sendTh;//写入数据的线程
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
	//初始化蓝牙设备
	public boolean initBT(){
		boolean isSucceed = false;
		uuid = new UUID(uuidString, false);
		try {
			//取得本地设备实例
			localDevice = LocalDevice.getLocalDevice();
			//设置服务器可发现，不成功则返回
			if(!localDevice.setDiscoverable(DiscoveryAgent.GIAC)){
				return false;
			}
		} catch (BluetoothStateException e) {
			guiServer.getTicker().setString(e.toString());
			System.out.println(e.toString());
		}
		localBTAddress = localDevice.getBluetoothAddress();//记录蓝牙地址
		localBTName = localDevice.getFriendlyName();//记录蓝牙名称
		agent = localDevice.getDiscoveryAgent();//取得蓝牙代理
		isSucceed = true;
		return isSucceed;		
	}
	public void run() {
		if(!initBT()){
			guiServer.getTicker().setString("初始化失败");
			return;
		}
		StringBuffer url = new StringBuffer("btspp://");
		url.append("localhost").append(':');
		url.append(uuid.toString());
		url.append(";name=p2pChatServer");
		url.append(";authorize=false");
		try {//建立服务端连接
			notifier = (StreamConnectionNotifier)Connector.open(url.toString());
			guiServer.getTicker().setString("等待客户端连接");
			conn = notifier.acceptAndOpen();//等待用户连接，如果连接成功则打开输入和输出流
			dis = conn.openDataInputStream();
			dos = conn.openDataOutputStream();
			receiveTh = new ReceiveThread();
			receiveTh.start();
			sendTh = new SendThread();
			sendTh.start();
			guiServer.getTicker().setString("就绪");
			BTReady = true;
		} catch (IOException e) {
			guiServer.getTicker().setString("初始化失败");
			System.out.println(e.toString());
		}catch (SecurityException e){
			guiServer.getTicker().setString("初始化失败");
			System.out.println(e.toString());
		}
//		thread = null;
	}
	//关闭连接和线程
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
	//发送数据
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
					guiServer.getTicker().setString("读取数据失败");
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
						if(exitFlag){//可能以为关闭操作被打断
							break;
						}
						if(sendTh != null){
								dos.writeUTF(sendText);
						}
					}
				}
			} catch (IOException e) {
				if(!exitFlag){
					guiServer.getTicker().setString("写数据失败");
				}
			}
		}
	}
}
