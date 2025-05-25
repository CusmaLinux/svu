package co.edu.itp.svu.domain;

import static co.edu.itp.svu.domain.NotificacionTestSamples.*;
import static co.edu.itp.svu.domain.OficinaTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.itp.svu.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotificacionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Notificacion.class);
        Notificacion notificacion1 = getNotificacionSample1();
        Notificacion notificacion2 = new Notificacion();
        assertThat(notificacion1).isNotEqualTo(notificacion2);

        notificacion2.setId(notificacion1.getId());
        assertThat(notificacion1).isEqualTo(notificacion2);

        notificacion2 = getNotificacionSample2();
        assertThat(notificacion1).isNotEqualTo(notificacion2);
    }
}
