package org.roboy.dialog.states.devoStates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roboy.dialog.states.definitions.State;
import org.roboy.dialog.states.definitions.StateParameters;
import org.roboy.linguistics.sentenceanalysis.Interpretation;
import org.roboy.ontology.Neo4jRelationship;
import org.roboy.memory.models.nodes.Interlocutor;
import org.roboy.util.QAJsonParser;

import java.util.Set;

import static org.roboy.ontology.Neo4jRelationship.*;

/**
 * Personal Information Asking State
 *
 * The state tries to interact with the InterlocutorModel to learn new information about the person.
 * This information is sent to the RoboyModel Memory Module through Neo4jMemoryInterface for storing.
 * Afterwards, RoboyModel can use this acquired data for the future interactions with the same person.
 *
 * - if there is no existing InterlocutorModel or the data is missing, ask a question
 * - the question topic (intent) is selected from the Neo4jRelationship predicates
 * - retrieve the questions stored in the QAList json file
 * - update the Context IntentsHistory
 * - try to extract the result from the Interpretation
 * - retrieve the answers stored in the QAList json file
 * - send the result to Memory
 *
 * PersonalInformationAskingState interface:
 * 1) Fallback is not required.
 * 2) Outgoing transitions that have to be defined:
 *    - TRANSITION_INFO_OBTAINED:    following state if the question was asked
 * 3) Required parameters: path to the QAList.json file.
 */
public class PersonalInformationAskingState extends State {
    private QAJsonParser qaValues;
    private Neo4jRelationship[] predicates = { FROM, HAS_HOBBY, WORK_FOR, STUDY_AT };
    private Neo4jRelationship selectedPredicate;
    private State nextState;

    private final String TRANSITION_INFO_OBTAINED = "questionAnswering";
    private final String QA_FILE_PARAMETER_ID = "qaFile";
    final Logger LOGGER = LogManager.getLogger();

    public final static String INTENTS_HISTORY_ID = "PIA";

    public PersonalInformationAskingState(String stateIdentifier, StateParameters params) {
        super(stateIdentifier, params);
        String qaListPath = params.getParameter(QA_FILE_PARAMETER_ID);
        LOGGER.info(" -> The QAList path: " + qaListPath);
        qaValues = new QAJsonParser(qaListPath);
    }

    @Override
    public Output act() {
        Interlocutor person = getContext().ACTIVE_INTERLOCUTOR.getValue();
        LOGGER.info(" -> Retrieved InterlocutorModel: " + person.getName());

        return Output.say("What is my purpose?");
    }

    @Override
    public Output react(Interpretation input) {
        nextState = getTransition(TRANSITION_INFO_OBTAINED);
        return Output.say("Oh. My. God.");
    }

    @Override
    public State getNextState() {
        return nextState;
    }

    @Override
    protected Set<String> getRequiredTransitionNames() {
        // optional: define all required transitions here:
        return newSet(TRANSITION_INFO_OBTAINED);
    }

    @Override
    protected Set<String> getRequiredParameterNames() {
        return newSet(QA_FILE_PARAMETER_ID);
    }

    private String InferResult(Interpretation input) {
        return getInference().inferRelationship(selectedPredicate, input);
    }
}