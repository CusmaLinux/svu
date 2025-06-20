package co.edu.itp.svu.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.itp.svu.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RespuestaDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ResponseDTO.class);
        ResponseDTO respuestaDTO1 = new ResponseDTO();
        respuestaDTO1.setId("id1");
        ResponseDTO respuestaDTO2 = new ResponseDTO();
        assertThat(respuestaDTO1).isNotEqualTo(respuestaDTO2);
        respuestaDTO2.setId(respuestaDTO1.getId());
        assertThat(respuestaDTO1).isEqualTo(respuestaDTO2);
        respuestaDTO2.setId("id2");
        assertThat(respuestaDTO1).isNotEqualTo(respuestaDTO2);
        respuestaDTO1.setId(null);
        assertThat(respuestaDTO1).isNotEqualTo(respuestaDTO2);
    }
}
