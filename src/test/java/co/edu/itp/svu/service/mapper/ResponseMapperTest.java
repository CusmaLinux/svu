package co.edu.itp.svu.service.mapper;

import static co.edu.itp.svu.domain.RespuestaAsserts.*;
import static co.edu.itp.svu.domain.RespuestaTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.edu.itp.svu.domain.Respuesta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ResponseMapperTest {

    @Mock
    private PqrsMapper pqrsMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ResponseMapperImpl respuestaMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(userMapper.userToUserDTO(any())).thenReturn(null);
        when(userMapper.userDTOToUser(any(co.edu.itp.svu.service.dto.UserDTO.class))).thenReturn(null);
        when(pqrsMapper.toDto(any(co.edu.itp.svu.domain.Pqrs.class))).thenReturn(null);
        when(pqrsMapper.toEntity(any(co.edu.itp.svu.service.dto.PqrsDTO.class))).thenReturn(null);
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRespuestaSample1();
        var actual = respuestaMapper.toEntity(respuestaMapper.toDto(expected));
        assertRespuestaAllPropertiesEquals(expected, actual);
    }
}
