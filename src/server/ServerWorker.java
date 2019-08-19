package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class ServerWorker extends Thread {

	private final Socket clientSocket;
	private final Server server;
	private String username = null;
	private OutputStream outputStream;

	public ServerWorker(Server server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			handleClientSocket();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handleClientSocket() throws IOException, InterruptedException {
		InputStream inputStream = clientSocket.getInputStream();
		this.outputStream = clientSocket.getOutputStream();

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(";", 3);
			if (tokens != null && tokens.length > 0) {
				String cmd = tokens[0];
				if ("logoff".equals(cmd) || "quit".equals(cmd)) {
					handleLogoff();
					break;
				} else if ("J".equals(cmd)) {
					handleLogin(outputStream, tokens);
				} else if ("C".equals(cmd)) {
					String[] tokensMsg = line.split(";", 3);
					handleMessage(tokensMsg);
				} else if ("R".equals(cmd)) {
					String[] tokensMsg = line.split(";", 3);
					handleRegistry(tokensMsg);
				} else {
					String msg = "unknown " + cmd + "\n";
					outputStream.write(msg.getBytes());
				}
			}
		}

		clientSocket.close();
	}

	
	// format: "msg" "username" body...
	private void handleMessage(String[] tokens) throws IOException {
		String sendTo = tokens[1];
		String body = tokens[2];

		List<ServerWorker> workerList = server.getWorkerList();
		for (ServerWorker worker : workerList) {
			if (sendTo.equalsIgnoreCase(worker.getLogin())) {
				String outMsg = "msg " + username + " " + body + "\n";
				worker.send(outMsg);
			}
		}
	}

	private void handleLogoff() throws IOException {
		server.removeWorker(this);
		List<ServerWorker> workerList = server.getWorkerList();

		// send other online users current user's status
		String onlineMsg = "offline " + username + "\n";
		for (ServerWorker worker : workerList) {
			if (!username.equals(worker.getLogin())) {
				worker.send(onlineMsg);
			}
		}
		clientSocket.close();
	}

	public String getLogin() {
		return username;
	}
	
	private void handleRegistry(String[] tokens) throws IOException {
		
		if (tokens.length == 3) {
			String username = tokens[1];
			String password = tokens[2];
		
		Scanner sc = new Scanner(new File("registry.txt"));
		PrintWriter pw = new PrintWriter(new File("registry.txt"));
		
			while (sc.hasNextLine()) {
				String regLine = sc.nextLine();
				String[] regToken = regLine.split(";", 2);
				String regName = regToken[0];
				
			
		
			if (!username.equals(regName)){
				pw.println(username + ";" + password + ";" + "0");
				String msg = "Successful registry\n";
				outputStream.write(msg.getBytes());
				this.username = username;
				System.out.println("User registered in succesfully: " + username);

				
			} else {
				String msg = "error: username occupied \n";
				outputStream.write(msg.getBytes());
				System.err.println("registry failed for " + username);
				}
			}
			sc.close();
			pw.close();
		}
	}


	private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
		if (tokens.length == 3) {
			String username = tokens[1];
			String password = tokens[2];
			
			Scanner sc = new Scanner(new File("registry.txt"));
			while (sc.hasNextLine()) {
				String regLine = sc.nextLine();
				String[] regToken = regLine.split(";", 3);
				String regName = regToken[0];
				String regPass = regToken[1];

			if (username.equals(regName) && password.equals(regPass)) {
				String msg = "ok login\n";
				outputStream.write(msg.getBytes());
				this.username = username;
				System.out.println("User logged in succesfully: " + username);

				List<ServerWorker> workerList = server.getWorkerList();

				// send current user all other online logins
				for (ServerWorker worker : workerList) {
					if (worker.getLogin() != null) {
						if (!username.equals(worker.getLogin())) {
							String msg2 = worker.getLogin() + "online " + "\n";
							send(msg2);
						}
					}
				}

				// send other online users current user's status
				String onlineMsg = username + "online " + "\n";
				for (ServerWorker worker : workerList) {
					if (!username.equals(worker.getLogin())) {
						worker.send(onlineMsg);
					}
				}
			} else {
				String msg = "error login (wrong password and/or username \n";
				outputStream.write(msg.getBytes());
				System.err.println("Login failed for " + username);
			}
			sc.close();
		}
		}
	}

	private void send(String msg) throws IOException {
		if (username != null) {
			outputStream.write(msg.getBytes());
		}
	}
}
