package com.orangomango.socket;

import java.net.*;
import java.io.*;

public class Client {
	private Socket socket;
	private String username;
	private BufferedReader reader;
	private BufferedWriter writer;
	
	public Client(Socket socket, String username){
		try {
			this.socket = socket;
			this.username = username;
			this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		} catch (IOException e){
			close();
		}
	}
	
	private void close(){
		try {
			System.out.println(ClientManager.RED+"ERROR: Connection lost"+ClientManager.RESET);
			if (this.socket != null){
				this.socket.close();
			}
			if (this.reader != null){
				this.reader.close();
			}
			if (this.writer != null){
				this.writer.close();
			}
			System.exit(0);
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void sendMessage(){
		try {
			writer.write(this.username);
			writer.newLine();
			writer.flush();
			BufferedReader msgReader = new BufferedReader(new InputStreamReader(System.in));
			while (this.socket.isConnected()){
				String message = msgReader.readLine();
				this.writer.write(this.username+": "+message);
				this.writer.newLine();
				this.writer.flush();
			}
		} catch (IOException e){
			e.printStackTrace();
			close();
		}
	}
	
	public void getMessage(){
		new Thread(() -> {
			try {
				while (this.socket.isConnected()){
					String message = this.reader.readLine();
					if (message == null){
						throw new IOException("Server closed");
					}
					System.out.println(message);
				}
			} catch (IOException ex){
				close();
			}
		}).start();
	}
	
	public static void main(String[] args) {
		try {
			int port = 1234;
			String host = "localhost";
			if (args.length > 2){
				try {
					host = args[0];
					port = Integer.parseInt(args[1]);
				} catch (NumberFormatException ex) {
					System.out.println("Invalid port!");
					return;
				}
			} else {
				System.out.println("Usage: Client <host> <port>. Using default values");
			}
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Enter username: ");
			String user = reader.readLine();
			Socket socket = new Socket(host, port);
			System.out.println(ClientManager.GREEN+"Successfully connected to the server, you can start typing"+ClientManager.RESET);
			Client client = new Client(socket, user);
			client.getMessage();
			client.sendMessage();
			reader.close();
		} catch (IOException ioe){
			System.out.println("Could not connect to server: "+ioe.getMessage());
		}
	}
}
