package com.orangomango.socket;

import java.net.*;
import java.io.*;
import java.util.*;

public class ClientManager implements Runnable{
	private Socket socket;
	private BufferedReader reader;
	public BufferedWriter writer;
	public String username;
	
	public static final String RED = "\u001B[31m";
	public static final String GREEN = "\u001B[32m";
	public static final String BLUE = "\u001B[36m";
	public static final String YELLOW = "\u001B[33m";
	public static final String RESET = "\u001B[0m";
	
	public static final String NAME_ALREADY_TAKEN = RED+"ERROR: Name already taken"+RESET;
	public static final String YOU_HAVE_BEEN_KICKED = RED+"ERROR: You have been kicked"+RESET;
	public static final String YOU_HAVE_BEEN_BANNED = RED+"ERROR: You have been banned"+RESET;
	
	public static List<ClientManager> clients = new ArrayList<>();
	
	public ClientManager(Socket socket){
		try {
			this.socket = socket;
			this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			this.username = this.reader.readLine();
			boolean found = false;
			for (ClientManager client : clients){
				if (client.username.equals(username)){
					found = true;
					break;
				}
			}
			if (found){
				this.writer.write(NAME_ALREADY_TAKEN);
				this.writer.newLine();
				this.writer.flush();
				this.socket.close();
				return;
			}
			
			// Check if user has been banned
			boolean banned = false;
			for (String user : Server.banlist){
				if (user.equals(username)){
					banned = true;
					break;
				}
			}
			if (banned){
				this.writer.write(YOU_HAVE_BEEN_BANNED);
				this.writer.newLine();
				this.writer.flush();
				this.socket.close();
				return;
			}
			
			clients.add(this);
			broadcastMessage(GREEN+this.username+" joined"+RESET);
			System.out.println(GREEN+socket.getInetAddress().getHostAddress()+":"+socket.getLocalPort()+"("+this.username+") has connected"+RESET);
		} catch (IOException e){
			close();
		}
	}
	
	@Override
	public void run(){
		while (this.socket.isConnected()){
			try {
				String message = this.reader.readLine();
				if (message == null){
					throw new IOException("Client disconnected");
				}
				broadcastMessage(message);
			} catch (IOException e){
				close();
				break;
			}
		}
	}
	
	private void broadcastMessage(String message){
		if (message.contains(":")) System.out.println(BLUE+message+RESET);
		if (message.equals(this.username+": /list")){
			try {
				this.writer.write(Server.getUsers());
				this.writer.newLine();
				this.writer.flush();
				return;
			} catch (IOException e){
				close();
			}
		}
		for (ClientManager client : clients){
			try {
				if (!client.equals(this)){
					// Allow pings
					if (message.split(":").length > 1 && message.split(":")[1].startsWith(" @")){
						String receiver = message.split(":")[1].substring(2, message.split(":")[1].length()).split(" ")[0];
						if (client.username.equals(receiver)){
							client.writer.write(BLUE+message+RESET);
							client.writer.newLine();
							client.writer.flush();
						}
					} else {
						client.writer.write(message);
						client.writer.newLine();
						client.writer.flush();
					}
				}
			} catch (IOException e){
				close();
			}
		}
	}
	
	public void close(){
		if (clients.contains(this)){
			clients.remove(this);
			broadcastMessage(RED+this.username+" left"+RESET);
			System.out.println(RED+this.socket.getInetAddress().getHostAddress()+"("+this.username+") disconnected"+RESET);
		}
		try {
			if (this.socket != null){
				this.socket.close();
			}
			if (this.reader != null){
				this.reader.close();
			}
			if (this.writer != null){
				this.writer.close();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
}
