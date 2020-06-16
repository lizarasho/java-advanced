package ru.ifmo.rain.rasho.bank.server.account;

public class RemoteAccount extends AbstractAccount {

    public RemoteAccount(final int id, final int amount) {
        super(id, amount);
    }

    public RemoteAccount(final int id) {
        super(id);
    }

}
