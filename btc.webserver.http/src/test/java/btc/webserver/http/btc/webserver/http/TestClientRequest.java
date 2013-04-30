package btc.webserver.http.btc.webserver.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class TestClientRequest extends TestCase {
	final static String user = "root";
	private BtcDataBase DB = null;
	final static String password = "";
	final static String db = "behindthecurtain";
	final static String jdbc = "jdbc:mysql://localhost:3306/" + db + "?user=" + user + "&password=" + password;
	public void testDbConnection(){
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			try {
				Connection con = DriverManager.getConnection(jdbc);
				DB = new BtcDataBase(con);
			} catch (SQLException e) {
				fail();
				e.printStackTrace();
			}
		} catch (InstantiationException e) {
			fail();
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			fail();
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			fail();
			e.printStackTrace();
		}
		
	}
	
	public void testCreate(){
		testDbConnection();
		ServerSocket socket;
		try {
			socket = new ServerSocket(8081);
			ConnectionTester tester = new ConnectionTester(8081);
			tester.testConnect();
			Thread t = new Thread(new ClientRequest(new Socket("localhost", 8081), DriverManager.getConnection(jdbc)));
			t.start();
		} catch (IOException e) {
			fail();
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testXMLParser(){
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(8082);
		} catch (IOException e1) {
			e1.printStackTrace();
			fail();
		}
		while(true){
			try {
				ConnectionTester ct = new ConnectionTester(8082);
				ct.testSend();
				Socket inc = ss.accept();
			} catch (IOException e) {
				fail();
				e.printStackTrace();
			}
		}
	}
}
