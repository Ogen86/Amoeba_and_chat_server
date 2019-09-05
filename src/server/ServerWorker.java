package server;

// to do: list-be gyűjteni a playereket játék indításakor, játékszál a szerverbe beépítani metódusként

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
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
	private static File registryFile = new File("src/server/registry.json");
	static final String sqlUsername = "Ogen86";
	static final String sqlPassword = "Ogenke86";
	static final String DB_URL =
			"jdbc:postgresql://localhost:5432/Registry";
	private static int regResult = 0;
	
	
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
	
	private void handleRegistry(String[] tokens) throws IOException {
			if (tokens.length == 3) {
			String username = tokens[1];
			String password = tokens[2];
			
			searchRegistry(username);
			
			if (regResult == 2) {
				String msg = "N;error: username occupied \n";
				outputStream.write(msg.getBytes());
				System.err.println("registry failed for " + username);
			} else if (regResult == 1) {
				fillRegistry(username,password);
				String msg = "O;Successful registry\n";
				outputStream.write(msg.getBytes());
				this.username = username;
				System.out.println("User registered in succesfully: " + username);
				}
			}
		}

	private void fillRegistry(String username, String password) {
		
		try {
			Connection con = DriverManager.getConnection(DB_URL, sqlUsername, sqlPassword);
			Statement stmt = con.createStatement();
			
			String command = "insert into Registry values('" + username + "', '" + password + "', '" + 0  + "', '" + "');";
			stmt.execute(command);
			
				
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	static void searchRegistry(String username) {
		regResult = 0;
		Connection con;
		try {
			con = DriverManager.getConnection(DB_URL, sqlUsername, sqlPassword);
			Statement stmt = con.createStatement();
			ResultSet results = stmt.executeQuery("select "+ username + " from Registry;");
			if (results.wasNull()) {
				regResult = 1;
			} else {
				regResult = 2;
			}
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	// the base setting for result is 0, if there isn't a find 1, and if there is a find 2; 
	static void searchRegistry(String username, String password) {
		regResult = 0;
		Connection con;
		try {
			con = DriverManager.getConnection(DB_URL, sqlUsername, sqlPassword);
			Statement stmt = con.createStatement();
			ResultSet results = stmt.executeQuery("select "+ username + ", " + password + " from Registry;");
			if (results.wasNull()) {
				regResult = 1;
			} else {
				regResult = 2;
			}
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
		if (tokens.length == 3) {
			String username = tokens[1];
			String password = tokens[2];
			
			searchRegistry(username, password);
			
			if (regResult == 2) {
				String msg = "N;error login (wrong password and/or username \n";
				outputStream.write(msg.getBytes());
				System.err.println("Login failed for " + username);
			} else if (regResult == 1) {
				String msg = "O;" + username;
				outputStream.write(msg.getBytes());
				this.username = username;
				System.out.println("User logged in succesfully: " + username);

				List<ServerWorker> workerList = server.getWorkerList();

				// send current user all other online logins
				for (ServerWorker worker : workerList) {
					if (worker.getLogin() != null) {
						if (!username.equals(worker.getLogin())) {
							String msg2 = "L;" + worker.getLogin();
							send(msg2);
						}
					}
				}

				// send other online users current user's status
				String onlineMsg = "L;" + username;
				for (ServerWorker worker : workerList) {
					if (!username.equals(worker.getLogin())) {
						worker.send(onlineMsg);
					}
				}
			}
			
			
		}
	}
	

	private void send(String msg) throws IOException {
		if (username != null) {
			outputStream.write(msg.getBytes());
		}
	}
}
