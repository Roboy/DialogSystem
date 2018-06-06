package roboy.emotions;

/**
 * Comprises the emotions Roboy can demonstrate
 */
public enum RoboyEmotion {
    SHY("shy"),
    SMILE_BLINK("smileblink"),
    LOOK_LEFT("lookleft"),
    LOOK_RIGHT("lookright"),
    CAT_EYES("catiris"),
    KISS("kiss"),
    FACEBOOK_EYES("img:facebook");

    public String type;

    RoboyEmotion(String type) {
        this.type = type;
    }
}
