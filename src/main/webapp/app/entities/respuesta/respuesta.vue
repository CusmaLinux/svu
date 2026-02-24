<template>
  <b-container>
    <h2 id="page-heading" data-cy="RespuestaHeading">
      <span v-text="t$('ventanillaUnicaApp.respuesta.home.title')" id="respuesta-heading"></span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info mr-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.respuesta.home.refreshListLabel')"></span>
        </button>
      </div>
    </h2>

    <!-- Filter begin -->
    <b-card no-body class="shadow-sm my-4">
      <b-card-header class="bg-light py-3">
        <h3 class="mb-0 h5 font-weight-bold">
          <font-awesome-icon icon="filter" class="mr-2" />
          Filtrar Respuestas
        </h3>
      </b-card-header>

      <b-card-body>
        <b-form @submit.prevent>
          <b-form-group label="Buscar" label-for="respuesta-search-input" label-class="font-weight-bold">
            <b-input-group>
              <b-input-group-prepend is-text>
                <font-awesome-icon icon="search" class="text-secondary" />
              </b-input-group-prepend>

              <b-form-input
                id="respuesta-search-input"
                v-model="searchQuery"
                type="text"
                placeholder="Busca por Nro de radicado..."
                data-cy="respuestaSearchInput"
                autocomplete="off"
              ></b-form-input>

              <b-input-group-append>
                <b-button
                  v-if="searchQuery"
                  @click="clearSearch()"
                  variant="link"
                  class="clear-button"
                  v-b-tooltip.hover
                  title="Limpiar búsqueda"
                >
                  <font-awesome-icon icon="times-circle" />
                </b-button>
              </b-input-group-append>
            </b-input-group>
          </b-form-group>
        </b-form>
      </b-card-body>
    </b-card>
    <!-- End filter -->

    <div class="alert alert-warning" v-if="!isFetching && respuestas && respuestas.length === 0">
      <span v-text="t$('ventanillaUnicaApp.respuesta.home.notFound')"></span>
    </div>
    <div class="table-responsive" v-if="respuestas && respuestas.length > 0">
      <table class="table table-striped" aria-describedby="respuestas">
        <thead>
          <tr>
            <th scope="row" @click="changeOrder('file_number')">
              <span v-text="t$('ventanillaUnicaApp.respuesta.pqr')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'file_number'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('contenido')">
              <span v-text="t$('ventanillaUnicaApp.respuesta.contenido')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'contenido'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('fechaRespuesta')">
              <span v-text="t$('ventanillaUnicaApp.respuesta.fechaRespuesta')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'fechaRespuesta'"></jhi-sort-indicator>
            </th>
            <th scope="row"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="respuesta in respuestas" :key="respuesta.id" data-cy="entityTable">
            <td>
              <div v-if="respuesta.pqrs">
                <router-link :to="{ name: 'PqrsView', params: { pqrsId: respuesta.pqrs.id } }">{{ respuesta.pqrs.fileNumber }}</router-link>
              </div>
            </td>
            <td>{{ respuesta.contenido }}</td>
            <td>{{ formatDateShort(respuesta.fechaRespuesta) || '' }}</td>
            <td class="text-right">
              <div class="btn-group">
                <router-link :to="{ name: 'RespuestaView', params: { respuestaId: respuesta.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.view')"></span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'RespuestaEdit', params: { respuestaId: respuesta.id } }" custom v-slot="{ navigate }">
                  <button v-can="['edit', 'responses']" @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.edit')"></span>
                  </button>
                </router-link>
                <b-button
                  v-can="['delete', 'responses']"
                  @click="prepareRemove(respuesta)"
                  variant="danger"
                  class="btn btn-sm"
                  data-cy="entityDeleteButton"
                  v-b-modal.removeEntity
                >
                  <font-awesome-icon icon="times"></font-awesome-icon>
                  <span class="d-none d-md-inline" v-text="t$('entity.action.delete')"></span>
                </b-button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <b-modal ref="removeEntity" id="removeEntity">
      <template #modal-title>
        <span
          id="ventanillaUnicaApp.respuesta.delete.question"
          data-cy="respuestaDeleteDialogHeading"
          v-text="t$('entity.delete.title')"
        ></span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-respuesta-heading" v-text="t$('ventanillaUnicaApp.respuesta.delete.question', { id: removeId })"></p>
      </div>
      <template #modal-footer>
        <div>
          <button type="button" class="btn btn-secondary" v-text="t$('entity.action.cancel')" @click="closeDialog()"></button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-respuesta"
            data-cy="entityConfirmDeleteButton"
            v-text="t$('entity.action.delete')"
            @click="removeRespuesta()"
          ></button>
        </div>
      </template>
    </b-modal>
    <div v-show="respuestas && respuestas.length > 0">
      <div class="row justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </b-container>
</template>

<script lang="ts" src="./respuesta.component.ts"></script>
