package btc.webserver.http.btc.webserver.http;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionTester {
	private int port;
	private Socket out = null;
	public ConnectionTester(int port){
		this.port=port;
	}
	
	public void testConnect() throws IOException{
		InetSocketAddress endPoint = new InetSocketAddress( "localhost",  
                port  );
		out = new Socket();
		out.connect(endPoint);
	}
	
	public void testSend() throws IOException{
		InetSocketAddress endPoint = new InetSocketAddress( "localhost",  
                port  );
		out = new Socket();
		out.connect(endPoint);
		String toSend = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<create>\n\t<username>test</username>\n\t<password>hello</password>\n</create>";
		DataOutputStream dos = new DataOutputStream(out.getOutputStream());
		dos.writeBytes(toSend);
		dos.flush();
		dos.close();
	}
}
