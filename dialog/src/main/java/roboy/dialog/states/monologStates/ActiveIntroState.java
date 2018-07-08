package roboy.dialog.states.monologStates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import roboy.dialog.states.definitions.State;
import roboy.dialog.states.definitions.StateParameters;
import roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.logic.StatementInterpreter;
import roboy.memory.nodes.Roboy;
import roboy.talk.Verbalizer;

import java.util.Set;

/**
 * Passive state to start a conversation.
 * Roboy is introducing himself autonomously
 *
 */
public class ActiveIntroState extends State {

    private final Logger LOGGER = LogManager.getLogger();

    private State nextState;

    private Roboy roboy;

    public ActiveIntroState(String stateIdentifier, StateParameters params) {
        super(stateIdentifier, params);
    }

    @Override
    public Output act() {

        return Output.say(roboy.getName());
    }

    @Override
    public Output react(Interpretation input) {

        return Output.sayNothing();
    }

    @Override
    public State getNextState() {
        return this;
    }

}