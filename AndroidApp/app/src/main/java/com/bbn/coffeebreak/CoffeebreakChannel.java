package com.bbn.coffeebreak;

import java.io.IOException;

public interface CoffeebreakChannel {

    void send(byte[] data) throws IOException;
    byte[] receive(int buflen) throws IOException;
}
