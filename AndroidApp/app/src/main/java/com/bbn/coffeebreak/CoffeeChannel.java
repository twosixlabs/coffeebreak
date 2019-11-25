package com.bbn.coffeebreak;

import java.io.IOException;

public interface CoffeeChannel {

    void send(byte[] data) throws IOException;
    void recv(byte[] buf) throws IOException;
}
