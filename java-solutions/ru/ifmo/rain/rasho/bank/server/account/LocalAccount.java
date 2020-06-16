package ru.ifmo.rain.rasho.bank.server.account;

import ru.ifmo.rain.rasho.bank.common.Account;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount implements Serializable {

    public LocalAccount(Account account) throws RemoteException {
        super(account.getId(), account.getAmount());
    }

}
