package lab3;

import lab3.text.ConsoleRunner;

import javafx.application.Application;
import lab3.gui.MainApp;
import lab3.text.ConsoleRunner;

import java.util.Arrays;

public class Main
{
    public static void main(String[] args)
    {
        boolean guiRequested = Arrays.stream(args)
                .map(String::toLowerCase)
                .anyMatch(arg -> arg.equals("gui"));

        boolean textRequested = Arrays.stream(args)
                .map(String::toLowerCase)
                .anyMatch(arg -> arg.equals("text"));

        if (textRequested) {
            new ConsoleRunner().run();
            return;
        }

        Application.launch(MainApp.class, args);
    }
}