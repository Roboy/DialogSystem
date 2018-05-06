package roboy.dialog;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import roboy.context.Context;
import roboy.context.ContextGUI;
import roboy.dialog.action.Action;
import roboy.dialog.personality.StateBasedPersonality;
import roboy.io.*;
import roboy.linguistics.sentenceanalysis.*;
import roboy.memory.Neo4jMemory;
import roboy.memory.DummyMemory;
import roboy.memory.Neo4jMemoryInterface;
import roboy.memory.nodes.Interlocutor;
import roboy.ros.RosMainNode;
import roboy.talk.Verbalizer;
import roboy.util.ConfigManager;
import roboy.util.IO;
import roboy.util.TelegramBotBoyPolling;
import sun.security.krb5.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary class to test new state based personality.
 * Will be be extended and might replace the old DialogSystem in the future.
 */
public class DialogSystem {

    private final static Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws IOException {

        // initialize telegram bot
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotApi = new TelegramBotsApi();
        try {

            telegramBotApi.registerBot(TelegramBotBoyPolling.getInstance());
        } catch (TelegramApiException e) {
            logger.error("Telegram bots api error: ", e);

        }//end catch()
        // initialize ROS node

        RosMainNode rosMainNode;

        if (ConfigManager.ROS_ENABLED) {
            rosMainNode = new RosMainNode();
        } else {
            // TODO: create a nice offline interface for RosMainNode, similar to DummyMemory
            rosMainNode = null;
        }

        // create input and output devices and initialize them with telegram bots api
        //TODO: this is hardcoded
        BotBoyInput botBoyInput = new BotBoyInput();
        BotBoyOutput botBoyOutput = new BotBoyOutput();

//        TelegramBotBoyPolling.getInstance().addInputListener(botBoyInput);
//        TelegramBotBoyPolling.getInstance().addOutputListener(botBoyOutput);

        MultiInputDevice multiIn = new MultiInputDevice(botBoyInput);
        MultiOutputDevice multiOut = new MultiOutputDevice(botBoyOutput);


        // ------------------------------ OLD ------------------------------
//        MultiInputDevice multiIn = IO.getInputs(rosMainNode);
//        MultiOutputDevice multiOut = IO.getOutputs(rosMainNode);
        // ------------------------------ OLD ------------------------------


        // TODO deal with memory
        Neo4jMemoryInterface memory;
        if (ConfigManager.ROS_ENABLED && ConfigManager.ROS_ACTIVE_PKGS.contains("roboy_memory")) {
            memory = new Neo4jMemory(rosMainNode);
        }
        else {
            memory = new DummyMemory();
        }

        if(ConfigManager.CONTEXT_GUI_ENABLED) {
            final Runnable gui = () -> ContextGUI.run();
            Thread t = new Thread(gui);
            t.start();
        }

        logger.info("Initializing analyzers...");

        List<Analyzer> analyzers = new ArrayList<>();

        // Do not disable the following two analyzers!
        // They allow simple states to work without running SemanticParserAnalyzer
        analyzers.add(new Preprocessor());
        analyzers.add(new SimpleTokenizer());

        analyzers.add(new SemanticParserAnalyzer(ConfigManager.PARSER_PORT));
        //analyzers.add(new OpenNLPPPOSTagger());
        analyzers.add(new DictionaryBasedSentenceTypeDetector());
        //analyzers.add(new SentenceAnalyzer());
        analyzers.add(new OpenNLPParser());
        //analyzers.add(new OntologyNERAnalyzer());
        analyzers.add(new AnswerAnalyzer());


        logger.info("Creating StateBasedPersonality...");

        StateBasedPersonality personality = new StateBasedPersonality(rosMainNode, memory, new Verbalizer());
        File personalityFile = new File(ConfigManager.PERSONALITY_FILE);

        // Repeat conversation a few times
        for (int numConversations = 0; numConversations < 3; numConversations++) {

            logger.info("############## New Conversation ##############");

            // flush the interlocutor
            Interlocutor person = new Interlocutor(memory);
            Context.getInstance().ACTIVE_INTERLOCUTOR_UPDATER.updateValue(person);

            try {
                // create "fresh" State objects using loadFromFile() at the beginning of every conversation
                // otherwise some states (with possibly bad implementation) will keep the old internal variables
                personality.loadFromFile(personalityFile);

            } catch (FileNotFoundException e) {
                logger.error("Personality file not found: " + e.getMessage());
                return;
            }

            List<Action> actions = personality.startConversation();

            while (true) {
                // do all actions defined in startConversation() or answer()
                multiOut.act(actions);

                // now stop if conversation ended
                if (personality.conversationEnded()) {
                    break;
                }

                // listen to interlocutor if conversation didn't end
                Input raw;
                try {
                    raw = multiIn.listen();
                    logger.error("This is good");
                } catch (Exception e) {
                    logger.error("Exception in input: " + e.getMessage());
                    return;
                }

                // analyze
                Interpretation interpretation = new Interpretation(raw.sentence, raw.attributes);
                for (Analyzer a : analyzers) {
                    try {
                        interpretation = a.analyze(interpretation);
                    } catch (Exception e) {
                        logger.error("Exception in analyzer " + a.getClass().getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // answer
                try {
                    actions = personality.answer(interpretation);
                } catch (Exception e) {
                    logger.error("Error in personality.answer: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            logger.info("############# Reset State Machine ############");
            // now reset --> conversationEnded() will now return false --> new conversation possible
            personality.reset();

        }
    }
}
