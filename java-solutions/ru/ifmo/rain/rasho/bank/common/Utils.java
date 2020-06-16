package ru.ifmo.rain.rasho.bank.common;

import ru.ifmo.rain.rasho.bank.server.account.LocalAccount;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Utils {

    private static final boolean LOGGING_ENABLED = true;

    public static void log(final String s) {
        if (LOGGING_ENABLED) {
            System.out.println(s);
        }
    }

    public static String getBankAccountId(final int passport, final int accountId) {
        return passport + ":" + accountId;
    }

    public static ConcurrentMap<Integer, Account> convertToLocalAccountsMap(final ConcurrentMap<Integer, Account> accountById)
            throws RemoteException {
        ConcurrentMap<Integer, Account> result = new ConcurrentHashMap<>();
        for (Integer id : accountById.keySet()) {
            result.put(id, new LocalAccount(accountById.get(id)));
        }
        return result;
    }

    public static int getIntegerArgumentSafely(String[] args, int index, String identifier, String usage) {
        int result;
        try {
            result = Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Expected integer number of " + identifier + ", found " + args[index] + ": " + e.getMessage() + '\n' + usage
            );
        }
        return result;
    }
}
