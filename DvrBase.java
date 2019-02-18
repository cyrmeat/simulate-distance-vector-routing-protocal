import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DvrBase {
	
	private int numberNeighbours;
	private List<Neighbour> neighbourTable = new ArrayList<Neighbour>();
	private List<DV> DVTable = new ArrayList<DV>();
	private static String hostID;
	private InetAddress hostIp = InetAddress.getLocalHost();
	private DatagramSocket serverSocket;
	private DatagramPacket sendPacket;
//	private byte[] sendData;
	
	public static void main(String args[]) throws Exception {
		
		hostID = args[0];
		int serverPort = Integer.parseInt(args[1]);
		String configTxt = args[2];
		boolean pr = false;
		if (args.length > 3){
			pr = true;
		}				
		
		// read neighbours from config.txt
//		readFile(configTxt);
		
		// create a Server object, which will automatically begin to accept connection
		new DvrBase(serverPort, configTxt);
	}
	
	private DvrBase(int serverPort, String filename) throws IOException {
		server(serverPort, filename);
	}
	
	private void server(int serverPort, String filename) throws IOException {
		// read neighbours from config.txt
		readFile(filename);
		
		// crate datagram socket from port 'servrePort'
		serverSocket = new DatagramSocket(serverPort);
		
		// crate receive and send data buffer
//		byte[] receiveData = new byte[1024];
/*		sendData = new byte[1024];
		
		// combine sendData
		String sendStr = hostID;
		for (int i = 0; i < DVTable.size();){
			sendStr = sendStr + " " + DVTable.get(i).getNodeId() + " " + DVTable.get(i).getDistance() + " " + DVTable.get(i).getNextHop();
			i = i + 1;
		}
		sendData = sendStr.getBytes(); */
//	    sendPacket = new DatagramPacket(sendData, sendData.length, hostIp, serverPort);
		
	    // send my DV table to neighbours every 5 seconds
	    TimerTask task = new sendTimer();
	    Calendar  calendar= Calendar.getInstance();                  
        Date firstTime = calendar.getTime();
        Timer timer = new Timer();
        timer.schedule(task, firstTime, 5000);
//	    for (int i = 0; i < neighbourTable.size();) {
//	    	serverSocket.send(sendPacket);
//	    	System.out.println("send");
//	    }
	    
	    // print the solution after 1 minutes
        TimerTask task2 = new printTimer();
        Calendar  calendar2= Calendar.getInstance();                  
        Date firstTime2 = calendar.getTime();
        Timer timer2 = new Timer();
        timer.schedule(task2, 60000);
        
        
        
	    // give a thread to every new received DV table
		while (true) {
			
			// create space for received datagram
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			// receive datagram
			serverSocket.receive(receivePacket);
			System.out.println(receiveData.length);
			System.out.println(new String(receiveData));
			
			
			// create a new thread for this receivePacket, and then forget about it
		    ReceiveThread receiveThread = new ReceiveThread(receivePacket);
		    Thread t = new Thread(receiveThread);
            t.start();
			
//			String sentence = new String(receivePacket.getData());
//			InetAddress IPAddress = receivePacket.getAddress();
//			int port = receivePacket.getPort();
//			System.out.println("connection from " + port);
		}
	}
	
	
	// Timer for send DV table
	public class sendTimer extends TimerTask {

		@Override
		public void run() {
//			System.out.println("send");
			byte[] sendData = new byte[1024];
			
			// combine sendData
			String sendStr = hostID;
			for (int i = 0; i < DVTable.size();){
				sendStr = sendStr + " " + DVTable.get(i).getNodeId() + " " + DVTable.get(i).getDistance() + " " + DVTable.get(i).getNextHop();
				i = i + 1;
			}
			sendData = sendStr.getBytes();
			
			for (int i = 0; i < neighbourTable.size();) {
		    	try {
		    		int neighbourPort = neighbourTable.get(i).getPort();
		    		sendPacket = new DatagramPacket(sendData, sendData.length, hostIp, neighbourPort);
					serverSocket.send(sendPacket);
					System.out.println("send " + neighbourPort);
					System.out.println("send " + sendStr);
				} catch (IOException e) {
					e.printStackTrace();
				}
		    	i = i + 1;
		    }
//			System.out.println("send");
		}	
	}
	
	// Timer for print solution
	public class printTimer extends TimerTask {
		
		public void run() {
			for (int i = 0; i < DVTable.size(); i++) {
				if (DVTable.get(i).getNextHop() == null){
					String node = DVTable.get(i).getNodeId();
					float distance = DVTable.get(i).getDistance();
					DecimalFormat df1 = new DecimalFormat("####.0"); 
					System.out.println("shortest path to node " + node +": the next hop is "+ node + " and the cost is "+ df1.format(distance));
				}
				else{
					String node = DVTable.get(i).getNodeId();
					float distance = DVTable.get(i).getDistance();
					String hop = DVTable.get(i).getNextHop();
					DecimalFormat df1 = new DecimalFormat("####.0"); 
					System.out.println("shortest path to node " + node + ": the next hop is "+ hop+" and the cost is "+ df1.format(distance));
				}
			}
		}
	}
	
	// create thread class to have multiple receive threads
	public class ReceiveThread implements Runnable {
		private List<DV> receiveDVTable = new ArrayList<DV>();
		private DatagramPacket receivePacket;
		private String receiveID;
		
		// constructor of ReceiveThread
		public ReceiveThread(){}
		public ReceiveThread(DatagramPacket receivePacket) throws IOException {  
        	this.receivePacket = receivePacket;
        }
		
		// read  received datagram packet
		private void extractData() {
			String sentence = new String(receivePacket.getData());
			String[] content = sentence.split(" ");
			receiveID = content[0];
			for (int i = 1; i < content.length;){
				DV node = new DV(content[i], Float.parseFloat(content[i+1]), content[i+2]);
				receiveDVTable.add(node);
				i = i + 3;
			}
			
			// this is used for test program
			int port = receivePacket.getPort();
			System.out.println("connection from " + port);				
		}
		
		// according to received DV table to update host's DV table
		private void modifyDV() {
			
			// get the distance between host and received node
			float distanceHostToReceive = 0;
			for (int i = 0; i < DVTable.size();){
//				System.out.println("第一个: " + DVTable.get(i).getNodeId());
				if (DVTable.get(i).getNodeId().equals(receiveID)) {
					distanceHostToReceive = DVTable.get(i).getDistance();
					i = i + 1;
				}
				else {
					i = i + 1;
				}
				//i = i + 1;
			}
			System.out.print("distanceHostToReceive= " + distanceHostToReceive);
			
			// update exist node and add new node to the list
			
			for (int i = 0; i < receiveDVTable.size();) {
				String node1 = receiveDVTable.get(i).getNodeId();
				System.out.println("他表的: " + i + receiveDVTable.get(i).getNodeId());
				if (! node1.equals(hostID)) {
					float distance1 = receiveDVTable.get(i).getDistance();
					float newDistance = distance1 + distanceHostToReceive;
					int count = 0;
					for (int j = 0; j < DVTable.size();) {
						System.out.println("我表的: " + j + DVTable.get(j).getNodeId());
						String node2 = DVTable.get(j).getNodeId();
						float distance2 =  DVTable.get(j).getDistance();
						if ((!node2.equals(receiveID)) && node1.equals(node2) && (newDistance < distance2)&& (count == 0)) {
							DVTable.get(j).setDistance(newDistance);
							DVTable.get(j).setNextHop(receiveID);
							count = count + 1;
							j = j + 1;
						}
						else if ((!node2.equals(receiveID)) && node1.equals(node2) && (newDistance >= distance2)&& (count == 0)) {
							count = count + 1;
							j = j + 1;
						}
						else if ((!node2.equals(receiveID)) && (j == (DVTable.size()-1)) && (count == 0)){							
							DV node = new DV();
							node.setNodeId(node1);
							node.setDistance(distance1+distanceHostToReceive);
							node.setNextHop(receiveID);
							DVTable.add(node);
							j = j + 1;
						}
						else{
							j = j + 1;}
						//j = j + 1;
					}
					i = i + 1;
				}
				else {
					i = i + 1;}
				//i = i + 1;
			}
			
/*			for (int i = 0; i < DVTable.size();) {
				if (DVTable.get(i).getNodeId() != receiveID) {
					String node1 = DVTable.get(i).getNodeId();
					int distance1 = DVTable.get(i).getDistance();
					
					for (int j = 0; j < receiveDVTable.size();) {
						String node2 = receiveDVTable.get(j).getNodeId();
						if (node2 != hostID && node1 == node2) {
							int distance2 = receiveDVTable.get(j).getDistance();
							int newDistance = distance2 + distanceHostToReceive;
							if (newDistance < distance1) {
								DVTable.get(i).setDistance(newDistance);
								DVTable.get(i).setNextHop(receiveID);
							}
						}
						j = j + 1;
					}
				}
				i = i + 1;
			}
*/		}

		@Override
		public void run() {
			
			extractData();
			
			modifyDV();
			String sendStr = "";
			for (int i = 0; i < DVTable.size();){
				sendStr = sendStr+" " + DVTable.get(i).getNodeId() + " " + DVTable.get(i).getDistance() + " " + DVTable.get(i).getNextHop();
				i = i + 1;
			}
			System.out.println("new = " + sendStr);
		}
	}
	
	private void readFile(String fileName) {
 
		try {
	        BufferedReader br = new BufferedReader(new FileReader(fileName));                                      
	        String lineTxt = null; 
	        lineTxt = br.readLine();
	        numberNeighbours = Integer.parseInt(lineTxt);
	        while ((lineTxt = br.readLine()) != null) {
	            String[] info = lineTxt.split(" ");
	            for (int i = 0 ; i < info.length;) {
	            	Neighbour nextOne = new Neighbour(info[i], Float.parseFloat(info[i+1]), Integer.parseInt(info[i+2]));
	                neighbourTable.add(nextOne);
	                DV dvNode = new DV(info[i], Integer.parseInt(info[i+1]), null);
	                DVTable.add(dvNode);
	                System.out.println(info[i] + " " + info[i+1] + " " + info[i+2]);
	                i = i + 3;
	                }
	            }
	        br.close();
		}
	    catch (Exception e) {
	        System.err.println("read errors :" + e);
	    }
	}
	
	
	public class Neighbour {
		String nodeId;
		float distance;
		int port;
		
		public Neighbour() {};
		public Neighbour(String nodeId, float distance, int port) {
			this.nodeId = nodeId;
			this.distance = distance;
			this.port = port;
		}
		
		private void setNodeId(String Id) {
			nodeId = Id;
		}
		
		private String getNodeId() {
			return nodeId;
		}
		
		private void setDistance(float distance1){
			distance = distance1;
		}
		
		private Float  getDistance() {
			return distance;
		}
		
		private void setPort(int port1) {
			port = port1;
		}
		
		private Integer getPort() {
			return port;
		}
	}
	
	public class DV {
		String nodeId;
		float distance;
		String nextHopId;
		
		public DV() {};
		public DV(String nodeId, float distance, String nextHopId) {
			this.nodeId = nodeId;
			this.distance = distance;
			this.nextHopId = nextHopId;
		}
		
		private void setNodeId(String Id) {
			nodeId = Id;
		}
		
		private String getNodeId() {
			return nodeId;
		}
		
		private void setDistance(float distance1){
			distance = distance1;
		}
		
		private Float  getDistance() {
			return distance;
		}
		
		private void setNextHop(String nextHop){
			nextHopId = nextHop;
		}
		
		private String  getNextHop() {
			return nextHopId;
		}
	}	
}
