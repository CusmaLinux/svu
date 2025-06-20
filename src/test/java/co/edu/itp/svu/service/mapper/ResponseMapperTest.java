package co.edu.itp.svu.service.mapper;

import static co.edu.itp.svu.domain.RespuestaAsserts.*;
import static co.edu.itp.svu.domain.RespuestaTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseMapperTest {

    private ResponseMapper respuestaMapper;

    @BeforeEach
    void setUp() {
        respuestaMapper = new ResponseMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRespuestaSample1();
        var actual = respuestaMapper.toEntity(respuestaMapper.toDto(expected));
        assertRespuestaAllPropertiesEquals(expected, actual);
    }
}
