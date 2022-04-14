package com.orangomango.socket;

import java.net.*;
import java.io.*;
import java.util.*;

public class ClientManager implements Runnable{
	private Socket socket;
	private BufferedReader reader;
	public BufferedWriter writer;
	public String username;
	public String roomCode;
	
	public static final String RED = "\u001B[31m";
	public static final String GREEN = "\u001B[32m";
	public static final String BLUE = "\u001B[36m";
	public static final String YELLOW = "\u001B[33m";
	public static final String RESET = "\u001B[0m";
	
	public static final String ALL_ROOMS = "$all";
	public static final Room LOBBY_ROOM = new Room("lobby", "$server");
	public static final String NAME_ALREADY_TAKEN = RED+"ERROR: Name already taken"+RESET;
	public static final String YOU_HAVE_BEEN_KICKED = RED+"ERROR: You have been kicked"+RESET;
	public static final String YOU_HAVE_BEEN_BANNED = RED+"ERROR: You have been banned"+RESET;
	
	public static List<ClientManager> clients = new ArrayList<>();
	
	public ClientManager(Socket socket, String rc){
		try {
			this.socket = socket;
			this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			this.roomCode = rc;
			
			this.username = this.reader.readLine();
			boolean found = false;
			for (ClientManager client : clients){
				if (client.username.equals(username)){
					found = true;
					break;
				}
			}
			if (found || username.equals("$server")){
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
			broadcastMessage(GREEN+this.username+" joined the server"+RESET, true);
			System.out.println(GREEN+socket.getInetAddress().getHostAddress()+":"+socket.getLocalPort()+"("+this.username+") connected"+RESET);
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
				broadcastMessage(message, false);
			} catch (IOException e){
				close();
				break;
			}
		}
	}
	
	private void disconnectAllFromRoom(String room){
		for (ClientManager u : clients){
			if (u.roomCode.equals(room) && u != this){
				try {
					u.writer.write(YELLOW+"SERVER: Owner left the room so you were put in the lobby"+RESET);
					u.writer.newLine();
					u.writer.flush();
					u.roomCode = LOBBY_ROOM.roomCode;
				} catch (IOException ioe){
					close();
				}
			}
		}
	}
	
	public String changeRoom(String room){
		String serverMessage = "";
		broadcastMessage(RED+this.username+" left this room"+RESET, false);
		if (Server.getRoomByCode(room) == null){
			Server.rooms.add(new Room(room, this.username));
			serverMessage = YELLOW+"SERVER: Room does not exists so will be created\n"+RESET;
		}
		if (Server.getRoomByCode(this.roomCode).roomOwner.equals(this.username)){
			disconnectAllFromRoom(this.roomCode);
			Server.rooms.remove(Server.getRoomByCode(this.roomCode));
		}
		this.roomCode = room;
		serverMessage += YELLOW+"SERVER: You entered room: "+room+RESET;
		broadcastMessage(GREEN+this.username+" joined this room"+RESET, false);
		return serverMessage;
	}
	
	private void broadcastMessage(String message, boolean toAll){
		if (message.contains(":")) System.out.println(BLUE+"["+this.roomCode+"] "+message+RESET);
		if (message.equals(this.username+": /list")){
			try {
				this.writer.write(Server.getUsers(this.roomCode));
				this.writer.newLine();
				this.writer.flush();
				return;
			} catch (IOException e){
				close();
			}
		} else if (message.startsWith(this.username+": /room") || message.equals(this.username+": /lobby")){
			String serverMessage;
			if (message.split(" ").length >= 3 || message.equals(this.username+": /lobby")){
				String room = message.equals(this.username+": /lobby") ? "lobby" : message.split(" ")[2];
				if (room.equals(ALL_ROOMS)){
					serverMessage = YELLOW+"SERVER: Invalid room code"+RESET;
				} else {
					serverMessage = changeRoom(room);
				}
			} else {
				serverMessage = YELLOW+"SERVER: Usage: /room <roomCode>"+RESET;
			}
			try {
				this.writer.write(serverMessage);
				this.writer.newLine();
				this.writer.flush();
				return;
			} catch (IOException e){
				close();
			}
		} else if (message.startsWith(this.username+": /")){
			// Last part of if-statement. Command is not available
			try {
				this.writer.write(YELLOW+"SERVER: Invalid command!"+RESET);
				this.writer.newLine();
				this.writer.flush();
				return;
			} catch (IOException e){
				close();
			}
		}
		for (ClientManager client : clients){
			if (!client.roomCode.equals(this.roomCode) && !toAll) continue;
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
			broadcastMessage(RED+this.username+" left the server"+RESET, true);
			System.out.println(RED+this.socket.getInetAddress().getHostAddress()+"("+this.username+") disconnected"+RESET);
			if (Server.getRoomByCode(this.roomCode).roomOwner.equals(this.username)){
				disconnectAllFromRoom(this.roomCode);
				Server.rooms.remove(Server.getRoomByCode(this.roomCode));
			}
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
