package com.orangomango.socket;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
	
	private ServerSocket server;
	public static ArrayList<String> banlist = new ArrayList<>();
	
	public Server(ServerSocket server){
		this.server = server;
	}
	
	public void start(){
		while (!this.server.isClosed()){
			try {
				Socket socket = this.server.accept();
				new Thread(new ClientManager(socket)).start();
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
	
	public static String getUsers(){
		StringBuilder builder = new StringBuilder();
		builder.append("Connected users:\n");
		for (ClientManager client : ClientManager.clients){
			builder.append(client.username+" ");
		}
		return ClientManager.YELLOW+builder.toString()+ClientManager.RESET;
	}
	
	/**
	 * Available commands:
	 * /kick <username> Kick a user from current session
	 * /say <username> <message> Write a private message to a user
	 * /ban <username> Ban a user
	 * /unban <username> Unban a user
	 * /banlist View banlist
	 * /list Display connected users
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
								if (client.username.equals(victim)){
									client.writer.write(ClientManager.YELLOW+"SERVER: "+msg+ClientManager.RESET);
									client.writer.newLine();
									client.writer.flush();
									break;
								}
							}
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
					} else if (command.startsWith("/ban")){
						if (command.split(" ").length == 1){
							System.out.println(ClientManager.RED+"Usage: /ban <username>"+ClientManager.RESET);
						} else {
							String victim = command.split(" ")[1];
							kick(victim, ClientManager.YOU_HAVE_BEEN_BANNED);
							banlist.add(victim);
						}
					} else if (command.startsWith("/list")){
						System.out.println(getUsers());
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
			if (args.length > 0){
				try {
					port = Integer.parseInt(args[0]);
				} catch (NumberFormatException ex) {
					System.out.println("Invalid port!");
					return;
				}
			} else {
				System.out.println("Usage: Server <port>. Using default values");
			}
			
			System.out.println(ClientManager.GREEN+"Server started on localhost at port "+port+ClientManager.RESET);
			ServerSocket ss = new ServerSocket(port);
			Server server = new Server(ss);
			server.listenForCommands();
			server.start();
		} catch (IOException ioe){
			System.out.println("Could not start server: "+ioe.getMessage());
		}
	}
}
