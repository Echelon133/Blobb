package ml.echelon133.blobb.user;

import java.util.List;

public class InvalidUserDetailsFieldException extends Exception {
    private List<String> messages;

    public InvalidUserDetailsFieldException(List<String> messages) {
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
