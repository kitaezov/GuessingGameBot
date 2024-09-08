package ru.kitaezov;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Game {
    private final int limit;
    private final Set<Integer> numbersToGuess;
    private final Set<Integer> guessedNumbers;

    public Game(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Предел должен быть больше 0");
        }
        this.limit = limit;
        this.numbersToGuess = Collections.synchronizedSet(generateNumbers());
        this.guessedNumbers = Collections.synchronizedSet(new HashSet<>());
    }

    public int getLimit() {
        return limit;
    }

    public Set<Integer> getNumbersToGuess() {
        return Collections.unmodifiableSet(numbersToGuess);
    }

    public boolean isActive() {
        return !numbersToGuess.isEmpty();
    }

    public synchronized boolean guessNumber(int number) {
        if (numbersToGuess.contains(number)) {
            numbersToGuess.remove(number);
            guessedNumbers.add(number);
            return true;
        }
        return false;
    }

    public boolean allNumbersGuessed() {
        return numbersToGuess.isEmpty();
    }

    public void stopGame() {
        numbersToGuess.clear();
    }

    private Set<Integer> generateNumbers() {
        Random random = new Random();
        Set<Integer> numbers = new HashSet<>();
        while (numbers.size() < 15) {
            numbers.add(random.nextInt(limit - 1) + 1);
        }
        return numbers;
    }
}