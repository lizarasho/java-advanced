package ru.ifmo.rain.rasho.bank.server;

import ru.ifmo.rain.rasho.bank.common.Bank;
import ru.ifmo.rain.rasho.bank.common.Utils;
import ru.ifmo.rain.rasho.bank.server.bank.RemoteBank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private static final int PORT = 8888;
    private static final String URL = "//localhost/bank";

    public static void main(final String... args) {
        final Bank bank = new RemoteBank(PORT);
        try {
            UnicastRemoteObject.exportObject(bank, PORT);
            Naming.rebind(URL, bank);
        } catch (final RemoteException e) {
            throw new RuntimeException("Cannot export object", e);
        } catch (final MalformedURLException e) {
            throw new RuntimeException("Malformed URL " + URL, e);
        }
        Utils.log("Server started");
    }
}
