package io.ilyaTGbot.demoSpringBot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vdurmont.emoji.EmojiParser;
import io.ilyaTGbot.demoSpringBot.config.BotConfig;
import io.ilyaTGbot.demoSpringBot.model.Joke;
import io.ilyaTGbot.demoSpringBot.model.JokeRepository;
import io.ilyaTGbot.demoSpringBot.model.User;
import io.ilyaTGbot.demoSpringBot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;


@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    static final int MAX_JOKE_ID_MINUS_ONE = 3772;
    static final String NEXT_JOKE = "NEXT_JOKE";
    @Autowired
    private BotConfig config;

    static final String ERROR_TEXT = "Error occurred: ";
    static final String RATING = "RATING";
    static final String CATEGORY = "CATEGORY";

    static final String HELP_TEXT = "This bot is created to send a random joke from the database each time you request it.\n\n" +
            "You can execute commands from the main menu on the left or by typing commands manually\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /joke to get a random joke\n\n" +
            "Type /settings to list available settings to configure\n\n" +
            "Type /help to see this message again\n";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JokeRepository jokeRepository;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/joke", "get a random joke"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

    }

    @Override
    public String getBotUsername() {
        return config.getBotUserName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            // Set variables
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();


            if (messageText.contains("/send") && chatId == config.getOwnerId()) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }

            } else {
                switch (messageText) {
                    case "/start" -> {
                        registerUser(update.getMessage());
                        showStart(chatId, update.getMessage().getChat().getFirstName());
                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            TypeFactory typeFactory = objectMapper.getTypeFactory();
                            List<Joke> jokeList = objectMapper.readValue(new File("db/stupidstuff.json"),
                                    typeFactory.constructCollectionType(List.class, Joke.class));
                            jokeRepository.saveAll(jokeList);
                        } catch (IOException e) {
                            log.error(Arrays.toString(e.getStackTrace()));
                        }
                    }
                    case "/settings" -> setSetting(chatId);
                    case "/help" -> prepareAndSendMessage(chatId, HELP_TEXT);
                    case "/joke" -> {
                        var joke = getRandomJoke();
                        joke.ifPresent(randomJoke -> addButtonAndSendMessage(randomJoke.getBody(), chatId));
                    }
                    default -> commandNotFound(chatId);
                }
            }
            //Add button to "Setting"
//        } else if (update.hasCallbackQuery()) {
//            String callbackData = update.getCallbackQuery().getData();
//            long messageId = update.getCallbackQuery().getMessage().getMessageId();
//            long hatId = update.getCallbackQuery().getMessage().getChatId();
//            EditMessageText message = new EditMessageText();
//            if (callbackData.equals(CATEGORY)) {
//                String text = "You pressed CATEGORY button";
//                executeEditMessageText(text, hatId, messageId);
//            } else if (callbackData.equals(RATING)) {
//                String text = "You pressed RATING button";
//                executeEditMessageText(text, hatId, messageId);
//            }
        } else if (update.hasCallbackQuery()) {

            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(NEXT_JOKE)) {
                var joke = getRandomJoke();
                joke.ifPresent(randomJoke -> addButtonAndSendMessage(randomJoke.getBody(), chatId));


            }

        }
    }

    private void addButtonAndSendMessage(String joke, long chatId) {
        SendMessage message = new SendMessage();
        message.setText(joke);
        message.setChatId(chatId);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var inlinekeyboardButton = new InlineKeyboardButton();
        inlinekeyboardButton.setCallbackData(NEXT_JOKE);
        inlinekeyboardButton.setText(EmojiParser.parseToUnicode("next joke " + ":rolling_on_the_floor_laughing:"));
        rowInline.add(inlinekeyboardButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        send(message);
    }

    private void send(SendMessage msg) {
        try {
            execute(msg); // Sending our message object to user
        } catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }


    private void setSetting(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("You can change the category or rating of the joke. Which parameter do you want to change?");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var categoryButton = new InlineKeyboardButton();
        categoryButton.setText("CATEGORY");
        categoryButton.setCallbackData(CATEGORY);

        var ratingButton = new InlineKeyboardButton();
        ratingButton.setText("RATING");
        ratingButton.setCallbackData(RATING);
        //View buttons
        rowInline.add(categoryButton);
        rowInline.add(ratingButton);

        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void showStart(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode(
                "Hi, " + name + "! :smile:" + " Nice to meet you! I am a Simple Random Joke Bot created by Pavlov Ilya.\n");
        sendMessage(answer, chatId);
    }

    private Optional<Joke> getRandomJoke() {
        var r = new Random();
        var randomId = r.nextInt(MAX_JOKE_ID_MINUS_ONE) + 1;

        return jokeRepository.findById(randomId);
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void commandNotFound(long chatId) {
        String answer = EmojiParser.parseToUnicode(
                "Command not recognized, please verify and try again :stuck_out_tongue_winking_eye: ");
        sendMessage(answer, chatId);

    }

    private void sendMessage(String textToSend, long chatId) {
        SendMessage message = new SendMessage(); // Create a message object object
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message); // Sending our message object to user
        } catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }
}
