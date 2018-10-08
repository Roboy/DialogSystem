package roboy.dialog.states.gameStates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yecht.Data;
import org.yecht.ruby.Out;
import roboy.dialog.states.definitions.State;
import roboy.dialog.states.definitions.StateParameters;
import roboy.emotions.RoboyEmotion;
import roboy.linguistics.Linguistics;
import roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.logic.Inference;
import roboy.logic.StatementInterpreter;
import roboy.talk.PhraseCollection;
import roboy.talk.Verbalizer;
import roboy.util.QAJsonParser;
import roboy.util.RandomList;

import java.util.*;

import static roboy.util.FileLineReader.readFile;


public class CupGameState extends State {

    private final String PHRASE_FILE_PARAMETER = "cupGamePhrases";
    private QAJsonParser phrases;
    private final static List<String> EXIT_REQUEST = Arrays.asList("bored", "stop", "something else", "different", "boring");
    private final static List<String> READY = Arrays.asList("done", "ready", "let's go", "play");
    private final static String TRANSITION_EXIT = "exit";
    private final Logger LOGGER = LogManager.getLogger();

    private enum CupGamePhase {
        SHUFFLE,
        SCAN,
        GUESS,
        OFFER_REPEAT,
        EXIT
    }


    private final static String FIND_CUPS = "FIND_CUPS";
    private final static String GOTOCUP1 = "GOTOCUP1";
    private final static String GOTOCUP2 = "GOTOCUP2";
    private final static String GOTOCUP3 = "GOTOCUP3";
    private final static String FINISHED = "FINISHED";

    private CupGamePhase phase = CupGamePhase.SHUFFLE;

    public CupGameState(String stateIdentifier, StateParameters params) {

        super(stateIdentifier, params);
        String phraseListPath = params.getParameter(PHRASE_FILE_PARAMETER);
        LOGGER.info(" -> Phrases path: " + phraseListPath);
        phrases = new QAJsonParser(phraseListPath);
    }

    @Override
    public Output act() {
//        switch (phase){
//            case SHUFFLE:
//                return Output.say(phrases.getQuestions(phase.toString()).getRandomElement());
//            case SCAN:
        if (phase == CupGamePhase.SCAN) {
                // TODO get response from the state machine
//                if (getRosMainNode().StartCupGameSmach()) {
                    // if smach start is successful
                    // comment on actions while not finished exploring
                    String prevState = "";
                    String currentSmachState = getContext().CUP_GAME_SMACH_STATE_MSG.getValue();
                    while(!currentSmachState.equals(FINISHED)) {
                        currentSmachState = getContext().CUP_GAME_SMACH_STATE_MSG.getValue();
                        if (!currentSmachState.equals(prevState)) {
                            LOGGER.info("[CupGameState] commenting: " + currentSmachState);
                            switch (getContext().CUP_GAME_SMACH_STATE_MSG.getValue()) {
                                case FIND_CUPS:
                                    getRosMainNode().SynthesizeSpeech(
                                            phrases.getQuestions(getContext().CUP_GAME_SMACH_STATE_MSG.
                                                    getValue()).
                                                    getRandomElement());
                                    break;
                                case GOTOCUP1:
                                    getRosMainNode().ShowEmotion(RoboyEmotion.HYPNO_COLOUR_EYES);

                                    getRosMainNode().SynthesizeSpeech(
                                            phrases.getQuestions(getContext().CUP_GAME_SMACH_STATE_MSG.
                                                    getValue()).
                                                    getRandomElement());
                                    break;
                            }

                            prevState = currentSmachState;
                        }
//                    }
                }
//                return Output.say(phrases.getQuestions(phase.toString()).getRandomElement());
//            case GUESS:
//                return Output.say("I think I found it there. So do I win now?");
//            case OFFER_REPEAT:
//                return Output.say("wanna play again?");
//            default:
//                return Output.sayNothing();
        }
        return Output.say(phrases.getQuestions(phase.toString()).getRandomElement());

    }

    @Override
    public Output react(Interpretation input) {
        exitOnRequest(input.getSentence());
        Boolean pickSuccessAnswer = true;
        CupGamePhase next = CupGamePhase.EXIT;
        switch (phase) {
            case SHUFFLE:
                if (StatementInterpreter.isFromList(input.getSentence(),READY)) {
                    next = CupGamePhase.SCAN;
                    break;
//                    return Output.say("Alright. Let me check with my superpower soli where did you hide the ball");
                }
                else {
                    pickSuccessAnswer = false;
                    break;
//                    return Output.say("Hm, seems you are not done yet. I give you another try");
                }
            case SCAN:
                next = CupGamePhase.GUESS;
                break;
//                return Output.useFallback();
            case GUESS:
                next = CupGamePhase.OFFER_REPEAT;
                break;
//                return Output.say("ha-ha.");
            case OFFER_REPEAT:
                if (StatementInterpreter.isFromList(input.getSentence(),Verbalizer.consent)) {
                    next = CupGamePhase.SHUFFLE;
//                    return Output.say("Here we go again then!");
                }
                else {
                    next = CupGamePhase.EXIT;
                    pickSuccessAnswer = false;
                }
                break;
//                    return Output.say("I believe you've had enough of my magic");
//            case EXIT:
//                return Output.say("Wanna stop playing? Sure, if you say so.");
        }

        Output answer;

        try {
            answer = Output.say(pickSuccessAnswer ? phrases.getSuccessAnswers(phase.toString()).getRandomElement() :
                    phrases.getFailureAnswers(phase.toString()).getRandomElement());
        }
        catch (Exception e) {
            answer = Output.useFallback();
        }

        phase = next;
        return answer;

    }

    private void exitOnRequest(String input) {
        if(StatementInterpreter.isFromList(input, EXIT_REQUEST)) {
            phase = CupGamePhase.EXIT;
        }
    }

    @Override
    public State getNextState() {
        if (phase == CupGamePhase.EXIT) {
            return getTransition(TRANSITION_EXIT);
        }
        else {
            return this;
        }
    }

    @Override
    protected Set<String> getRequiredTransitionNames() {
        return newSet(TRANSITION_EXIT);
    }


}
