package utils;

import java.util.InputMismatchException;
import java.util.Scanner;

public class MenuInput {

    public static int menuChoice(int max, Scanner scanner) {
        int choice = -1;
        while (choice <= 0 || choice > max) {
            System.out.print("- Choose an option: ");
            try {
                choice = scanner.nextInt();
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("- Error: Invalid input");
                scanner.nextLine();
            }
        }
        return choice;
    }

}
