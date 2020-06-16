package ru.ifmo.rain.rasho.bank.server.account;

import ru.ifmo.rain.rasho.bank.common.Account;
import ru.ifmo.rain.rasho.bank.common.Utils;

public abstract class AbstractAccount implements Account {

    private final int id;
    private int amount;

    public AbstractAccount(final int id, final int amount) {
        this.id = id;
        this.amount = amount;
    }

    public AbstractAccount(int id) {
        this(id, 0);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        Utils.log("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        Utils.log("Setting amount of money for account " + id);
        this.amount = amount;
    }

    @Override
    public synchronized void addAmount(int amount) {
        this.amount += amount;
    }

}
