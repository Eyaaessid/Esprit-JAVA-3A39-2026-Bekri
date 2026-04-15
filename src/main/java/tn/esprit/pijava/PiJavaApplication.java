package tn.esprit.pijava;

import com.sun.net.httpserver.HttpServer;
import tn.esprit.pijava.config.AppContext;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PiJavaApplication {

    public static void main(String[] args) throws IOException {
        AppContext appContext = new AppContext();
        int port = appContext.getProperties().getInt("server.port", 8080);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/objectifs", appContext.getObjectifController());
        server.createContext("/api/questions", appContext.getQuestionController());
        server.setExecutor(null);
        server.start();

        System.out.println("HTTP server started on port " + port);
    }

}
