package btc.webserver.http.btc.webserver.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class HttpServer {
	final static String user = "root";
	final static String password = "";
	final static String db = "behindthecurtain";
	final static String jdbc = "jdbc:mysql://localhost:3306/" + db + "?user=" + user + "&password=" + password;
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		//Runtime.getRuntime().exec("export set CLASSPATH=.:~/mysql-connector-java.jar:$CLASSPATH");
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		Connection con = DriverManager.getConnection(jdbc);
		ServerSocket socket = new ServerSocket(8080);
		while(true){
			Socket inc = socket.accept();
			System.out.println("Client Connected: " + inc.getInetAddress());
			Thread t = new Thread(new ClientRequest(inc, con));
			t.start();
		}
	}
}
