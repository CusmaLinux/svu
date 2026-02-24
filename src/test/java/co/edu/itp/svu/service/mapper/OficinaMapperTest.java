package co.edu.itp.svu.service.mapper;

import static co.edu.itp.svu.domain.OficinaAsserts.*;
import static co.edu.itp.svu.domain.OficinaTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.edu.itp.svu.domain.Oficina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OficinaMapperTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private OficinaMapperImpl oficinaMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(userMapper.userToUserDTO(any())).thenReturn(null);
        when(userMapper.userDTOToUser(any(co.edu.itp.svu.service.dto.UserDTO.class))).thenReturn(null);
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOficinaSample1();
        var actual = oficinaMapper.toEntity(oficinaMapper.toDto(expected));
        assertOficinaAllPropertiesEquals(expected, actual);
    }
}
