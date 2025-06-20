package co.edu.itp.svu.web.rest;

import static co.edu.itp.svu.domain.RespuestaAsserts.*;
import static co.edu.itp.svu.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.repository.RespuestaRepository;
import co.edu.itp.svu.service.dto.ResponseDTO;
import co.edu.itp.svu.service.mapper.ResponseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the {@link RespuestaResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class RespuestaResourceIT {

    private static final String DEFAULT_CONTENIDO = "AAAAAAAAAA";
    private static final String UPDATED_CONTENIDO = "BBBBBBBBBB";

    private static final Instant DEFAULT_FECHA_RESPUESTA = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FECHA_RESPUESTA = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/respuestas";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RespuestaRepository respuestaRepository;

    @Autowired
    private ResponseMapper respuestaMapper;

    @Autowired
    private MockMvc restRespuestaMockMvc;

    private Respuesta respuesta;

    private Respuesta insertedRespuesta;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Respuesta createEntity() {
        return new Respuesta().contenido(DEFAULT_CONTENIDO).fechaRespuesta(DEFAULT_FECHA_RESPUESTA);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Respuesta createUpdatedEntity() {
        return new Respuesta().contenido(UPDATED_CONTENIDO).fechaRespuesta(UPDATED_FECHA_RESPUESTA);
    }

    @BeforeEach
    public void initTest() {
        respuesta = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedRespuesta != null) {
            respuestaRepository.delete(insertedRespuesta);
            insertedRespuesta = null;
        }
    }

    @Test
    void createRespuesta() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Respuesta
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);
        var returnedRespuestaDTO = om.readValue(
            restRespuestaMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(respuestaDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ResponseDTO.class
        );

        // Validate the Respuesta in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRespuesta = respuestaMapper.toEntity(returnedRespuestaDTO);
        assertRespuestaUpdatableFieldsEquals(returnedRespuesta, getPersistedRespuesta(returnedRespuesta));

        insertedRespuesta = returnedRespuesta;
    }

    @Test
    void createRespuestaWithExistingId() throws Exception {
        // Create the Respuesta with an existing ID
        respuesta.setId("existing_id");
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRespuestaMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(respuestaDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Respuesta in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkFechaRespuestaIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        respuesta.setFechaRespuesta(null);

        // Create the Respuesta, which fails.
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);

        restRespuestaMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(respuestaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllRespuestas() throws Exception {
        // Initialize the database
        insertedRespuesta = respuestaRepository.save(respuesta);

        // Get all the respuestaList
        restRespuestaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(respuesta.getId())))
            .andExpect(jsonPath("$.[*].contenido").value(hasItem(DEFAULT_CONTENIDO.toString())))
            .andExpect(jsonPath("$.[*].fechaRespuesta").value(hasItem(DEFAULT_FECHA_RESPUESTA.toString())));
    }

    @Test
    void getRespuesta() throws Exception {
        // Initialize the database
        insertedRespuesta = respuestaRepository.save(respuesta);

        // Get the respuesta
        restRespuestaMockMvc
            .perform(get(ENTITY_API_URL_ID, respuesta.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(respuesta.getId()))
            .andExpect(jsonPath("$.contenido").value(DEFAULT_CONTENIDO.toString()))
            .andExpect(jsonPath("$.fechaRespuesta").value(DEFAULT_FECHA_RESPUESTA.toString()));
    }

    @Test
    void getNonExistingRespuesta() throws Exception {
        // Get the respuesta
        restRespuestaMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void putExistingRespuesta() throws Exception {
        // Initialize the database
        insertedRespuesta = respuestaRepository.save(respuesta);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the respuesta
        Respuesta updatedRespuesta = respuestaRepository.findById(respuesta.getId()).orElseThrow();
        updatedRespuesta.contenido(UPDATED_CONTENIDO).fechaRespuesta(UPDATED_FECHA_RESPUESTA);
        ResponseDTO respuestaDTO = respuestaMapper.toDto(updatedRespuesta);

        restRespuestaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, respuestaDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(respuestaDTO))
            )
            .andExpect(status().isOk());

        // Validate the Respuesta in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRespuestaToMatchAllProperties(updatedRespuesta);
    }

    @Test
    void putNonExistingRespuesta() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        respuesta.setId(UUID.randomUUID().toString());

        // Create the Respuesta
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRespuestaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, respuestaDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(respuestaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Respuesta in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchRespuesta() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        respuesta.setId(UUID.randomUUID().toString());

        // Create the Respuesta
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRespuestaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(respuestaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Respuesta in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamRespuesta() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        respuesta.setId(UUID.randomUUID().toString());

        // Create the Respuesta
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRespuestaMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(respuestaDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Respuesta in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateRespuestaWithPatch() throws Exception {
        // Initialize the database
        insertedRespuesta = respuestaRepository.save(respuesta);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the respuesta using partial update
        Respuesta partialUpdatedRespuesta = new Respuesta();
        partialUpdatedRespuesta.setId(respuesta.getId());

        partialUpdatedRespuesta.contenido(UPDATED_CONTENIDO);

        restRespuestaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRespuesta.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRespuesta))
            )
            .andExpect(status().isOk());

        // Validate the Respuesta in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRespuestaUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRespuesta, respuesta),
            getPersistedRespuesta(respuesta)
        );
    }

    @Test
    void fullUpdateRespuestaWithPatch() throws Exception {
        // Initialize the database
        insertedRespuesta = respuestaRepository.save(respuesta);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the respuesta using partial update
        Respuesta partialUpdatedRespuesta = new Respuesta();
        partialUpdatedRespuesta.setId(respuesta.getId());

        partialUpdatedRespuesta.contenido(UPDATED_CONTENIDO).fechaRespuesta(UPDATED_FECHA_RESPUESTA);

        restRespuestaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRespuesta.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRespuesta))
            )
            .andExpect(status().isOk());

        // Validate the Respuesta in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRespuestaUpdatableFieldsEquals(partialUpdatedRespuesta, getPersistedRespuesta(partialUpdatedRespuesta));
    }

    @Test
    void patchNonExistingRespuesta() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        respuesta.setId(UUID.randomUUID().toString());

        // Create the Respuesta
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRespuestaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, respuestaDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(respuestaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Respuesta in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchRespuesta() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        respuesta.setId(UUID.randomUUID().toString());

        // Create the Respuesta
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRespuestaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(respuestaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Respuesta in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamRespuesta() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        respuesta.setId(UUID.randomUUID().toString());

        // Create the Respuesta
        ResponseDTO respuestaDTO = respuestaMapper.toDto(respuesta);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRespuestaMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(respuestaDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Respuesta in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteRespuesta() throws Exception {
        // Initialize the database
        insertedRespuesta = respuestaRepository.save(respuesta);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the respuesta
        restRespuestaMockMvc
            .perform(delete(ENTITY_API_URL_ID, respuesta.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return respuestaRepository.count();
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

    protected Respuesta getPersistedRespuesta(Respuesta respuesta) {
        return respuestaRepository.findById(respuesta.getId()).orElseThrow();
    }

    protected void assertPersistedRespuestaToMatchAllProperties(Respuesta expectedRespuesta) {
        assertRespuestaAllPropertiesEquals(expectedRespuesta, getPersistedRespuesta(expectedRespuesta));
    }

    protected void assertPersistedRespuestaToMatchUpdatableProperties(Respuesta expectedRespuesta) {
        assertRespuestaAllUpdatablePropertiesEquals(expectedRespuesta, getPersistedRespuesta(expectedRespuesta));
    }
}
