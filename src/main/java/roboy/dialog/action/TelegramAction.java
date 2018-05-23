package roboy.dialog.action;

/**
 * Action used for telegram conversations
 */
public class TelegramAction implements Action{
    private String text;
    private String chatID;
    /**
     * Constructor.
     *
     * @param text The text Roboy will utter
     */
    public TelegramAction(String text, String chatID){
        this.text = text;
        this.chatID = chatID;
    }

    public String getText(){
        return text;
    }

    public String getChatID(){
        return chatID;
    }
}
