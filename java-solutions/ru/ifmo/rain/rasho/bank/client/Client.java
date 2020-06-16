package ru.ifmo.rain.rasho.bank.client;

import ru.ifmo.rain.rasho.bank.common.Account;
import ru.ifmo.rain.rasho.bank.common.Bank;
import ru.ifmo.rain.rasho.bank.common.Utils;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public class Client {

    private static final String URL = "//localhost/bank";
    private static final String MAIN_USAGE = "Usage: Client <name> <surname> <passport> <account id> <amount of change>";

    private static Bank getBank() {
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup(URL);
        } catch (NotBoundException e) {
            throw new RuntimeException("Bank is not bound", e);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid bank URL " + URL, e);
        } catch (RemoteException e) {
            throw new RuntimeException("Remote exception during bank initializing occurred", e);
        }
        return bank;
    }

    private static void processPerson(final Bank bank, final String name, final String surname,
                                      final int passport, final int accountId, final int amount) {
        try {
            Account account = bank.getAccount(passport, accountId);
            if (account == null) {
                Utils.log("Creating account " + Utils.getBankAccountId(passport, accountId)
                        + " of " + name + ' ' + surname);
                account = bank.createAccount(name, surname, passport, accountId);
                Utils.log("Bank account was successfully created.");
            } else {
                Utils.log("Updating account balance....");
                account.addAmount(amount);
                Utils.log("Bank account was successfully updated.");
            }
            Utils.log("Current bank account balance equals " + account.getAmount() + '.');
        } catch (RemoteException e) {
            throw new RuntimeException("Remote exception during person bank account processing occurred", e);
        }
    }

    public static void main(final String... args) {
        if (args == null) {
            throw new IllegalArgumentException("Expected non-null arguments. " + MAIN_USAGE);
        }
        if (args.length != 5) {
            throw new IllegalArgumentException("Expected 5 arguments, found " + args.length + ". " + MAIN_USAGE);
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("All arguments must be non-null. " + MAIN_USAGE);
        }

        Bank bank = getBank();

        final String name = args[0];
        final String surname = args[1];
        final int passport = Utils.getIntegerArgumentSafely(args, 2, "passport", MAIN_USAGE);
        final int accountId = Utils.getIntegerArgumentSafely(args, 3, "account id", MAIN_USAGE);
        final int amount = Utils.getIntegerArgumentSafely(args, 4, "amount of change", MAIN_USAGE);

        processPerson(bank, name, surname, passport, accountId, amount);

    }
}
