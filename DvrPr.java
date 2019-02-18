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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class DvrPr {
	private int numberNeighbours;
	private List<Neighbour> neighbourTable = new ArrayList<Neighbour>();
	private List<Neighbour> neighbourTable2 = new ArrayList<Neighbour>();
	private List<DV> DVTable = new ArrayList<DV>();
	private List<DV> DVTable2 = new ArrayList<DV>();
	private static String hostID;
	private InetAddress hostIp = InetAddress.getLocalHost();
	private DatagramSocket serverSocket;
	private DatagramPacket sendPacket;
	private Map<String, Integer> alive = new HashMap<String, Integer>();
	
	public static void main(String args[]) throws Exception {
		
		hostID = args[0];
		int serverPort = Integer.parseInt(args[1]);
		String configTxt = args[2];
		boolean pr = false;
		if (args.length > 3){
			pr = true;
		}				
			
		// create a Server object, which will automatically begin to accept connection
		new DvrPr(serverPort, configTxt, pr);
	}
	
	private DvrPr(int serverPort, String filename, boolean pr) throws IOException {
		server(serverPort, filename, pr);
	}
	
	private void server(int serverPort, String filename, boolean pr) throws IOException {
		// read neighbours from config.txt
		readFile(filename, pr);
		
		// add node to "alive"
		for (int i = 0; i< neighbourTable.size(); i++){
			alive.put(neighbourTable.get(i).getNodeId(), 0);
		}
		
		// crate datagram socket from port 'servrePort'
		serverSocket = new DatagramSocket(serverPort);
		
		
	    // send my DV table to neighbours every 5 seconds
	    TimerTask task = new sendTimer();
	    Calendar  calendar= Calendar.getInstance();                  
        Date firstTime = calendar.getTime();
        Timer timer = new Timer();
        timer.schedule(task, firstTime, 5000);

	    
	    // print the solution after 1 minutes
        TimerTask task2 = new printTimer();
        Timer timer2 = new Timer();
//        timer2.schedule(task2, 58000);
        timer2.schedule(task2, 30000);
        
        // check if need posion reverse
        if (pr == true) {
	        // change DVTable
	        TimerTask task3 = new changeTimer();
	        Timer timer3 = new Timer();
	        timer3.schedule(task3, 60000);
	        
	        // print after change
	        TimerTask task4 = new printTimer();
	        Timer timer4 = new Timer();
	        timer4.schedule(task4, 90000);
        }
        
	    // give a thread to every new received DV table
		while (true) {
			
			// create space for received datagram
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			// receive datagram
			serverSocket.receive(receivePacket);
//			System.out.println(receiveData.length);
			System.out.println(new String(receiveData));
			
			
			// create a new thread for this receivePacket, and then forget about it
		    ReceiveThread receiveThread = new ReceiveThread(receivePacket);
		    Thread t = new Thread(receiveThread);
            t.start();
			
		}
	}
	
	
	// Timer for send DV table
	public class sendTimer extends TimerTask {

		@Override
		public void run() {

			byte[] sendData = new byte[1024];
			
			
			Iterator<Neighbour> NeighIter = neighbourTable.iterator();
			while (NeighIter.hasNext()) {

		    	try {
		    		Neighbour nb = NeighIter.next();
		    		int neighbourPort = nb.getPort();
		    		String neighbourID = nb.getNodeId();
		    		
		    		// if did not receive message from neighbour after sent 3 times, delete this neighbour
		    		if (alive.get(neighbourID) > 3) {
		    			
		    			// print solutions after node failure for 1 minute
		    			TimerTask task5 = new printTimer();
		    	        Timer timer5 = new Timer();
//		    	        timer5.schedule(task5, 60000);
		    	        timer5.schedule(task5, 50000);
		    	        
		    			Iterator<DV> DVIter = DVTable.iterator();
		    			while (DVIter.hasNext()){
		    				DV dv = DVIter.next();
		    				if (dv.getNodeId().equals(neighbourID)){
		    					dv.setDistance(Float.MAX_VALUE);
		    				//DVIter.remove();
		    				}
		    				if (dv.getNextHop().equals(neighbourID)){
		    					dv.setDistance(Float.MAX_VALUE);
		    					dv.setNextHop(dv.getNodeId());
		    				}
		    			}
		    			NeighIter.remove();		    			
		    		}
		    		else{
			    		String sendStr2 = hostID;
			    		for (int j = 0; j < DVTable.size(); j++){
			    			String nexthop = DVTable.get(j).getNextHop();
			    			String node = DVTable.get(j).getNodeId();
			    			if (nexthop != null && nexthop.equals(neighbourID) && (!node.equals(neighbourID))){
	//		    				sendStr2 = sendStr2 + " " + DVTable.get(j).getNodeId() + " " + Float.MAX_VALUE + " " + DVTable.get(i).getNextHop();
			    				sendStr2 = sendStr2 + " " + DVTable.get(j).getNodeId() + " " + Float.MAX_VALUE + " " + DVTable.get(j).getNextHop();
			    			}
			    			else{
			    				sendStr2 = sendStr2 + " " + DVTable.get(j).getNodeId() + " " + DVTable.get(j).getDistance() + " " + DVTable.get(j).getNextHop();
			    			}		    			
			    		}
			    		sendData = sendStr2.getBytes();
			    		sendPacket = new DatagramPacket(sendData, sendData.length, hostIp, neighbourPort);
						serverSocket.send(sendPacket);
						System.out.println("send " + sendStr2);
						System.out.println("send " + neighbourPort);
						int value = alive.get(neighbourID);
						alive.replace(neighbourID, value+1); 
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
//		    	i = i + 1;
		    }
			
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
	
	// Timer for change distance
	public class changeTimer extends TimerTask {
		public void run(){
			
			for (int i = 0; i < DVTable.size(); i++){
				String node1 = DVTable.get(i).getNodeId();
				for (int j = 0; j < DVTable2.size(); j++){
					String node2 = DVTable2.get(j).getNodeId();
					if (node1.equals(node2)){
						DVTable.get(i).setNodeId(node2);
						DVTable.get(i).setDistance(DVTable2.get(j).getDistance());
						DVTable.get(i).setNextHop(node2);
					}
				}
			}
			
			for (int i = 0; i < neighbourTable.size(); i++){
				String node1 = neighbourTable.get(i).getNodeId();
				for (int j = 0; j < neighbourTable2.size(); j++){
					String node2 = neighbourTable2.get(j).getNodeId();
					if (node1.equals(node2)){
						neighbourTable.get(i).setNodeId(node2);
						neighbourTable.get(i).setDistance(neighbourTable2.get(j).getDistance());
					}
				}
			}
			
			String sendStr = "";
			for (int i = 0; i < DVTable.size();){
				sendStr = sendStr+" " + DVTable.get(i).getNodeId() + " " + DVTable.get(i).getDistance() + " " + DVTable.get(i).getNextHop();
				i = i + 1;
			}
//			System.out.println("改过之后的 = " + sendStr);
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
			
			// if received, then minus the value in "alive"
			int value = alive.get(receiveID);
			alive.replace(receiveID, value-1);
			
			// this is used for test program
			int port = receivePacket.getPort();
//			System.out.println("connection from " + port);				
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
//			System.out.print("distanceHostToReceive= " + distanceHostToReceive);
			
			// update exist node and add new node to the list
			
			for (int i = 0; i < receiveDVTable.size();) {
				String node1 = receiveDVTable.get(i).getNodeId();
//				System.out.println("他表的: " + i + receiveDVTable.get(i).getNodeId());
				if (! node1.equals(hostID)) {
					float distance1 = receiveDVTable.get(i).getDistance();
					float newDistance = distance1 + distanceHostToReceive;
					int count = 0;
					for (int j = 0; j < DVTable.size();) {
//						System.out.println("我表的: " + j + DVTable.get(j).getNodeId());
						String node2 = DVTable.get(j).getNodeId();
						float distance2 =  DVTable.get(j).getDistance();
						if ((!node2.equals(receiveID)) && node1.equals(node2) && (newDistance < distance2)) {
							DVTable.get(j).setDistance(newDistance);
							DVTable.get(j).setNextHop(receiveID);
							count = count + 1;
							j = j + 1;
						}
						else if ((!node2.equals(receiveID)) && node1.equals(node2) && (newDistance >= distance2)) {
							if (DVTable.get(j).getNextHop().equals(receiveID) && newDistance > 50000){								
								DVTable.get(j).setDistance(Float.MAX_VALUE);
								j = j + 1;
								count = count + 1;
								//DVTable.get(j).setNextHop(null);
							}
							else{
							count = count + 1;
							j = j + 1;}
						}
						else if ((!node2.equals(receiveID)) && (j == (DVTable.size()-1)) && (count == 0)){
							if ((alive.get(node1) == null) || (alive.get(node1) <= 3)){
							DV node = new DV();
							node.setNodeId(node1);
							node.setDistance(distance1+distanceHostToReceive);
							node.setNextHop(receiveID);
							DVTable.add(node);
							j = j + 1;}
							else{
								j = j +1;
							}
						}
						else{
							j = j + 1;}
					}
					i = i + 1;
				}
				else {
					i = i + 1;}
			}
		}

		@Override
		public void run() {
			
			extractData();
			modifyDV();
			String sendStr = "";
			for (int i = 0; i < DVTable.size();){
				sendStr = sendStr+" " + DVTable.get(i).getNodeId() + " " + DVTable.get(i).getDistance() + " " + DVTable.get(i).getNextHop();
				i = i + 1;
			}
//			System.out.println("new = " + sendStr);
		}
	}
	
	private void readFile(String fileName, boolean pr) {
		
		if (pr == false){
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
		                DV dvNode = new DV(info[i], Float.parseFloat(info[i+1]), info[i]);
		                DVTable.add(dvNode);
//		                System.out.println(info[i] + " " + info[i+1] + " " + info[i+2]);
		                i = i + 3;
		                }
		            }
		        br.close();
			}
		    catch (Exception e) {
		        System.err.println("read errors :" + e);
		    }
		}
		else{
			try {
		        BufferedReader br = new BufferedReader(new FileReader(fileName));                                      
		        String lineTxt = null; 
		        lineTxt = br.readLine();
		        numberNeighbours = Integer.parseInt(lineTxt);
		        while ((lineTxt = br.readLine()) != null) {
		            String[] info = lineTxt.split(" ");
		            int i = 0 ;
		            	Neighbour nextOne = new Neighbour(info[i], Float.parseFloat(info[i+1]), Integer.parseInt(info[i+3]));
		                neighbourTable.add(nextOne);
		                DV dvNode = new DV(info[i], Integer.parseInt(info[i+1]), info[i]);
		                DVTable.add(dvNode);
		                // add changed node to a new table
//		                System.out.println(info[i] + " " + info[i+1] + " " + info[i+2] + " "+ info[i+3]);
		                float distance1 = Float.parseFloat(info[i+1]);
		                float distance2 = Float.parseFloat(info[i+2]);
		                if (distance1 != distance2){
		                Neighbour nextOne2 = new Neighbour(info[i], Float.parseFloat(info[i+2]), Integer.parseInt(info[i+3]));
		                neighbourTable2.add(nextOne2);
		                DV dvNode2 = new DV(info[i], Float.parseFloat(info[i+2]), info[i]);
		                DVTable2.add(dvNode2);}
		                
//		                i = i + 3;
		                
		            }
		        br.close();
			}
		    catch (Exception e) {
		        System.err.println("read errors :" + e);
		    }
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
		
		private Float getDistance() {
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
