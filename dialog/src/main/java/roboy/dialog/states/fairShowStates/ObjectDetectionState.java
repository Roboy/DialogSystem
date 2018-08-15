package roboy.dialog.states.fairShowStates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import roboy.dialog.states.definitions.MonologState;
import roboy.dialog.states.definitions.State;
import roboy.dialog.states.definitions.StateParameters;
import roboy.emotions.RoboyEmotion;
import roboy.ros.RosMainNode;
import roboy.util.RandomList;

import java.util.*;


enum Objects {

    AEROPLANE {

        private RandomList<String> phrases = new RandomList<>("an aeroplane. Let's fly to Spain and spend a day at the beach.", "an aeroplane. Are you ready for take-off?", "an aeroplane. Please fasten you seatbelts, Ladies and Gentlemen!");

        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement()).setEmotion(RoboyEmotion.HEARTS);

        }

    },
    BYCICLE {

        private RandomList<String> phrases = new RandomList<>("a bycicle, I bet I can ride faster than you", "a bike, I could easily win the tour de France", "a bycicle, I learnt riding the bycicle when I was just three years old. And you?", "bike. I love bycicles. I only fell off my bike once.");

        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement()).setEmotion(happyEmotions.getRandomElement());

        }

    },
    BOTTLE {

        private RandomList<String> phrases = new RandomList<>("a bottle, it would be awesome to drink a cold beer.", "a drink. In a few years from now, I want to be a bartender" , "a beverage. In the morning, I love coffee, in the evening, I drink beer", "a drink, chin-chin", "a bottle. Cheers, enjoy your drink.");

        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement()).setEmotion(happyEmotions.getRandomElement());

        }
    },
    CAR {

        private RandomList<String> phrases = new RandomList<>("car, in a few years, my fellow robots will be able to ride your car", "car. In Germany, you can go as fast as you want on the highway. I was really fast once." , "car. The cars we drive say a lot about us.");

        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement());

        }
    },
    CHAIR {

        private RandomList<String> phrases = new RandomList<>("a chair, grab a seat if you want." , "a chair, have you ever played musical chairs? I love this game." , "a chair. Sometimes, I am really, really lazy. I just sit on a chair and do nothing but relaxing. It is so comfortable" , "a chair. For now, I would prefer sitting on a beach chair close to the sea.");

        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement()).setEmotion(happyEmotions.getRandomElement());

        }

    },
    DIGNINGTABLE {
        private RandomList<String> phrases = new RandomList<>("a table, I am kind of hungry. I can hardly wait for my next meal" , "a table. We need some chairs and food and we can have a great dinner.");

        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement()).setEmotion(RoboyEmotion.TEETH);

        }
    },
    DOG {

        private RandomList<String> phrases = new RandomList<>("a dog. Sometimes they bark at me. I think they are afraid." , "a dog. When I was a robot kid, I loved dogs, but now I am scared of them." , "a dog. I hope he doesn't bite me. I am a bit afraid of big dogs.");

        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement()).setEmotion(RoboyEmotion.ROLLING_EYES);

        }
    },
    SOFA {

        private RandomList<String> phrases = new RandomList<>("a sofa. We need a TV and a good movie and we could have an awesome time together." , "a sofa. I love sofas. They are so relaxing.");


        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement()).setEmotion(RoboyEmotion.SHY);

        }
    },
    TVMONITOR {

        private RandomList<String> phrases = new RandomList<>("a mobile phone. Follow me on Facebook, Instagram or LinkedIn. You will not regret it." , "a mobile phone. Come on, take a picture with me.");
        private RandomList<RoboyEmotion> socialMediaEmotions = new RandomList<>(RoboyEmotion.FACEBOOK_EYES, RoboyEmotion.INSTAGRAM_EYES, RoboyEmotion.LINKED_IN_EYES);

        @Override
        public State.Output performSpecialAction(RosMainNode rmn){

            return State.Output.say(connectingPhrases.getRandomElement() + phrases.getRandomElement()).setEmotion(socialMediaEmotions.getRandomElement());

        }
    };

    public RandomList<String> connectingPhrases = new RandomList<>(" and look there, ", " and over there i can see ", " oh see, ", " this is a really nice ");
    public RandomList<RoboyEmotion> happyEmotions = new RandomList<>(RoboyEmotion.SMILE_BLINK, RoboyEmotion.HAPPY, RoboyEmotion.SURPRISED, RoboyEmotion.TEETH);

    public abstract State.Output performSpecialAction(RosMainNode rosNode);

}

/**
 * Passive state to react on detected Objects
 */
public class ObjectDetectionState extends MonologState {

    private final static String TRANSITION_FINISHED = "finished";

    private final Set<Objects> detectedObjects = new HashSet<>();

    private Vector<Output> outputs = new Vector<>();

    private final Logger logger = LogManager.getLogger();

    private State nextState = this;

    public ObjectDetectionState(String stateIdentifier, StateParameters params) {
        super(stateIdentifier, params);
    }

    @Override
    public Output act() {

        if(outputs.isEmpty()){

            checkObjects();

            for(Objects obj : detectedObjects) {

                outputs.add(obj.performSpecialAction(getRosMainNode()));

            }
        }

        Output current = outputs.remove(0);
        if(outputs.isEmpty()) nextState = getTransition(TRANSITION_FINISHED);

        return current;
    }

    @Override
    public State getNextState() {
        return nextState;
    }


    /**
     * fetches detected objects from vision and writes into a list
     */
    private void checkObjects(){

        try {
            List<String> detectedThings = getContext().OBJECT_DETECTION.getLastValue().getNames();
            for (String obj : detectedThings) {
                detectedObjects.add(Objects.valueOf(obj.toUpperCase()));
            }
        }catch (NullPointerException e){
            nextState = getTransition(TRANSITION_FINISHED);
            logger.error("Make sure object detection publisher is running, receiving: " + e.getMessage());
        }

    }

}