package com.orangomango.socket;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
	
	private ServerSocket server;
	public static ArrayList<String> banlist = new ArrayList<>();
	public static ArrayList<String> ipbanlist = new ArrayList<>();
	public static ArrayList<Room> rooms = new ArrayList<>();
	
	static {
		rooms.add(ClientManager.LOBBY_ROOM);
	}
	
	public Server(ServerSocket server){
		this.server = server;
	}
	
	public void start(){
		while (!this.server.isClosed()){
			try {
				Socket socket = this.server.accept();
				new Thread(new ClientManager(socket, ClientManager.LOBBY_ROOM.roomCode)).start();
			} catch (IOException e){
				close();
			}
		}
	}
	
	private void kick(String victim, String message){
		try {
			for (ClientManager client : ClientManager.clients){
				if (client.username.equals(victim)){
					client.writer.write(message);
					client.writer.newLine();
					client.writer.flush();
					client.close();
					break;
				}
			}
		} catch (IOException e){
			close();
		}
	}
	
	public static Room getRoomByCode(String code){
		for (Room r : rooms){
			if (r.roomCode.equals(code)){
				return r;
			}
		}
		return null;
	}
	
	public static String getUsers(String room){
		StringBuilder builder = new StringBuilder();
		builder.append("Connected users:\n");
		for (ClientManager client : ClientManager.clients){
			if (client.roomCode.equals(room) || room.equals("$all"))
				builder.append(client.username+"["+client.roomCode+"] ");
		}
		return ClientManager.YELLOW+builder.toString()+ClientManager.RESET;
	}
	
	/**
	 * Available commands:
	 * /kick <username> Kick a user from current session
	 * /say <username> <message> Write a private message to a user
	 * /ban <username> Ban a user
	 * /unban <username> Unban a user
	 * /banip <ip-address> Ban the ip address
	 * /unbanip <ip-address> Unban the ip address
	 * /banlist View banlist
	 * /list Display connected users
	 * /rooms Display available rooms
	 * /setroom <username> <roomCode> Set the room for a specified username
	 */
	public void listenForCommands(){
		new Thread(() -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				while (!this.server.isClosed()){
					String command = reader.readLine();
					if (command.startsWith("/kick")){
						if (command.split(" ").length == 1){
							System.out.println(ClientManager.RED+"Usage: /kick <username>"+ClientManager.RESET);
						} else {
							String victim = command.split(" ")[1];
							kick(victim,ClientManager.YOU_HAVE_BEEN_KICKED);
						}
					} else if (command.startsWith("/say")){
						if (command.split(" ").length < 3){
							System.out.println(ClientManager.RED+"Usage: /say <username> <msg>"+ClientManager.RESET);
						} else {
							String victim = command.split(" ")[1];
							String msg = command.split(" ", 3)[2];
							for (ClientManager client : ClientManager.clients){
								if (client.username.equals(victim) || victim.equals(ClientManager.EVERYONE)){
									client.writer.write(ClientManager.YELLOW+"SERVER: "+msg+ClientManager.RESET);
									client.writer.newLine();
									client.writer.flush();
								}
							}
						}
					} else if (command.startsWith("/unbanip")){
						if (command.split(" ").length == 1){
							System.out.println(ClientManager.RED+"Usage: /unbanip <ip-address>"+ClientManager.RESET);
						} else {
							String victim = command.split(" ")[1];
							ipbanlist.remove(victim);
						}
					} else if (command.startsWith("/unban")){
						if (command.split(" ").length == 1){
							System.out.println(ClientManager.RED+"Usage: /unban <username>"+ClientManager.RESET);
						} else {
							String victim = command.split(" ")[1];
							banlist.remove(victim);
						}
					} else if (command.startsWith("/banlist")){
						System.out.println(ClientManager.YELLOW+banlist.toString()+ClientManager.RESET);
					} else if (command.startsWith("/banip")){
						if (command.split(" ").length == 1){
							System.out.println(ClientManager.RED+"Usage: /banip <ip-address>"+ClientManager.RESET);
						} else {
							String victim = command.split(" ")[1];
							for (ClientManager cm : new ArrayList<ClientManager>(ClientManager.clients)){
								if (cm.host.equals(victim)){
									kick(cm.username, ClientManager.YOU_HAVE_BEEN_BANNED);
								}
							}
							ipbanlist.add(victim);
						}
					} else if (command.startsWith("/ban")){
						if (command.split(" ").length == 1){
							System.out.println(ClientManager.RED+"Usage: /ban <username>"+ClientManager.RESET);
						} else {
							String victim = command.split(" ")[1];
							kick(victim, ClientManager.YOU_HAVE_BEEN_BANNED);
							banlist.add(victim);
						}
					} else if (command.startsWith("/list")){
						System.out.println(getUsers(ClientManager.ALL_ROOMS));
					} else if (command.startsWith("/setroom")){
						if (command.split(" ").length < 3){
							System.out.println(ClientManager.RED+"Usage: /setroom <username> <roomCode>"+ClientManager.RESET);
						} else {
							String victim = command.split(" ")[1];
							String setRoom = command.split(" ")[2];
							for (ClientManager client : ClientManager.clients){
								if (client.username.equals(victim)){
									client.writer.write(client.changeRoom(setRoom));
									client.writer.newLine();
									client.writer.flush();
									break;
								}
							}
						}
					} else if (command.startsWith("/rooms")){
						StringBuilder builder = new StringBuilder();
						builder.append("Available rooms:\n");
						for (Room room : rooms){
							builder.append(room.roomCode+"["+room.roomOwner+"] ");
						}
						System.out.println(ClientManager.YELLOW+builder.toString()+ClientManager.RESET);
					} else {
						System.out.println(ClientManager.RED+"Invalid command!"+ClientManager.RESET);
					}
				}
				reader.close();
			} catch (IOException e){
				close();
			}
		}).start();
	}
	
	private void close(){
		try {
			if (this.server != null){
				this.server.close();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			int port = 1234;
			String host = "localhost";
			if (args.length >= 2){
				try {
					host = args[0];
					port = Integer.parseInt(args[1]);
				} catch (NumberFormatException ex) {
					System.out.println("Invalid port!");
					return;
				}
			} else {
				System.out.println("Usage: Server <host> <port>. Using default values");
			}
			
			System.out.println(ClientManager.GREEN+"Server started on "+host+" at port "+port+ClientManager.RESET);
			ServerSocket ss = new ServerSocket(port, 10, InetAddress.getByName(host));
			Server server = new Server(ss);
			server.listenForCommands();
			server.start();
		} catch (IOException ioe){
			System.out.println("Could not start server: "+ioe.getMessage());
		}
	}
}
