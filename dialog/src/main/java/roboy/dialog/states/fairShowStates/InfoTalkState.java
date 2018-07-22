package roboy.dialog.states.fairShowStates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import roboy.dialog.states.definitions.State;
import roboy.dialog.states.definitions.StateParameters;
import roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.memory.nodes.Roboy;

/**
 * Passive state to start a conversation.
 * Roboy is introducing himself autonomously
 *
 */
public class InfoTalkState extends State {

    private final Logger LOGGER = LogManager.getLogger();

    private State nextState;

    private Roboy roboy;

    public InfoTalkState(String stateIdentifier, StateParameters params) {
        super(stateIdentifier, params);
    }

    @Override
    public Output act() {

        return Output.say(getContext().PERSON_DETECTION.getLastNValues(5).toString());
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
