package co.edu.itp.svu.web.rest;

import static co.edu.itp.svu.domain.InformePqrsAsserts.*;
import static co.edu.itp.svu.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.domain.InformePqrs;
import co.edu.itp.svu.repository.InformePqrsRepository;
import co.edu.itp.svu.security.AuthoritiesConstants;
import co.edu.itp.svu.service.dto.InformePqrsDTO;
import co.edu.itp.svu.service.mapper.InformePqrsMapper;
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
 * Integration tests for the {@link InformePqrsResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class InformePqrsResourceIT {

    private static final Instant DEFAULT_FECHA_INICIO = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FECHA_INICIO = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_FECHA_FIN = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FECHA_FIN = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_TOTAL_PQRS = 1;
    private static final Integer UPDATED_TOTAL_PQRS = 2;

    private static final Integer DEFAULT_TOTAL_RESUELTAS = 1;
    private static final Integer UPDATED_TOTAL_RESUELTAS = 2;

    private static final Integer DEFAULT_TOTAL_PENDIENTES = 1;
    private static final Integer UPDATED_TOTAL_PENDIENTES = 2;

    private static final String ENTITY_API_URL = "/api/informe-pqrs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private InformePqrsRepository informePqrsRepository;

    @Autowired
    private InformePqrsMapper informePqrsMapper;

    @Autowired
    private MockMvc restInformePqrsMockMvc;

    private InformePqrs informePqrs;

    private InformePqrs insertedInformePqrs;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static InformePqrs createEntity() {
        return new InformePqrs()
            .fechaInicio(DEFAULT_FECHA_INICIO)
            .fechaFin(DEFAULT_FECHA_FIN)
            .totalPqrs(DEFAULT_TOTAL_PQRS)
            .totalResueltas(DEFAULT_TOTAL_RESUELTAS)
            .totalPendientes(DEFAULT_TOTAL_PENDIENTES);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static InformePqrs createUpdatedEntity() {
        return new InformePqrs()
            .fechaInicio(UPDATED_FECHA_INICIO)
            .fechaFin(UPDATED_FECHA_FIN)
            .totalPqrs(UPDATED_TOTAL_PQRS)
            .totalResueltas(UPDATED_TOTAL_RESUELTAS)
            .totalPendientes(UPDATED_TOTAL_PENDIENTES);
    }

    @BeforeEach
    public void initTest() {
        informePqrs = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedInformePqrs != null) {
            informePqrsRepository.delete(insertedInformePqrs);
            insertedInformePqrs = null;
        }
    }

    @Test
    void createInformePqrs() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the InformePqrs
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);
        var returnedInformePqrsDTO = om.readValue(
            restInformePqrsMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(informePqrsDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            InformePqrsDTO.class
        );

        // Validate the InformePqrs in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedInformePqrs = informePqrsMapper.toEntity(returnedInformePqrsDTO);
        assertInformePqrsUpdatableFieldsEquals(returnedInformePqrs, getPersistedInformePqrs(returnedInformePqrs));

        insertedInformePqrs = returnedInformePqrs;
    }

    @Test
    void createInformePqrsWithExistingId() throws Exception {
        // Create the InformePqrs with an existing ID
        informePqrs.setId("existing_id");
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restInformePqrsMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(informePqrsDTO)))
            .andExpect(status().isBadRequest());

        // Validate the InformePqrs in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkFechaInicioIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        informePqrs.setFechaInicio(null);

        // Create the InformePqrs, which fails.
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        restInformePqrsMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(informePqrsDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkFechaFinIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        informePqrs.setFechaFin(null);

        // Create the InformePqrs, which fails.
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        restInformePqrsMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(informePqrsDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllInformePqrs() throws Exception {
        // Initialize the database
        insertedInformePqrs = informePqrsRepository.save(informePqrs);

        // Get all the informePqrsList
        restInformePqrsMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(informePqrs.getId())))
            .andExpect(jsonPath("$.[*].fechaInicio").value(hasItem(DEFAULT_FECHA_INICIO.toString())))
            .andExpect(jsonPath("$.[*].fechaFin").value(hasItem(DEFAULT_FECHA_FIN.toString())))
            .andExpect(jsonPath("$.[*].totalPqrs").value(hasItem(DEFAULT_TOTAL_PQRS)))
            .andExpect(jsonPath("$.[*].totalResueltas").value(hasItem(DEFAULT_TOTAL_RESUELTAS)))
            .andExpect(jsonPath("$.[*].totalPendientes").value(hasItem(DEFAULT_TOTAL_PENDIENTES)));
    }

    @Test
    void getInformePqrs() throws Exception {
        // Initialize the database
        insertedInformePqrs = informePqrsRepository.save(informePqrs);

        // Get the informePqrs
        restInformePqrsMockMvc
            .perform(get(ENTITY_API_URL_ID, informePqrs.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(informePqrs.getId()))
            .andExpect(jsonPath("$.fechaInicio").value(DEFAULT_FECHA_INICIO.toString()))
            .andExpect(jsonPath("$.fechaFin").value(DEFAULT_FECHA_FIN.toString()))
            .andExpect(jsonPath("$.totalPqrs").value(DEFAULT_TOTAL_PQRS))
            .andExpect(jsonPath("$.totalResueltas").value(DEFAULT_TOTAL_RESUELTAS))
            .andExpect(jsonPath("$.totalPendientes").value(DEFAULT_TOTAL_PENDIENTES));
    }

    @Test
    void getNonExistingInformePqrs() throws Exception {
        // Get the informePqrs
        restInformePqrsMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void putExistingInformePqrs() throws Exception {
        // Initialize the database
        insertedInformePqrs = informePqrsRepository.save(informePqrs);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the informePqrs
        InformePqrs updatedInformePqrs = informePqrsRepository.findById(informePqrs.getId()).orElseThrow();
        updatedInformePqrs
            .fechaInicio(UPDATED_FECHA_INICIO)
            .fechaFin(UPDATED_FECHA_FIN)
            .totalPqrs(UPDATED_TOTAL_PQRS)
            .totalResueltas(UPDATED_TOTAL_RESUELTAS)
            .totalPendientes(UPDATED_TOTAL_PENDIENTES);
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(updatedInformePqrs);

        restInformePqrsMockMvc
            .perform(
                put(ENTITY_API_URL_ID, informePqrsDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(informePqrsDTO))
            )
            .andExpect(status().isOk());

        // Validate the InformePqrs in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedInformePqrsToMatchAllProperties(updatedInformePqrs);
    }

    @Test
    void putNonExistingInformePqrs() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        informePqrs.setId(UUID.randomUUID().toString());

        // Create the InformePqrs
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInformePqrsMockMvc
            .perform(
                put(ENTITY_API_URL_ID, informePqrsDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(informePqrsDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the InformePqrs in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchInformePqrs() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        informePqrs.setId(UUID.randomUUID().toString());

        // Create the InformePqrs
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInformePqrsMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(informePqrsDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the InformePqrs in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamInformePqrs() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        informePqrs.setId(UUID.randomUUID().toString());

        // Create the InformePqrs
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInformePqrsMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(informePqrsDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the InformePqrs in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateInformePqrsWithPatch() throws Exception {
        // Initialize the database
        insertedInformePqrs = informePqrsRepository.save(informePqrs);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the informePqrs using partial update
        InformePqrs partialUpdatedInformePqrs = new InformePqrs();
        partialUpdatedInformePqrs.setId(informePqrs.getId());

        partialUpdatedInformePqrs.fechaInicio(UPDATED_FECHA_INICIO).totalPendientes(UPDATED_TOTAL_PENDIENTES);

        restInformePqrsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedInformePqrs.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedInformePqrs))
            )
            .andExpect(status().isOk());

        // Validate the InformePqrs in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInformePqrsUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedInformePqrs, informePqrs),
            getPersistedInformePqrs(informePqrs)
        );
    }

    @Test
    void fullUpdateInformePqrsWithPatch() throws Exception {
        // Initialize the database
        insertedInformePqrs = informePqrsRepository.save(informePqrs);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the informePqrs using partial update
        InformePqrs partialUpdatedInformePqrs = new InformePqrs();
        partialUpdatedInformePqrs.setId(informePqrs.getId());

        partialUpdatedInformePqrs
            .fechaInicio(UPDATED_FECHA_INICIO)
            .fechaFin(UPDATED_FECHA_FIN)
            .totalPqrs(UPDATED_TOTAL_PQRS)
            .totalResueltas(UPDATED_TOTAL_RESUELTAS)
            .totalPendientes(UPDATED_TOTAL_PENDIENTES);

        restInformePqrsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedInformePqrs.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedInformePqrs))
            )
            .andExpect(status().isOk());

        // Validate the InformePqrs in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInformePqrsUpdatableFieldsEquals(partialUpdatedInformePqrs, getPersistedInformePqrs(partialUpdatedInformePqrs));
    }

    @Test
    void patchNonExistingInformePqrs() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        informePqrs.setId(UUID.randomUUID().toString());

        // Create the InformePqrs
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInformePqrsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, informePqrsDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(informePqrsDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the InformePqrs in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchInformePqrs() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        informePqrs.setId(UUID.randomUUID().toString());

        // Create the InformePqrs
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInformePqrsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(informePqrsDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the InformePqrs in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamInformePqrs() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        informePqrs.setId(UUID.randomUUID().toString());

        // Create the InformePqrs
        InformePqrsDTO informePqrsDTO = informePqrsMapper.toDto(informePqrs);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInformePqrsMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(informePqrsDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the InformePqrs in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteInformePqrs() throws Exception {
        // Initialize the database
        insertedInformePqrs = informePqrsRepository.save(informePqrs);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the informePqrs
        restInformePqrsMockMvc
            .perform(delete(ENTITY_API_URL_ID, informePqrs.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return informePqrsRepository.count();
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

    protected InformePqrs getPersistedInformePqrs(InformePqrs informePqrs) {
        return informePqrsRepository.findById(informePqrs.getId()).orElseThrow();
    }

    protected void assertPersistedInformePqrsToMatchAllProperties(InformePqrs expectedInformePqrs) {
        assertInformePqrsAllPropertiesEquals(expectedInformePqrs, getPersistedInformePqrs(expectedInformePqrs));
    }

    protected void assertPersistedInformePqrsToMatchUpdatableProperties(InformePqrs expectedInformePqrs) {
        assertInformePqrsAllUpdatablePropertiesEquals(expectedInformePqrs, getPersistedInformePqrs(expectedInformePqrs));
    }
}
