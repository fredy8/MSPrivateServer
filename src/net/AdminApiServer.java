package net;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import net.server.Server;

import java.io.IOException;

public class AdminApiServer implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (httpExchange.getRequestMethod().equals("POST") && httpExchange.getRequestURI().getPath().equals("/nexon")) {
            Server.getInstance().getWorld(0).getPlayerStorage().getCharacterByName("Asdf").getCashShop().gainCash(1, 5000);
            httpExchange.sendResponseHeaders(204, 0);
            httpExchange.getResponseBody().close();
        } else {
            httpExchange.sendResponseHeaders(404, 0);
        }
    }
}
