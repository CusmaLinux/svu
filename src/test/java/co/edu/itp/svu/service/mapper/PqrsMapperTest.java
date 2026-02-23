package co.edu.itp.svu.service.mapper;

import static co.edu.itp.svu.domain.PqrsAsserts.*;
import static co.edu.itp.svu.domain.PqrsTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.edu.itp.svu.domain.Pqrs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PqrsMapperTest {

    @Mock
    private SatisfactionSurveyMapper satisfactionSurveyMapper;

    @InjectMocks
    private PqrsMapperImpl pqrsMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(satisfactionSurveyMapper.toDto(any(co.edu.itp.svu.domain.SatisfactionSurvey.class))).thenReturn(null);
        when(satisfactionSurveyMapper.toEntity(any(co.edu.itp.svu.service.dto.SatisfactionSurveyDTO.class))).thenReturn(null);
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPqrsSample1();
        var actual = pqrsMapper.toEntity(pqrsMapper.toDto(expected));
        assertPqrsAllPropertiesEquals(expected, actual);
    }
}
