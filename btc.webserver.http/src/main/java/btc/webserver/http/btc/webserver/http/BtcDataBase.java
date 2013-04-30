package btc.webserver.http.btc.webserver.http;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class BtcDataBase {
	
	String userid;
	static final String PASSWORD="password";
	String password;
	Statement statement = null;
	private final Connection conn;
	public BtcDataBase(Connection conn) throws SQLException {
		this.conn = conn;
	}
	
	public void queryRun(String runname) throws SQLException {

		ResultSet result_set = null;

		result_set = statement.executeQuery("select * from runs");

		while (result_set.next()) {
			String run_name = result_set.getString("runname");

			if (run_name.equals(runname)) {
				System.out.println("\nRun Name is "
						+ result_set.getString("runname")
						+ " and File Name is: "
						+ result_set.getString("filepath"));
			}
		}
	}

	/**
	 * creates user
	 * @param user
	 * 		user name
	 * @param password
	 * 		desired password
	 * @return
	 * 		true if successful
	 * @throws SQLException
	 */
	public boolean createUser(String user, String password, String salt)throws SQLException{
		System.out.println("Creating user");
		PreparedStatement check = conn.prepareStatement("SELECT * FROM User WHERE username = ?");
		check.setString(1, user);
		ResultSet rs = check.executeQuery();
		boolean hasUser = rs.next();
		if(!hasUser)
		{
			PreparedStatement ps = conn.prepareStatement("INSERT INTO User (username, password, salt) VALUES(?,?,?)");
			ps.setString(1, user);
			ps.setString(2, password);
			ps.setString(3, salt);
			ps.executeUpdate();
			return true;
		}
		else
		{
			return false;
		}
	}
	 
	/**
	 * logs a user in or checks a creation against the system
	 * @param user
	 * 		user name
	 * @param pass
	 * 		password (will be hashed)
	 * @return
	 * 		true if successful
	 * @throws SQLException
	 */
	public boolean dbLogin(String user, String pass) throws SQLException{
		String statement = "SELECT password FROM User WHERE username = ?";
		PreparedStatement ps = conn.prepareStatement(statement);
		ps.setString(1, user);
		ResultSet rs = ps.executeQuery();
		if(rs.next() && pass.equals(rs.getString("password"))){
			return true;
		}
		return false;
	}
	
	/**
	 * puts a run in db
	 * @param uName
	 * 		user name
	 * @param xml
	 * 		xml file text
	 * @param rName
	 * 		run name
	 * @return
	 * 		true if successful
	 * 		false if failed or exception
	 * @throws SQLException
	 * @throws IOException
	 */
	public boolean putRun(String uName, String xml, String rName) throws SQLException, IOException{
		String filePath = "/opt/lampp/htdocs/webalizer/" + uName + "-" + rName + ".json";
		PreparedStatement toCheck = conn.prepareStatement("SELECT runName FROM Runs WHERE filepath = ?");
		toCheck.setString(1, filePath);
		ResultSet set = toCheck.executeQuery();
		if(set.next()) return false;
		PreparedStatement ps = conn.prepareStatement("INSERT INTO Runs (runName, filepath, userName) VALUES(?,?,?)");
		ps.setString(1, rName);
		ps.setString(2, filePath);
		ps.setString(3, uName);
		ps.executeUpdate();
		FileWriter fstream = new FileWriter(filePath);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(xml);
		out.close();
		return true;
	}
	
	/**
	 * Lists runs for user
	 * @param uName
	 * 		username to list for
	 * @return
	 * 		resultset of runs
	 * @throws SQLException
	 */
	public ResultSet listRuns(String uName) throws SQLException{
		PreparedStatement ps = conn.prepareStatement("SELECT runName FROM Runs WHERE userName = ?");
		ps.setString(1, uName);
		return ps.executeQuery();
	}
	
	public boolean changePassword(String user, String pass, String newpass) throws SQLException{
		PreparedStatement ps = conn.prepareStatement("SELECT * FROM User WHERE username = ?");
		ps.setString(1, user);
		ResultSet rs = ps.executeQuery();
		while(rs.next()){
			if(rs.getString(PASSWORD).equals(pass)){
				PreparedStatement p = conn.prepareStatement("UPDATE User SET password = ? WHERE username = ?");
				p.setString(1, newpass);
				p.setString(2, user);
				p.executeUpdate();
				return true;
			}
		}
		return false;
	}
}
