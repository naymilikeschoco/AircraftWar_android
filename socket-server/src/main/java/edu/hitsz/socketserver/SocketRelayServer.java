package edu.hitsz.socketserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketRelayServer {

    public static void main(String[] args) {
        int port = 8989;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        new SocketRelayServer().start(port);
    }

    private final ExecutorService pool = Executors.newCachedThreadPool();

    @SuppressWarnings("InfiniteLoopStatement")
    public void start(int port) {
        System.out.println("Socket relay server starting on port: " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Waiting for player #1...");
                Socket p1 = serverSocket.accept();
                System.out.println("Player #1 connected: " + p1.getRemoteSocketAddress());

                System.out.println("Waiting for player #2...");
                Socket p2 = serverSocket.accept();
                System.out.println("Player #2 connected: " + p2.getRemoteSocketAddress());

                startMatch(p1, p2);
            }
        } catch (IOException e) {
            System.err.println("Server stopped: " + e.getMessage());
        }
    }

    private void startMatch(Socket p1, Socket p2) {
        notifyMatchReady(p1);
        notifyMatchReady(p2);
        pool.execute(() -> relay(p1, p2, "P1->P2"));
        pool.execute(() -> relay(p2, p1, "P2->P1"));
    }

    private void notifyMatchReady(Socket player) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(player.getOutputStream()));
            writer.write("READY");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.out.println("Failed to send READY: " + e.getMessage());
        }
    }

    private void relay(Socket from, Socket to, String tag) {
        try (Socket src = from;
             Socket dst = to;
             BufferedReader reader = new BufferedReader(new InputStreamReader(src.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dst.getOutputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
                writer.flush();
                System.out.println(tag + " " + line);
            }
        } catch (IOException e) {
            System.out.println("Relay ended (" + tag + "): " + e.getMessage());
        }
    }
}
