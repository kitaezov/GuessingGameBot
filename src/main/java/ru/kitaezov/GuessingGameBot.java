package ru.kitaezov;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class GuessingGameBot extends TelegramLongPollingBot {
    private static final int MESSAGE_DELETE_DELAY = 60000;
    private Game game;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            String text = message.getText();

            if (text.startsWith("/start")) {
                handleStartCommand(chatId, text.split("\\s+"));
            } else if (text.equalsIgnoreCase("/stop")) {
                handleStopCommand(chatId);
            } else {
                handleGuessCommand(chatId, text, message);
            }
        }
    }

    @NotNull
    private void handleStartCommand(String chatId, String[] command) {
        if (command.length < 2) {
            sendMessageWithDeletion(chatId, "🔢 Укажите лимит (100, 200, 300, ... , 1000).", MESSAGE_DELETE_DELAY);
            return;
        }

        try {
            int limit = Integer.parseInt(command[1]);
            if (limit < 100 || limit > 1000 || limit % 100 != 0) {
                sendMessageWithDeletion(chatId, "⚠️ Лимит должен быть кратным 100 и находиться в пределах от 100 до 1000.", MESSAGE_DELETE_DELAY);
                return;
            }

            game = new Game(limit);
            sendMessageWithDeletion(chatId, "🎮 Игра началась! Попробуйте угадать числа от 1 до " + (limit - 1) + ".", MESSAGE_DELETE_DELAY);
        } catch (NumberFormatException e) {
            logError("Ошибка лимита", e);
            sendMessageWithDeletion(chatId, "⚠️ Некорректный лимит. Укажите число (100, 200, 300, ... , 1000).", MESSAGE_DELETE_DELAY);
        }
    }

    private void handleGuessCommand(String chatId, String text, Message message) {
        if (!Optional.ofNullable(game).map(Game::isActive).orElse(false)) {
            sendMessageWithDeletion(chatId, "❗ Нет активной игры. Используйте /start, чтобы начать новую игру.", MESSAGE_DELETE_DELAY);
            return;
        }

        try {
            int guessedNumber = Integer.parseInt(text);
            int maxNumber = game.getLimit() - 1;
            if (guessedNumber < 1 || guessedNumber > maxNumber) {
                sendMessageWithDeletion(chatId, "⚠️ Число должно быть в диапазоне от 1 до " + maxNumber + ".", MESSAGE_DELETE_DELAY);
                return;
            }

            String userTag = getUserTag(message.getFrom());

            if (game.guessNumber(guessedNumber)) {
                sendMessageWithDeletion(chatId, "🎉 Правильно! " + userTag + " угадал(а) число " + guessedNumber + "!", MESSAGE_DELETE_DELAY);
            } else {
                sendMessageWithDeletion(chatId, "❌ Неправильное число или уже угаданное. Попробуйте снова.", MESSAGE_DELETE_DELAY);
            }

            if (game.allNumbersGuessed()) {
                game.stopGame();
                sendMessage(chatId, "🏆 Все числа угаданы!\nОтличная работа, команда!");
            }

        } catch (NumberFormatException e) {
            logError("Ошибка формата числа", e);
        }
    }

    private void handleStopCommand(String chatId) {
        if (!Optional.ofNullable(game).map(Game::isActive).orElse(false)) {
            sendMessageWithDeletion(chatId, "❗ Нет активной игры для остановки.", MESSAGE_DELETE_DELAY);
            return;
        }

        Set<Integer> numbersToGuess = game.getNumbersToGuess();
        sendMessageWithDeletion(chatId, "🛑 Игра остановлена.\nЗагаданные числа были: " + numbersToGuess, MESSAGE_DELETE_DELAY);
        game.stopGame();
    }

    @Override
    public String getBotUsername() {
        return "Название бота, без @";
    }

    @Override
    public String getBotToken() {
        return "Токен бота";
    }

    private void sendMessageWithDeletion(String chatId, String text, int delay) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            Message sentMessage = execute(message);
            deleteMessageWithDelay(chatId, sentMessage.getMessageId(), delay);
        } catch (TelegramApiException e) {
            logError("Ошибка при отправки сообщения!", e);
        }
    }

    private void deleteMessageWithDelay(String chatId, Integer messageId, int delay) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    execute(new DeleteMessage(chatId, messageId));
                } catch (TelegramApiException e) {
                    logError("Ошибка при удалении сообщения!", e);
                }
            }
        }, delay);
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logError("Ошибка при отправки сообщения!", e);
        }
    }

    private String getUserTag(User user) {
        return (user.getUserName() != null)
                ? "@" + user.getUserName()
                : user.getFirstName();
    }

    private void logError(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();
    }
}
