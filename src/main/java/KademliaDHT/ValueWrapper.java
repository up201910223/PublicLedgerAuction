package main.java.KademliaDHT;

import java.io.*;

public class ValueWrapper implements Serializable {

    private Object storedValue;

    public ValueWrapper(Object storedValue) {
        this.storedValue = storedValue;
    }

    public Object retrieveValue() {
        return storedValue;
    }

    public void updateValue(Object storedValue) {
        this.storedValue = storedValue;
    }

    /**
     * Custom serialization logic to write the internal state
     *
     * @param outputStream the stream to write object data to
     * @throws IOException if an I/O error occurs
     */
    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.writeObject(storedValue);
    }

    /**
     * Custom deserialization logic to read the internal state
     *
     * @param inputStream the stream to read object data from
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        storedValue = inputStream.readObject();
    }
}
