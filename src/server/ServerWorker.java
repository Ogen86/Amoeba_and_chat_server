package server;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ServerWorker extends Thread {

	private final Socket clientSocket;
	private final Server server;
	private String username = null;
	private OutputStream outputStream;
	private OutputStreamWriter regStream;
	private final String registry = "src/server/registry.txt";

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

	// CMD format: commandToken;1stcommandToken;2ndcommandToken (if needed)
	private void handleClientSocket() throws IOException, InterruptedException {
		InputStream inputStream = clientSocket.getInputStream();
		this.outputStream = clientSocket.getOutputStream();
		try {
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
					handleMessage(tokens);
				} else if ("R".equals(cmd)) {
					handleRegistry(tokens);
				} else if ("P".equals(cmd)) {
					handlePlayRequest(tokens);
				} else {
					String msg = "unknown " + cmd + "\n";
					outputStream.write(msg.getBytes());
				}
			}
		}

		clientSocket.close();
		} catch (SocketException e) {
			handleLogoff();
		}
	}

	private void handlePlayRequest(String[] tokens) throws IOException {
		List<ServerWorker> workerList = server.getWorkerList();
		if (tokens.length == 3) {
			String username = tokens[1];

			for (ServerWorker worker : workerList) {
				if (username.equalsIgnoreCase(worker.getLogin())) {
					String outMsg = "P" + ";" + this.getLogin();
					worker.send(outMsg);
					InputStream response = worker.clientSocket.getInputStream();
					BufferedReader rpReader = new BufferedReader(new InputStreamReader(response));

					if (rpReader.readLine() != null) {

						if (rpReader.equals("A;" + worker.getLogin())) {

							String msg = "O;The game started";
							outputStream.write(msg.getBytes());
							OutputStream player2 = worker.outputStream;
							player2.write(msg.getBytes());

							Socket clientSocket1 = this.clientSocket;
							Socket clientSocket2 = worker.clientSocket;

							GameThread gameThread = new GameThread(clientSocket1, clientSocket2);

							gameThread.run();

						} else if (rpReader.equals("D;" + worker.getLogin())) {
							String msg = "N;" + username + "; declined to play";
							outputStream.write(msg.getBytes());
						} else {
							String msg = "N;error: unrecognised command \n";
							outputStream.write(msg.getBytes());
						}
					}
				}
			}
		}
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
		String onlineMsg = "L;" + username + "\n";
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

	private void handleRegistry(String[] tokens) {
		if (tokens.length == 3) {
			String username = tokens[1];
			String password = tokens[2];
			
			
			
			try {
				regStream = new OutputStreamWriter(new FileOutputStream(registry, true));
				Scanner sc = new Scanner(new File(registry));
				PrintWriter pw = new PrintWriter(regStream,true);
				
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
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
				
			
	}
	

	private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
		if (tokens.length == 3) {
			String username = tokens[1];
			String password = tokens[2];

			try {
				Scanner sc = new Scanner(new File(registry));
			
			while (sc.hasNextLine()) {
				String regLine = sc.nextLine();
				String[] regToken = regLine.split(";", 3);
				String regName = regToken[0];
				String regPass = regToken[1];

			if (username.equals(regName) && password.equals(regPass)) {
				String msg = "O;" + username;
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
				String onlineMsg = "C;" + username + "online " + "\n";
				for (ServerWorker worker : workerList) {
					if (!username.equals(worker.getLogin())) {
						worker.send(onlineMsg);
					}
				}
			} else {
				String msg = "N;error login (wrong password and/or username \n";
				outputStream.write(msg.getBytes());
				System.err.println("Login failed for " + username);
			}
			//sc.close();
		}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void send(String msg) throws IOException {
		if (username != null) {
			outputStream.write(msg.getBytes());
		}
	}
}
