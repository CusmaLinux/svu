package co.edu.itp.svu.web.rest;

import static co.edu.itp.svu.domain.OficinaAsserts.*;
import static co.edu.itp.svu.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.repository.OficinaRepository;
import co.edu.itp.svu.service.dto.OficinaDTO;
import co.edu.itp.svu.service.mapper.OficinaMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Integration tests for the {@link OficinaResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class OficinaResourceIT {

    private static final String DEFAULT_NOMBRE = "AAAAAAAAAA";
    private static final String UPDATED_NOMBRE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPCION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPCION = "BBBBBBBBBB";

    private static final String DEFAULT_NIVEL = "AAAAAAAAAA";
    private static final String UPDATED_NIVEL = "BBBBBBBBBB";

    private static final String DEFAULT_OFICINA_SUPERIOR = "AAAAAAAAAA";
    private static final String UPDATED_OFICINA_SUPERIOR = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/oficinas";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OficinaRepository oficinaRepository;

    @Autowired
    private OficinaMapper oficinaMapper;

    @Autowired
    private MockMvc restOficinaMockMvc;

    private Oficina oficina;

    private Oficina insertedOficina;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Oficina createEntity() {
        return new Oficina()
            .nombre(DEFAULT_NOMBRE)
            .descripcion(DEFAULT_DESCRIPCION)
            .nivel(DEFAULT_NIVEL)
            .oficinaSuperior(DEFAULT_OFICINA_SUPERIOR);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Oficina createUpdatedEntity() {
        return new Oficina()
            .nombre(UPDATED_NOMBRE)
            .descripcion(UPDATED_DESCRIPCION)
            .nivel(UPDATED_NIVEL)
            .oficinaSuperior(UPDATED_OFICINA_SUPERIOR);
    }

    @BeforeEach
    public void initTest() {
        oficina = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedOficina != null) {
            oficinaRepository.delete(insertedOficina);
            insertedOficina = null;
        }
    }

    @Test
    void createOficina() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Oficina
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);
        var returnedOficinaDTO = om.readValue(
            restOficinaMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oficinaDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OficinaDTO.class
        );

        // Validate the Oficina in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOficina = oficinaMapper.toEntity(returnedOficinaDTO);
        assertOficinaUpdatableFieldsEquals(returnedOficina, getPersistedOficina(returnedOficina));

        insertedOficina = returnedOficina;
    }

    @Test
    void createOficinaWithExistingId() throws Exception {
        // Create the Oficina with an existing ID
        oficina.setId("existing_id");
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOficinaMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oficinaDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Oficina in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkNombreIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        oficina.setNombre(null);

        // Create the Oficina, which fails.
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        restOficinaMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oficinaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkNivelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        oficina.setNivel(null);

        // Create the Oficina, which fails.
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        restOficinaMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oficinaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllOficinas() throws Exception {
        // Initialize the database
        insertedOficina = oficinaRepository.save(oficina);

        // Get all the oficinaList
        restOficinaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(oficina.getId())))
            .andExpect(jsonPath("$.[*].nombre").value(hasItem(DEFAULT_NOMBRE)))
            .andExpect(jsonPath("$.[*].descripcion").value(hasItem(DEFAULT_DESCRIPCION)))
            .andExpect(jsonPath("$.[*].nivel").value(hasItem(DEFAULT_NIVEL)))
            .andExpect(jsonPath("$.[*].oficinaSuperior").value(hasItem(DEFAULT_OFICINA_SUPERIOR)));
    }

    @Test
    void getOficina() throws Exception {
        // Initialize the database
        insertedOficina = oficinaRepository.save(oficina);

        // Get the oficina
        restOficinaMockMvc
            .perform(get(ENTITY_API_URL_ID, oficina.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(oficina.getId()))
            .andExpect(jsonPath("$.nombre").value(DEFAULT_NOMBRE))
            .andExpect(jsonPath("$.descripcion").value(DEFAULT_DESCRIPCION))
            .andExpect(jsonPath("$.nivel").value(DEFAULT_NIVEL))
            .andExpect(jsonPath("$.oficinaSuperior").value(DEFAULT_OFICINA_SUPERIOR));
    }

    @Test
    void getNonExistingOficina() throws Exception {
        // Get the oficina
        restOficinaMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void putExistingOficina() throws Exception {
        // Initialize the database
        insertedOficina = oficinaRepository.save(oficina);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the oficina
        Oficina updatedOficina = oficinaRepository.findById(oficina.getId()).orElseThrow();
        updatedOficina
            .nombre(UPDATED_NOMBRE)
            .descripcion(UPDATED_DESCRIPCION)
            .nivel(UPDATED_NIVEL)
            .oficinaSuperior(UPDATED_OFICINA_SUPERIOR);
        OficinaDTO oficinaDTO = oficinaMapper.toDto(updatedOficina);

        restOficinaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, oficinaDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oficinaDTO))
            )
            .andExpect(status().isOk());

        // Validate the Oficina in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOficinaToMatchAllProperties(updatedOficina);
    }

    @Test
    void putNonExistingOficina() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oficina.setId(UUID.randomUUID().toString());

        // Create the Oficina
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOficinaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, oficinaDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oficinaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Oficina in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchOficina() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oficina.setId(UUID.randomUUID().toString());

        // Create the Oficina
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOficinaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(oficinaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Oficina in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamOficina() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oficina.setId(UUID.randomUUID().toString());

        // Create the Oficina
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOficinaMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oficinaDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Oficina in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateOficinaWithPatch() throws Exception {
        // Initialize the database
        insertedOficina = oficinaRepository.save(oficina);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the oficina using partial update
        Oficina partialUpdatedOficina = new Oficina();
        partialUpdatedOficina.setId(oficina.getId());

        partialUpdatedOficina.nombre(UPDATED_NOMBRE);

        restOficinaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOficina.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOficina))
            )
            .andExpect(status().isOk());

        // Validate the Oficina in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOficinaUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedOficina, oficina), getPersistedOficina(oficina));
    }

    @Test
    void fullUpdateOficinaWithPatch() throws Exception {
        // Initialize the database
        insertedOficina = oficinaRepository.save(oficina);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the oficina using partial update
        Oficina partialUpdatedOficina = new Oficina();
        partialUpdatedOficina.setId(oficina.getId());

        partialUpdatedOficina
            .nombre(UPDATED_NOMBRE)
            .descripcion(UPDATED_DESCRIPCION)
            .nivel(UPDATED_NIVEL)
            .oficinaSuperior(UPDATED_OFICINA_SUPERIOR);

        restOficinaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOficina.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOficina))
            )
            .andExpect(status().isOk());

        // Validate the Oficina in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOficinaUpdatableFieldsEquals(partialUpdatedOficina, getPersistedOficina(partialUpdatedOficina));
    }

    @Test
    void patchNonExistingOficina() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oficina.setId(UUID.randomUUID().toString());

        // Create the Oficina
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOficinaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, oficinaDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(oficinaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Oficina in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchOficina() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oficina.setId(UUID.randomUUID().toString());

        // Create the Oficina
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOficinaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(oficinaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Oficina in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamOficina() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oficina.setId(UUID.randomUUID().toString());

        // Create the Oficina
        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOficinaMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(oficinaDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Oficina in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteOficina() throws Exception {
        // Initialize the database
        insertedOficina = oficinaRepository.save(oficina);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the oficina
        restOficinaMockMvc
            .perform(delete(ENTITY_API_URL_ID, oficina.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return oficinaRepository.count();
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

    protected Oficina getPersistedOficina(Oficina oficina) {
        return oficinaRepository.findById(oficina.getId()).orElseThrow();
    }

    protected void assertPersistedOficinaToMatchAllProperties(Oficina expectedOficina) {
        assertOficinaAllPropertiesEquals(expectedOficina, getPersistedOficina(expectedOficina));
    }

    protected void assertPersistedOficinaToMatchUpdatableProperties(Oficina expectedOficina) {
        assertOficinaAllUpdatablePropertiesEquals(expectedOficina, getPersistedOficina(expectedOficina));
    }
}
