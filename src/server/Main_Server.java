package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class Main_Server {

	HashMap<Integer, String> sockets = new HashMap<>();

	public static void main(String[] args) {

		try {
			ServerSocket ss = new ServerSocket(10001);
			System.out.println("server initialized");
			Scanner sc = new Scanner(System.in);
			String srv = sc.nextLine();

			if (srv.equals("start")){
				while (true) {
					System.out.println("lobby initialized");
					
					Socket clientSocket1 = ss.accept();
					Socket clientSocket2 = ss.accept();
	
					Thread th = new Thread(new GameThread(clientSocket1, clientSocket2));
					th.start();
	
					if (srv.equals("exit")) {
						System.out.println("server is shutting down");
						break;
	
					}
					sc.close();
					ss.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
