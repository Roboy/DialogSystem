package org.roboy.dialog.personality;

import java.util.List;

import org.roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.dialog.action.Action;
import roboy.linguistics.sentenceanalysis.Interpretation;

/**
 * Personality interface. A personality is designed to define how RoboyModel reacts in every
 * given situation. RoboyModel can always only represent one personality at a time. Different
 * personalities are meant to be used in different situations, like a more formal or loose
 * one depending on the occasion where he is at. In the future, also different languages
 * could be realized by the use of different personalities.
 */
public interface Personality {

	/**
	 * The central method of a personality. Given an interpretation of all inputs
	 * (audio, visual, ...) to RoboyModel, this method decides which actions to perform
	 * in response.
	 * 
	 * @param input The interpretation of the inputs
	 * @return A list of actions to perform in response
	 */
	List<Action> answer(Interpretation input);
}
