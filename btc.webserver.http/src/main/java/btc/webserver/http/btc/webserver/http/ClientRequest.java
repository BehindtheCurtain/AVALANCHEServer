package btc.webserver.http.btc.webserver.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
	private final Connection conn;
	private Socket inc;
	
	private String xmlReq;
	private String user = "";
	private String pass = "";
	private String salt = "";
	private String sessionId = "";

	
	static final String CRLF = "\n";
	static final String SALT="salt";
	static final String CHANGE="change";
	static final String USERNAME="username";
	static final String PASSWORD="password";
	static final String NEWPASSWORD="newpassword";
	static final String RUN="run";
	static final String NAME="name";
	static final String RUNCONTENT="runcontent";
	static final String PUT="put";
	static final String GET="get";
	static final String LOGIN="login";
	static final String ACCTCREATION="create";
	static final String CHGPWD="changepw";
	static final String POST="post";
	static final String QUERY="query";
	static final String DOWNLOAD="download";
	
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
		DataOutputStream out = null;
		BufferedReader in = null;
		try{
			System.out.println(inc.getInetAddress());
			in = new BufferedReader(new InputStreamReader(inc.getInputStream()));
			String input;
			out = new DataOutputStream(inc.getOutputStream());
			
			
			while(!(input = in.readLine()).equals("<?>")){
				System.out.println(input);
				if(input.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")){
					body=true;
				}
				if(input.contains("<<"))
				{
					sessionId = in.readLine();
				}
				else if(body){
					xmlReq += input + "\n";
				}
			}
			try {

					out.writeBytes(XMLParser());
			        out.flush();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			in.close();
	        out.close();
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
	public String XMLParser() throws SAXException, IOException, ParserConfigurationException, SQLException{
		String message = "";
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new StringBufferInputStream(xmlReq));
		if(((NodeList) doc.getElementsByTagName(DOWNLOAD)).getLength() > 0){
			nList = doc.getElementsByTagName(DOWNLOAD);
			message += downloadRun();
		}
		else if(((NodeList) doc.getElementsByTagName(CHANGE)).getLength() > 0) {
			nList = doc.getElementsByTagName(CHANGE);
			message += changePassword();
		}
		else if(((NodeList) doc.getElementsByTagName(RUN)).getLength() > 0){
			message += storeRun(doc);
		}
		else if(((NodeList) doc.getElementsByTagName(QUERY)).getLength() > 0){
			nList = doc.getElementsByTagName(QUERY);
			message += listRuns();
		}
		else if(((NodeList) doc.getElementsByTagName(ACCTCREATION)).getLength() > 0){
			nList = doc.getElementsByTagName(ACCTCREATION);
			message += createAccount();
		}
		else if(((NodeList) doc.getElementsByTagName(LOGIN)).getLength() > 0){
			nList = doc.getElementsByTagName(LOGIN);
			message += login();
		}
		return message;
	}
	
	/**
	 * Creates a user account
	 * @param username
	 * 		name to create
	 * @param password
	 * 		password to create
	 * @return message
	 * 		http message
	 * @throws SQLException 
	 */
	public String createAccount() throws SQLException{
		String message ="";
		for(int i = 0; i < nList.getLength(); i++){
			Node node = nList.item(i);
			Element eElement = (Element) node;
			user = eElement.getElementsByTagName(USERNAME).item(0).getTextContent();
			pass = eElement.getElementsByTagName(PASSWORD).item(0).getTextContent();
			salt = eElement.getElementsByTagName(SALT).item(0).getTextContent();
			if(db.createUser(user, pass, salt)){
				if(db.dbLogin(user, pass)){
					message += "HTTP/1.1 201 Created\r\nContent-Type: text/html\r\n\r\n<html><head></head><body><h1>Hello</h1></body></html>";
					sessionId = user;
				}
				else{
					message += "HTTP/1.1 401 Unauthorized\nContent-Type: text/html\n<html><HEAD><TITLE>404 Not Found</TITLE></HEAD><BODY><b>404</b></ br><p>Unsuccessful Login Attempt.<p></BODY></HTML>";
				}
			} 
			else{
				message += "HTTP/1.1 409 Conflict\nContent-Type: text/html\n<html><HEAD><TITLE>404 Not Found</TITLE></HEAD><BODY><b>404</b></ br><p>Unsuccessful Login Attempt.<p></BODY></HTML>";
			}
		}
		return message;
	}
	
	/**
	 * Store a run in the DB
	 * @param doc
	 * 		document to parse
	 * @return
	 * 		the http message
	 * @throws SQLException
	 * @throws IOException
	 */
	public String storeRun(Document doc) throws SQLException, IOException{
		String message = "";
		String runName = doc.getDocumentElement().getAttribute(NAME);
		String run = doc.getDocumentElement().getTextContent();
		
		if(db.putRun(sessionId, run, runName)){
			message += "HTTP/1.1 201 Created\r\nContent-Type: text/html\r\n\r\n<html><head></head><body><h1>Hello</h1></body></html>";
		}
		else{
			message += "HTTP/1.1 401 Unauthorized\nContent-Type: text/html\n<html><HEAD><TITLE>404 Not Found</TITLE></HEAD><BODY><b>404</b></ br><p>Unsuccessful Login Attempt.<p></BODY></HTML>";
		}
		return message;
	}
	
	public String login() throws SQLException{
		String message = "";
		for(int i = 0; i < nList.getLength(); i++){
			Node node = nList.item(i);
			Element eElement = (Element) node;
			user = eElement.getElementsByTagName(USERNAME).item(0).getTextContent();
			pass = eElement.getElementsByTagName(PASSWORD).item(0).getTextContent();
			if(db.dbLogin(user, pass)){
				message += "HTTP/1.1 202 Accepted\r\nContent-Type: text/html\r\n\r\n<html><head></head><body><h1>Hello</h1></body></html>";
				sessionId = user;
			}
			else{
				message += "HTTP/1.1 401 Unauthorized\nContent-Type: text/html\n<html><HEAD><TITLE>404 Not Found</TITLE></HEAD><BODY><b>404</b></ br><p>Unsuccessful Login Attempt.<p></BODY></HTML>";
			}
		}
		return message;
	}
	
	/**
	 * List runs for user
	 * @return
	 * 		resultset from database
	 * @throws SQLException
	 */
	public String listRuns() throws SQLException {
		String message = "HTTP/1.1 200 OK\r\nContent-Type: text/xml\r\n\r\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<runs>\n";
		for(int i = 0; i < nList.getLength(); i++){
			Node node = nList.item(i);
			Element eElement = (Element) node;
			sessionId = eElement.getElementsByTagName(USERNAME).item(0).getTextContent();
		}
			ResultSet rs = db.listRuns(sessionId);
			while(rs.next()){
				message += "\t<run>" + rs.getString("runName") + "</run>\n";
			}
			message += "</runs>";
		return message;
	}
	
	public String downloadRun() throws IOException{
		String message = "HTTP/1.1 201 Authorized\r\nContent-Type: text/xml\r\n\r\n";
		String runName = "";
		for(int i = 0; i < nList.getLength(); i++){
			Node node = nList.item(i);
			Element eElement = (Element) node;
			runName += eElement.getElementsByTagName("runname").item(0).getTextContent();
			sessionId = eElement.getElementsByTagName(USERNAME).item(0).getTextContent();
		}
		String line;
		BufferedReader br = new BufferedReader(new FileReader("/opt/lampp/htdocs/webalizer/" + sessionId + "-" + runName + ".xml"));
		while((line = br.readLine()) != null) message += line;
		return message;
	}
	
	public String changePassword() throws SQLException{
		String message = "";
		String password = "";
		String newPassword = "";
		for(int i = 0; i < nList.getLength(); i++){
			Node node = nList.item(i);
			Element eElement = (Element) node;
			sessionId = eElement.getElementsByTagName(USERNAME).item(0).getTextContent();
			password = eElement.getElementsByTagName(PASSWORD).item(0).getTextContent();
			newPassword = eElement.getElementsByTagName(NEWPASSWORD).item(0).getTextContent();
		}
		if(db.changePassword(sessionId, password,newPassword)){
			message += "HTTP/1.1 202 Accepted\r\nContent-Type: text/html\r\n\r\n<html><head></head><body><h1>Password Successfully changed</h1></body></html>";
			return message;
		}
		message += "HTTP/1.1 401 Unauthorized\nContent-Type: text/html\n<html><HEAD><TITLE>404 Not Found</TITLE></HEAD><BODY><b>404</b></ br><p>Your username and password combination failed.<p></BODY></HTML>";
		return message;
	}
}
