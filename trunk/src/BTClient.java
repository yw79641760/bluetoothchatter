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
	private LocalDevice localDevice;//�����豸ʵ��
	String localBTAddress;//����������ַ
	String localBTName;//������������
	GUIClient guiClient;
	DiscoveryAgent discoveryAgent;//���ִ���
	Thread thread;
	Thread receiveTh;
	Thread sendTh;
	StreamConnection conn;
	boolean exitFlag;
	boolean BTReady;
	DataInputStream dis;
	DataOutputStream dos;
	String sendText = "";
	Hashtable remoteDevices = new Hashtable();//�洢�ҵ���Զ���豸
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
			uuid = new UUID(uuidString, false);//���ص�UUID
			localDevice = LocalDevice.getLocalDevice();//ȡ�ñ����豸ʵ��
			localBTAddress = localDevice.getBluetoothAddress();//��¼������ַ
			localBTName = localDevice.getFriendlyName();//��¼��������
			localDevice.setDiscoverable(DiscoveryAgent.GIAC);
			discoveryAgent = localDevice.getDiscoveryAgent();//ȡ����������
			isSucceed = true;
		} catch (BluetoothStateException e) {
			guiClient.getTicker().setString("��ʼ�������豸ʧ��:" + e.toString());
			System.out.println(e.toString());
		}
		return isSucceed;
	}
	//�����豸
	public void search() {
		try {
			//���remoteDevices
			remoteDevices.clear();
			//������ĺ���֪�������豸����remoteDevices
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
			//����֪���豸�ϲ�ѯ����
			searchService(remoteDevices);
			if (serviceRecord != null)//�ҵ�����
				return;
			discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);//�����豸
			searchService(remoteDevices);
			remoteDevices.clear();
		} catch (BluetoothStateException e) {
			guiClient.getTicker().setString("���������豸ʧ��" + e.toString());
			System.out.println(e.toString());
		}
		if (serviceRecord != null) {
			this.notify();
		} else {
			guiClient.getTicker().setString("δ�����豸�����");
		}
	}
	//��������
	private void searchService(Hashtable remotes)
			throws BluetoothStateException {
		UUID[] UUIDs = new UUID[2];
		UUIDs[0] = new UUID(0x0003);//����֧��RFCOMM
		UUIDs[1] = new UUID(uuidString, false);
		//ȡ��ÿһ���豸��ѯ
		for (Enumeration e = remotes.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			RemoteDevice remoteDevice = (RemoteDevice) remotes.get(key);
			//��ѯ
			discoveryAgent.searchServices(null, UUIDs, remoteDevice, this);
		}
	}

	public void run() {
		if (!initBT()) {
			guiClient.getTicker().setString("��ʼ��ʧ��");
			return;
		}
		try {
			guiClient.getTicker().setString("��ʼ����");
			//�ȴ�������������
			synchronized (this) {
				this.wait();
			}
			if (exitFlag)
				return;
				search();
				//�ȴ�URL׼����
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
				guiClient.getTicker().setString("׼������");
				BTReady = true;
		} catch (InterruptedException e) {
			guiClient.getTicker().setString("��ʼ��ʧ��");
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
	//��¼�ҵ����豸
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
		//���ָ���Ȥ�ķ�������ֱ��ʹ�õ�һ��
		if (servRecord == null || servRecord.length == 0) {
			url = null;
			serviceRecord = null;
			return;
		}
		serviceRecord = servRecord[0];
		//ȡ�ø���Ȥ������URL
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
				guiClient.getTicker().setString("��ȡ����ʧ��");
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
						guiClient.getTicker().setString("д����ʧ��");
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
