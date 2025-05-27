package co.edu.itp.svu.domain;

import java.util.UUID;

public class RespuestaTestSamples {

    public static Respuesta getRespuestaSample1() {
        return new Respuesta().id("id1");
    }

    public static Respuesta getRespuestaSample2() {
        return new Respuesta().id("id2");
    }

    public static Respuesta getRespuestaRandomSampleGenerator() {
        return new Respuesta().id(UUID.randomUUID().toString());
    }
}
