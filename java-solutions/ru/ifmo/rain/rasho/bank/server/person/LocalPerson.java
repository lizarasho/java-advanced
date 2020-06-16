package ru.ifmo.rain.rasho.bank.server.person;

import ru.ifmo.rain.rasho.bank.common.Utils;
import ru.ifmo.rain.rasho.bank.common.Person;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson implements Serializable {

    public LocalPerson(final Person person) throws RemoteException {
        super(
                person.getName(),
                person.getSurname(),
                person.getPassport(),
                Utils.convertToLocalAccountsMap(person.getAccountByIdMap())
        );
    }

}
