package flashcards;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        UserInterface ui = new UserInterface();
        Flashcards flashcards = new Flashcards(ui);
        String initialImportFileName = getParamValueFromArgs("-import", args);
        String initialExportFileName = getParamValueFromArgs("-export", args);

        if (!initialImportFileName.isEmpty()) {
            flashcards.importFlashcards(initialImportFileName);
        }

        String action;

        do {
            ui.println("Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):");
            action = ui.nextLine();
            switch (action) {
                case "add":
                    new FlashcardFactory(ui).createFlashcard(flashcards);
                    break;
                case "remove":
                    new FlashcardFactory(ui).removeFlashcard(flashcards);
                    break;
                case "import":
                    flashcards.importFlashcards("");
                    break;
                case "export":
                    flashcards.exportFlashcards("");
                    break;
                case "ask":
                    flashcards.ask();
                    break;
                case "log":
                    ui.saveLog();
                    break;
                case "hardest card":
                    flashcards.displayHardestCards();
                    break;
                case "reset stats":
                    flashcards.resetStats();
                    break;
            }
        } while (!"exit".equals(action));

        System.out.println("Bye bye!");
        if (!initialExportFileName.isEmpty()) {
            flashcards.exportFlashcards(initialExportFileName);
        }
    }

    private static String getParamValueFromArgs(String param, String[] args) {
        for (int ii = 0; ii < args.length - 1; ii++) {
            if (param.equals(args[ii])) {
                return args[ii + 1];
            }
        }
        return "";
    }
}


class FlashcardFactory {
    final private UserInterface ui;

    public FlashcardFactory(UserInterface ui) {
        this.ui = ui;
    }

    public void createFlashcard(Flashcards flashcards) {

        ui.println("The card:");
        String term = ui.nextLine();
        if (flashcards.containsKey(term)) {
            ui.println("The card \"" + term + "\" already exists.");
            return;
        }

        ui.println("The definition of the card:");
        String definition = ui.nextLine();
        if (flashcards.containsValue(definition)) {
            ui.println("The definition \"" + definition + "\" already exists. Try again:");
            return;
        }

        flashcards.put(term, definition);
        ui.println("The pair (\"" + term + "\":\"" + definition + "\") has been added");
    }

    public void removeFlashcard(Flashcards flashcards) {
        ui.println("Which card?");
        String term = ui.nextLine();
        if (flashcards.containsKey(term)) {
            flashcards.remove(term);
            ui.println("The card has been removed.");
        } else {
            ui.println("Can't remove \"" + term + "\": there is no such card.");
        }
    }
}

class Flashcards {
    private final UserInterface ui;

    public Flashcards(UserInterface ui) {
        this.ui = ui;
    }

    final private Map<String, String> flashcards = new LinkedHashMap<>();
    final private Map<String, Integer> mistakes = new LinkedHashMap<>();

    public boolean containsKey(String term) {
        return flashcards.containsKey(term);
    }

    public boolean containsValue(String description) {
        return flashcards.containsKey(description);
    }

    public void put(String term, String definition) {
        flashcards.put(term, definition);
    }

    public void remove(String term) {
        flashcards.remove(term);
        mistakes.remove(term);
    }

    public void exportFlashcards(String fileName) {
        if (fileName.isEmpty()) {
            ui.println("File name:");
            fileName = ui.nextLine();
        }

        try (PrintWriter out = new PrintWriter(fileName)) {
            for (Map.Entry<String, String> flashcard : flashcards.entrySet()) {
                out.println(flashcard.getKey());
                out.println(flashcard.getValue());
                out.println(getMistakesForTerm(flashcard.getKey()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ui.println(flashcards.size() + " cards have been saved.");
    }

    public void importFlashcards(String fileName) {
        if (fileName.isEmpty()) {
            ui.println("File name:");
            fileName = ui.nextLine();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            int counter = 0;
            String term;
            while ((term = br.readLine()) != null) {
                String definition = br.readLine();
                int cardMistakes = Integer.parseInt(br.readLine());
                flashcards.put(term, definition);
                if (cardMistakes > 0) {
                    mistakes.put(term, cardMistakes);
                }
                counter++;
            }
            ui.println(counter + " cards have been loaded.");
        } catch (IOException e) {
            ui.println("File not found.");
        }
    }

    public void ask() {
        ui.println("How many times to ask?");
        int numOfQuestions = Integer.parseInt(ui.nextLine());

        for (int ii = 0; ii < numOfQuestions; ii++) {
            int drawnCard = new Random().nextInt(flashcards.size());
            int cardCounter = 0;
            for (var flashcard : flashcards.entrySet()) {
                cardCounter++;
                if (cardCounter < drawnCard) {
                    continue;
                }

                ui.println("Print the definition of \"" + flashcard.getKey() + "\":");
                String answer = ui.nextLine();
                if (answer.compareTo(flashcard.getValue()) == 0) {
                    ui.println("Correct!");
                } else {
                    addMistake(flashcard.getKey());
                    if (flashcards.containsValue(answer)) {
                        ui.println("Wrong. The right answer is \"" + flashcard.getValue() + "\", " +
                                "but your definition is correct for \"" + getKeyByValue(flashcards, answer) + "\".");
                    } else {
                        ui.println("Wrong. The right answer is \"" + flashcard.getValue() + "\".");
                    }
                }
                break;
            }
        }
    }

    private void addMistake(String term) {
        if (mistakes.containsKey(term)) {
            mistakes.put(term, mistakes.get(term) + 1);
        } else {
            mistakes.put(term, 1);
        }
    }

    private int getMistakesForTerm(String term) {
        return mistakes.getOrDefault(term, 0);
    }

    public void resetStats() {
        mistakes.clear();
        ui.println("Card statistics have been reset.");
    }

    public void displayHardestCards() {
        if (mistakes.size() == 0) {
            ui.println("There are no cards with errors.");
            return;
        }

        // find maxMistakes
        int maxMistakes = 0;
        for (Integer ii : mistakes.values()) {
            maxMistakes = Math.max(ii, maxMistakes);
        }

        // get terms with maxMistakes
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : mistakes.entrySet()) {
            if (entry.getValue() == maxMistakes) {
                list.add("\"" + entry.getKey() + "\"");
            }
        }

        StringBuilder str;
        if (list.size() == 1) {
            str = new StringBuilder("The hardest card is " + list.get(0) + ". You have " + maxMistakes + " errors answering it.");
        } else {
            str = new StringBuilder("The hardest cards are ");
            for (String term : list) {
                str.append(term);
            }
            str.append(". You have ").append(maxMistakes).append(" errors answering them.");
        }
        ui.println(str.toString());
    }


    private String getKeyByValue(Map<String, String> map, String value) {
        for (var entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}

class UserInterface {
    final private List<String> log = new ArrayList<>();

    public void println(String str) {
        System.out.println(str);
        log.add(str);
    }

    public String nextLine() {
        Scanner scanner = new Scanner(System.in);
        String str = scanner.nextLine();
        log.add(str);
        return str;
    }

    public void saveLog() {
        println("File name:");
        String fileName = nextLine();

        try (PrintWriter out = new PrintWriter(fileName)) {
            for (String str : log) {
                out.println(str);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        println("The log has been saved.");
    }
}