package edu.hitsz.socketserver;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int socketPort = 8989;
        int authPort = 8080;
        if (args.length >= 1) {
            try {
                socketPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (args.length >= 2) {
            try {
                authPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        int finalSocketPort = socketPort;
        int finalAuthPort = authPort;
        new Thread(() -> new SocketRelayServer().start(finalSocketPort), "socket-relay-server").start();
        new Thread(() -> new AuthHttpServer().start(finalAuthPort), "auth-http-server").start();
        System.out.println("Server Main started. socketPort=" + socketPort + ", authPort=" + authPort);
    }
}
