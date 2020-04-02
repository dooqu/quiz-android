package com.dooqu.quiz.service;

import java.util.ArrayList;
import java.util.List;


public class Command {
    protected String name;
    protected List<String> arguments;


    public Command(String name, String[] args, int offset, int length) {
        arguments = new ArrayList();
        this.name = name;
        if (args != null && args.length > 1) {
            for (int i = offset, count = 0; count < length; count++, i++) {
                arguments.add(args[i]);
            }
        }
    }


    public static Command parse(String commandString) {
        if (commandString == null || "".equals(commandString)) {
            return null;
        }

        String[] args = commandString.split(" ");
        if (args.length >= 1) {
            return new Command(args[0], args, 1, args.length - 1);
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    public String getArgumentAt(int index) {
        if (index < 0 || index >= arguments.size()) {
            throw new IndexOutOfBoundsException();
        }

        return arguments.get(index);
    }
}
