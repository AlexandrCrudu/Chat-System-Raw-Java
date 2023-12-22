package utils;

import server.ClientHandler;

public class UsernameFinder {
    public static boolean foundUsername(String username) {
        boolean found = false;
        for (ClientHandler handler:ClientHandler.handlers) {
            if(handler.getUsername().equalsIgnoreCase(username)) {
                found = true;
                break;
            }
        }
        return found;
    }

}
