package roboy.dialog.personality.states;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import roboy.context.Context;
import roboy.linguistics.Linguistics;
import roboy.linguistics.Linguistics.SEMANTIC_ROLE;
import roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.memory.Neo4jMemory;
import roboy.memory.Neo4jMemoryInterface;
import roboy.memory.Neo4jRelationships;
import roboy.memory.nodes.Interlocutor;
import roboy.memory.nodes.MemoryNodeModel;
import roboy.util.Lists;

/**
 * Roboy introduces himself and asks "Who are you?". Moves to success state if the answer
 * is at most 2 words.
 */
public class IntroductionState extends AbstractBooleanState{

	Interlocutor person;
    Neo4jMemoryInterface memory;
    public Neo4jRelationships predicate = Neo4jRelationships.FRIEND_OF;

	private static final List<String> introductions = Lists.stringList(
			"I am Roboy. Who are you?",
			"My name is Roboy. What is your name?"
			);
	
	public IntroductionState(Neo4jMemoryInterface memory) {
		setFailureTexts(Lists.stringList(
				"It's always nice to meet new people.",
				"How refreshing to see a new face."));
		this.person = Context.getInstance().ACTIVE_INTERLOCUTOR.getValue();
		this.memory = memory;
	}
	
	@Override
	public List<Interpretation> act() {
		return Lists.interpretationList(new Interpretation(introductions.get((int)(Math.random()*introductions.size()))));
	}

	/**
	 * Performs person detection by consulting memory.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected boolean determineSuccess(Interpretation input) {
		Context.getInstance().ACTIVE_INTERLOCUTOR_UPDATER.updateValue(person);

		String[] tokens = (String[]) input.getFeatures().get(Linguistics.TOKENS);
		String name = null;
		if(tokens.length==1){
			name = tokens[0];
		} else {
			Map<SEMANTIC_ROLE,Object> pas = (Map<SEMANTIC_ROLE,Object>) input.getFeature(Linguistics.PAS);
			if(pas==null || !pas.containsKey(SEMANTIC_ROLE.PREDICATE)) return false;
			String predicate = ((String)pas.get(SEMANTIC_ROLE.PREDICATE)).toLowerCase();
			String agent = (String)pas.get(SEMANTIC_ROLE.AGENT);
			String patient = (String)pas.get(SEMANTIC_ROLE.PATIENT);
			// if(agent==null) agent = "i";
			// TODO Handle cases where name could not be parsed.
			// Maybe something like "I did not quite get your name, could you repeat it."
			// When using a default value with persistent memory, Roboy will always recognize them.
			if(patient==null) agent = "laura";
			// if(!"am".equals(predicate) && !"is".equals(predicate)) return false;
			// if(!agent.toLowerCase().contains("i") && !agent.toLowerCase().contains("my")) return false;
			name = patient;
		}
		if(name!=null){
//			WorkingMemory.getInstance().save(new Triple("is","name",name));
//			List<Triple> subject = PersistentKnowledge.getInstance().retrieve(new Triple(null,name,null));
//			List<Triple> object = PersistentKnowledge.getInstance().retrieve(new Triple(null,null,name));
			//TODO Currently assuming no duplicate names in memory. Support for last name addition needed.
            String retrievedResult = "";
            person.addName(name);
            if(!person.FAMILIAR) {
                return false;
            } else {
                ArrayList<Integer> ids = person.getRelationships(predicate);
                if (ids != null && !ids.isEmpty()) {
                    try {
                        for (int i = 0; i < ids.size() && i < 3; i++) {
                            MemoryNodeModel requestedObject = new MemoryNodeModel(memory);
							requestedObject.fromJSON(memory.getById(ids.get(i)), new Gson());
                            retrievedResult += requestedObject.getProperties().get("name").toString();
                            retrievedResult += " and ";
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
				}
                if (!retrievedResult.equals("")) {
                    retrievedResult = "By the way I know you are friends with " + retrievedResult.substring(0, retrievedResult.length() - 5);
                }
            }
            setSuccessTexts(Lists.stringList(
                    "Oh hi, " + name + ". Sorry, I didn't recognize you at first. But you know how the vision guys are. " + retrievedResult,
                    "Hi " + name + " nice to see you again. " + retrievedResult
            ));
            return true;
		}
		return false;
	}

}
