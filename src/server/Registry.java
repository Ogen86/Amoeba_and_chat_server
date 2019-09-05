package server;

public class Registry {
	
	private String username;
	private String password;
	private int wins;
	
	
	public Registry(String username, String password, int wins) {
		super();
		this.username = username;
		this.password = password;
		this.wins = wins;
	}

	

	public Registry() {
		super();
	}



	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public int getWins() {
		return wins;
	}


	public void setWins(int wins) {
		this.wins = wins;
	}
	
	@Override
	public String toString() {
		return "Person [username=" + username + ", password=" + password + ", wins=" + wins + "]";
	}
	

}
