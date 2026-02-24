package co.edu.itp.svu.service.mapper;

import static co.edu.itp.svu.domain.NotificacionAsserts.*;
import static co.edu.itp.svu.domain.NotificacionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.edu.itp.svu.domain.Notificacion;
import co.edu.itp.svu.service.dto.NotificacionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NotificacionMapperTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private NotificacionMapperImpl notificacionMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(userMapper.userToUserDTO(any())).thenReturn(null);
        when(userMapper.userDTOToUser((co.edu.itp.svu.service.dto.UserDTO) any())).thenReturn(null);
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getNotificacionSample1();
        var actual = notificacionMapper.toEntity(notificacionMapper.toDto(expected));
        assertNotificacionAllPropertiesEquals(expected, actual);
    }
}
