package btc.webserver.http.btc.webserver.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.sql.*;

public class ClientRequest implements Runnable{

	private NodeList nList = null;
	private static final String CRLF = "\n";
	private Socket inc;
	private String xmlReq;
	private String user = "";
	private String pass = "";
	private final Connection conn;
	static final String USERNAME="username";
	static final String PASSWORD="password";
	static final String RUN="run";
	static final String PUT="put";
	static final String GET="get";
	static final String LOGIN="login";
	static final String ACCTCREATION="create";
	static final String CHGPWD="changepw";
	private BtcDataBase db;
	/**
	 * Constructor
	 * @param incoming
	 * 	incoming socket request
	 * @throws SQLException 
	 */
	public ClientRequest(Socket incoming, Connection con) throws SQLException{
		inc = incoming;
		this.conn = con;
		db = new BtcDataBase(con);
		xmlReq = "";
	}
	
	/**
	 * Thread run
	 */
	public void run() {
		try {
			System.out.println("This bastard: " + inc.getInetAddress() + " has connected.");
			service();
			try {
				XMLParser();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Get input from user
	 * @throws IOException
	 */
	public void service() throws IOException{
		boolean body = false;
		try{
			System.out.println(inc.getInetAddress());
			BufferedReader in = new BufferedReader(new InputStreamReader(inc.getInputStream()));
			String input;
			DataOutputStream out = new DataOutputStream(inc.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
	        out.writeBytes("Content-Type: text/html\r\n\r\n");
	        out.writeBytes("<html><head></head><body><h1>Hello</h1></body></html>");
	        out.flush();
			while(!(input = in.readLine()).equals("<?>")){
				System.out.println(input);
				if(input.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")){
					body=true;
				}
				if(body){
					xmlReq += input + "\n";
				}
			}
			in.close();
	        out.close();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			inc.close();
		}
	}
	
	/**
	 * XML parse
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SQLException 
	 */
	public void XMLParser() throws SAXException, IOException, ParserConfigurationException, SQLException{
		//File f = new File("xmldoc.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new StringBufferInputStream(xmlReq));
		if(((NodeList) doc.getElementsByTagName(PUT)).getLength() > 0){
			nList = doc.getElementsByTagName(PUT);
		}
		else if(((NodeList) doc.getElementsByTagName(GET)).getLength() > 0){
			nList = doc.getElementsByTagName(GET);
		}
		else if(((NodeList) doc.getElementsByTagName(ACCTCREATION)).getLength() > 0){
			createAccount();
		}
	}
	
	/**
	 * Creates a user account
	 * @param username
	 * 		name to create
	 * @param password
	 * 		password to create
	 * @throws SQLException 
	 */
	public void createAccount() throws SQLException{
		for(int i = 0; i < nList.getLength(); i++){
			Node node = nList.item(i);
			Element eElement = (Element) node;
			user = eElement.getElementsByTagName(USERNAME).item(0).getTextContent();
			pass = eElement.getElementsByTagName(PASSWORD).item(0).getTextContent();
			db.createUser(user, pass);
		}
	}
}
