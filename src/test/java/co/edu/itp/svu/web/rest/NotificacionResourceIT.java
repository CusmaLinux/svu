package co.edu.itp.svu.web.rest;

import static co.edu.itp.svu.domain.NotificacionAsserts.*;
import static co.edu.itp.svu.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.domain.Notificacion;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.NotificacionRepository;
import co.edu.itp.svu.repository.UserRepository;
import co.edu.itp.svu.service.NotificacionService;
import co.edu.itp.svu.service.UserService;
import co.edu.itp.svu.service.dto.AdminUserDTO;
import co.edu.itp.svu.service.dto.NotificacionDTO;
import co.edu.itp.svu.service.mapper.NotificacionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the {@link NotificacionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class NotificacionResourceIT {

    private static final String DEFAULT_TIPO = "AAAAAAAAAA";
    private static final String UPDATED_TIPO = "BBBBBBBBBB";

    private static final Instant DEFAULT_FECHA = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FECHA = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_MENSAJE = "AAAAAAAAAA";
    private static final String UPDATED_MENSAJE = "BBBBBBBBBB";

    private static final Boolean DEFAULT_LEIDO = false;
    private static final Boolean UPDATED_LEIDO = true;

    private static final String ENTITY_API_URL = "/api/notification";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Mock
    private NotificacionRepository notificacionRepositoryMock;

    @Autowired
    private NotificacionMapper notificacionMapper;

    @Mock
    private NotificacionService notificacionServiceMock;

    @Autowired
    private MockMvc restNotificacionMockMvc;

    private Notificacion notificacion;

    private Notificacion insertedNotificacion;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Notificacion createEntity() {
        return new Notificacion().tipo(DEFAULT_TIPO).fecha(DEFAULT_FECHA).mensaje(DEFAULT_MENSAJE).leido(DEFAULT_LEIDO);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Notificacion createUpdatedEntity() {
        return new Notificacion().tipo(UPDATED_TIPO).fecha(UPDATED_FECHA).mensaje(UPDATED_MENSAJE).leido(UPDATED_LEIDO);
    }

    @BeforeEach
    public void initTest() {
        notificacion = createEntity();
        if (userRepository.findOneByLogin("user").isEmpty()) {
            AdminUserDTO user = new AdminUserDTO();
            user.setLogin("user");
            user.setPassword("password");
            user.setActivated(true);
            userService.createUser(user);
        }
        userRepository.findAll().stream().findFirst().ifPresent(notificacion::recipient);
    }

    @AfterEach
    public void cleanup() {
        if (insertedNotificacion != null) {
            notificacionRepository.delete(insertedNotificacion);
            insertedNotificacion = null;
        }
        userRepository.deleteAll();
    }

    @Test
    void createNotificacion() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Notificacion
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);
        var returnedNotificacionDTO = om.readValue(
            restNotificacionMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(notificacionDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            NotificacionDTO.class
        );

        // Validate the Notificacion in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedNotificacion = notificacionMapper.toEntity(returnedNotificacionDTO);
        assertNotificacionUpdatableFieldsEquals(returnedNotificacion, getPersistedNotificacion(returnedNotificacion));

        insertedNotificacion = returnedNotificacion;
    }

    @Test
    void createNotificacionWithExistingId() throws Exception {
        // Create the Notificacion with an existing ID
        notificacion.setId("existing_id");
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restNotificacionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(notificacionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Notificacion in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkTipoIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificacion.setTipo(null);

        // Create the Notificacion, which fails.
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        restNotificacionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(notificacionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkFechaIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        notificacion.setFecha(null);

        // Create the Notificacion, which fails.
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        restNotificacionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(notificacionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllNotificacions() throws Exception {
        // Initialize the database
        insertedNotificacion = notificacionRepository.save(notificacion);

        // Get all the notificacionList
        restNotificacionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(notificacion.getId())))
            .andExpect(jsonPath("$.[*].tipo").value(hasItem(DEFAULT_TIPO)))
            .andExpect(jsonPath("$.[*].fecha").value(hasItem(DEFAULT_FECHA.toString())))
            .andExpect(jsonPath("$.[*].mensaje").value(hasItem(DEFAULT_MENSAJE.toString())))
            .andExpect(jsonPath("$.[*].leido").value(hasItem(DEFAULT_LEIDO.booleanValue())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllNotificacionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(notificacionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restNotificacionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(notificacionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllNotificacionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(notificacionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restNotificacionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(notificacionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getNotificacion() throws Exception {
        // Initialize the database
        insertedNotificacion = notificacionRepository.save(notificacion);

        // Get the notificacion
        restNotificacionMockMvc
            .perform(get(ENTITY_API_URL_ID, notificacion.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(notificacion.getId()))
            .andExpect(jsonPath("$.tipo").value(DEFAULT_TIPO))
            .andExpect(jsonPath("$.fecha").value(DEFAULT_FECHA.toString()))
            .andExpect(jsonPath("$.mensaje").value(DEFAULT_MENSAJE.toString()))
            .andExpect(jsonPath("$.leido").value(DEFAULT_LEIDO.booleanValue()));
    }

    @Test
    void getNonExistingNotificacion() throws Exception {
        // Get the notificacion
        restNotificacionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void putExistingNotificacion() throws Exception {
        // Initialize the database
        insertedNotificacion = notificacionRepository.save(notificacion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the notificacion
        Notificacion updatedNotificacion = notificacionRepository.findById(notificacion.getId()).orElseThrow();
        updatedNotificacion.tipo(UPDATED_TIPO).fecha(UPDATED_FECHA).mensaje(UPDATED_MENSAJE).leido(UPDATED_LEIDO);
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(updatedNotificacion);

        restNotificacionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, notificacionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificacionDTO))
            )
            .andExpect(status().isOk());

        // Validate the Notificacion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedNotificacionToMatchAllProperties(updatedNotificacion);
    }

    @Test
    void putNonExistingNotificacion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificacion.setId(UUID.randomUUID().toString());

        // Create the Notificacion
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restNotificacionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, notificacionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificacionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Notificacion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchNotificacion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificacion.setId(UUID.randomUUID().toString());

        // Create the Notificacion
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNotificacionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(notificacionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Notificacion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamNotificacion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificacion.setId(UUID.randomUUID().toString());

        // Create the Notificacion
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNotificacionMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(notificacionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Notificacion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateNotificacionWithPatch() throws Exception {
        // Initialize the database
        insertedNotificacion = notificacionRepository.save(notificacion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the notificacion using partial update
        Notificacion partialUpdatedNotificacion = new Notificacion();
        partialUpdatedNotificacion.setId(notificacion.getId());

        partialUpdatedNotificacion.mensaje(UPDATED_MENSAJE);

        restNotificacionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedNotificacion.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedNotificacion))
            )
            .andExpect(status().isOk());

        // Validate the Notificacion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertNotificacionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedNotificacion, notificacion),
            getPersistedNotificacion(notificacion)
        );
    }

    @Test
    void fullUpdateNotificacionWithPatch() throws Exception {
        // Initialize the database
        insertedNotificacion = notificacionRepository.save(notificacion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the notificacion using partial update
        Notificacion partialUpdatedNotificacion = new Notificacion();
        partialUpdatedNotificacion.setId(notificacion.getId());

        partialUpdatedNotificacion.tipo(UPDATED_TIPO).fecha(UPDATED_FECHA).mensaje(UPDATED_MENSAJE).leido(UPDATED_LEIDO);

        restNotificacionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedNotificacion.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedNotificacion))
            )
            .andExpect(status().isOk());

        // Validate the Notificacion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertNotificacionUpdatableFieldsEquals(partialUpdatedNotificacion, getPersistedNotificacion(partialUpdatedNotificacion));
    }

    @Test
    void patchNonExistingNotificacion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificacion.setId(UUID.randomUUID().toString());

        // Create the Notificacion
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restNotificacionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, notificacionDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(notificacionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Notificacion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchNotificacion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificacion.setId(UUID.randomUUID().toString());

        // Create the Notificacion
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNotificacionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(notificacionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Notificacion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamNotificacion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        notificacion.setId(UUID.randomUUID().toString());

        // Create the Notificacion
        NotificacionDTO notificacionDTO = notificacionMapper.toDto(notificacion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNotificacionMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(notificacionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Notificacion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteNotificacion() throws Exception {
        // Initialize the database
        insertedNotificacion = notificacionRepository.save(notificacion);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the notificacion
        restNotificacionMockMvc
            .perform(delete(ENTITY_API_URL_ID, notificacion.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return notificacionRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Notificacion getPersistedNotificacion(Notificacion notificacion) {
        return notificacionRepository.findById(notificacion.getId()).orElseThrow();
    }

    protected void assertPersistedNotificacionToMatchAllProperties(Notificacion expectedNotificacion) {
        assertNotificacionAllPropertiesEquals(expectedNotificacion, getPersistedNotificacion(expectedNotificacion));
    }

    protected void assertPersistedNotificacionToMatchUpdatableProperties(Notificacion expectedNotificacion) {
        assertNotificacionAllUpdatablePropertiesEquals(expectedNotificacion, getPersistedNotificacion(expectedNotificacion));
    }
}
