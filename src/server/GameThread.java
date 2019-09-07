package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class GameThread implements Runnable {

	Socket clientSocket1;
	Socket clientSocket2;

	public GameThread(Socket clientSocket1, Socket clientSocket2) {
		this.clientSocket1 = clientSocket1;
		this.clientSocket2 = clientSocket2;
	}

	@Override
	public void run() {

		try {
			BufferedReader br1 = new BufferedReader(new InputStreamReader(clientSocket1.getInputStream()));
			BufferedReader br2 = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));
			PrintWriter pw1 = new PrintWriter(clientSocket1.getOutputStream(), true);
			PrintWriter pw2 = new PrintWriter(clientSocket2.getOutputStream(), true);

			while (true) {
				String msg = br1.readLine();
				pw2.println(msg);
				String msg2 = br2.readLine();
				pw1.println(msg2);
				
				if (msg.equals("W;/*"))
					pw1.println("O;You won!");
					pw2.println("O;You lost!");
					
				if ( msg2.equals("W;/*"))
					pw1.println("O;You lost!");
					pw2.println("O;You won!");
					
				if (msg.equals("B;/*") || msg2.equals("B;/*")) {
					pw1.println("game terminatied by user command");
					pw2.println("game terminatied by user command");
					break;
				}
			}

			pw1.close();
			pw2.close();
			br1.close();
			br2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
