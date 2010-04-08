import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/**
 * @author yw79641760
 * 
 * @func   Bluetooth Utility for client
 */
public class BTClient implements DiscoveryListener, Runnable {
	public static final String uuidString = "F0E0D0C0B0A000908070605040302010";
	public static UUID uuid;
	private LocalDevice localDevice;//本地设备实例
	String localBTAddress;//本地蓝牙地址
	String localBTName;//本地蓝牙名称
	GUIClient guiClient;
	DiscoveryAgent discoveryAgent;//发现代理
	Thread thread;
	Thread receiveTh;
	Thread sendTh;
	StreamConnection conn;
	boolean exitFlag;
	boolean BTReady;
	DataInputStream dis;
	DataOutputStream dos;
	String sendText = "";
	Hashtable remoteDevices = new Hashtable();//存储找到的远程设备
	String url = "";
	ServiceRecord serviceRecord;

	public BTClient(GUIClient client) {
		this.guiClient = client;
		thread = new Thread(this);
		thread.start();
	}

	public boolean initBT() {
		boolean isSucceed = false;
		try {
			uuid = new UUID(uuidString, false);//本地的UUID
			localDevice = LocalDevice.getLocalDevice();//取得本地设备实例
			localBTAddress = localDevice.getBluetoothAddress();//记录蓝牙地址
			localBTName = localDevice.getFriendlyName();//记录蓝牙名称
			localDevice.setDiscoverable(DiscoveryAgent.GIAC);
			discoveryAgent = localDevice.getDiscoveryAgent();//取得蓝牙代理
			isSucceed = true;
		} catch (BluetoothStateException e) {
			guiClient.getTicker().setString("初始化蓝牙设备失败:" + e.toString());
			System.out.println(e.toString());
		}
		return isSucceed;
	}
	//搜索设备
	public void search() {
		try {
			//清除remoteDevices
			remoteDevices.clear();
			//将缓存的和已知的蓝牙设备加入remoteDevices
			RemoteDevice[] cacheDevices = discoveryAgent.retrieveDevices(DiscoveryAgent.CACHED);
			if (cacheDevices != null) {
				for (int i = 0; i < cacheDevices.length; i++) {
					remoteDevices.put(cacheDevices[i].getBluetoothAddress(),cacheDevices[i]);
				}
			}
			RemoteDevice[] preDevices = discoveryAgent.retrieveDevices(DiscoveryAgent.PREKNOWN);
			if (preDevices != null) {
				for (int i = 0; i < preDevices.length; i++) {
					remoteDevices.put(cacheDevices[i].getBluetoothAddress(),cacheDevices[i]);
				}
			}
			//在已知的设备上查询服务
			searchService(remoteDevices);
			if (serviceRecord != null)//找到返回
				return;
			discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);//搜索设备
			searchService(remoteDevices);
			remoteDevices.clear();
		} catch (BluetoothStateException e) {
			guiClient.getTicker().setString("搜索蓝牙设备失败" + e.toString());
			System.out.println(e.toString());
		}
		if (serviceRecord != null) {
			this.notify();
		} else {
			guiClient.getTicker().setString("未发现设备或服务");
		}
	}
	//搜索服务
	private void searchService(Hashtable remotes)
			throws BluetoothStateException {
		UUID[] UUIDs = new UUID[2];
		UUIDs[0] = new UUID(0x0003);//必须支持RFCOMM
		UUIDs[1] = new UUID(uuidString, false);
		//取出每一个设备查询
		for (Enumeration e = remotes.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			RemoteDevice remoteDevice = (RemoteDevice) remotes.get(key);
			//查询
			discoveryAgent.searchServices(null, UUIDs, remoteDevice, this);
		}
	}

	public void run() {
		if (!initBT()) {
			guiClient.getTicker().setString("初始化失败");
			return;
		}
		try {
			guiClient.getTicker().setString("开始查找");
			//等待启动服务搜索
			synchronized (this) {
				this.wait();
			}
			if (exitFlag)
				return;
				search();
				//等待URL准备好
				synchronized (this) {
					wait();
				}
				if (exitFlag)
					return;
				try {
					conn = (StreamConnection) Connector.open(url);
					dis = conn.openDataInputStream();
					dos = conn.openDataOutputStream();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Thread receiveTh = new ReceiveThread();
				receiveTh.start();
				Thread sendTh = new SendThread();
				sendTh.start();
				guiClient.getTicker().setString("准备就绪");
				BTReady = true;
		} catch (InterruptedException e) {
			guiClient.getTicker().setString("初始化失败");
			System.out.println(e.toString());
			return;
		}
		thread = null;
	}

	public void close() {
		try {
			exitFlag = true;
			synchronized (this) {
				this.notify();
			}
			if (sendTh != null) {
				synchronized (sendTh) {
					sendTh.notify();
				}
			}
			if (dis != null) {
				dis.close();
			}
			if (dos != null) {
				dos.close();
			}
			if (conn != null) {
				conn.close();
			}
			if (receiveTh != null) {
				receiveTh.join();
			}
			if (sendTh != null) {
				sendTh.join();
			}
			if (thread != null) {
				thread.join();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	//记录找到的设备
	public void deviceDiscovered(RemoteDevice device, DeviceClass cod) {
		remoteDevices.put(device.getBluetoothAddress(), device);
	}

	public void inquiryCompleted(int discType) {
		try {
			searchService(remoteDevices);
		} catch (BluetoothStateException e) {
			guiClient.getTicker().setString(e.toString());
			System.out.println(e.toString());
		}
		remoteDevices.clear();
	}

	public void serviceSearchCompleted(int arg0, int arg1) {
		synchronized (this) {
			this.notifyAll();
		}
	}

	public void servicesDiscovered(int tranID, ServiceRecord[] servRecord) {
		//发现感兴趣的服务，这里直接使用第一个
		if (servRecord == null || servRecord.length == 0) {
			url = null;
			serviceRecord = null;
			return;
		}
		serviceRecord = servRecord[0];
		//取得感兴趣的连接URL
		url = serviceRecord.getConnectionURL(
				ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
	}

	public void send(String str) {
		if (sendTh == null) {
			return;
		}
		sendText = str;
		synchronized (sendTh) {
			sendTh.notify();
		}
	}

	class ReceiveThread extends Thread {
		public void run() {
			try {
				while (!exitFlag) {
					String str = dis.readUTF();
					if (str != null) {
						guiClient.receiveTF.setString(str);
					}
				}
			} catch (IOException e) {
				guiClient.getTicker().setString("读取数据失败");
				System.out.println(e.toString());
			}
		}
	}

	class SendThread extends Thread {
		public void run() {
			while (!exitFlag) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						System.out.println(e.toString());
					}
				}
				if (exitFlag)
					break;
				if (sendText != null)
					try {
						dos.writeUTF(sendText);
					} catch (IOException e) {
						guiClient.getTicker().setString("写数据失败");
						System.out.println(e.toString());
					}
			}
		}
	}

	public void startSearch() {
		synchronized (this) {
			this.notifyAll();
		}
	}
}
