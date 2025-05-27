<template>
  <div>
    <h2 id="page-heading" data-cy="RespuestaHeading">
      <span v-text="t$('ventanillaUnicaApp.respuesta.home.title')" id="respuesta-heading"></span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info mr-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.respuesta.home.refreshListLabel')"></span>
        </button>
        <router-link :to="{ name: 'RespuestaCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-respuesta"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span v-text="t$('ventanillaUnicaApp.respuesta.home.createLabel')"></span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && respuestas && respuestas.length === 0">
      <span v-text="t$('ventanillaUnicaApp.respuesta.home.notFound')"></span>
    </div>
    <div class="table-responsive" v-if="respuestas && respuestas.length > 0">
      <table class="table table-striped" aria-describedby="respuestas">
        <thead>
          <tr>
            <th scope="row" @click="changeOrder('id')">
              <span v-text="t$('global.field.id')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('contenido')">
              <span v-text="t$('ventanillaUnicaApp.respuesta.contenido')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'contenido'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('fechaRespuesta')">
              <span v-text="t$('ventanillaUnicaApp.respuesta.fechaRespuesta')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'fechaRespuesta'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('pqr.id')">
              <span v-text="t$('ventanillaUnicaApp.respuesta.pqr')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'pqr.id'"></jhi-sort-indicator>
            </th>
            <th scope="row"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="respuesta in respuestas" :key="respuesta.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'RespuestaView', params: { respuestaId: respuesta.id } }">{{ respuesta.id }}</router-link>
            </td>
            <td>{{ respuesta.contenido }}</td>
            <td>{{ formatDateShort(respuesta.fechaRespuesta) || '' }}</td>
            <td>
              <div v-if="respuesta.pqr">
                <router-link :to="{ name: 'PqrsView', params: { pqrsId: respuesta.pqr.id } }">{{ respuesta.pqr.id }}</router-link>
              </div>
            </td>
            <td class="text-right">
              <div class="btn-group">
                <router-link :to="{ name: 'RespuestaView', params: { respuestaId: respuesta.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.view')"></span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'RespuestaEdit', params: { respuestaId: respuesta.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.edit')"></span>
                  </button>
                </router-link>
                <b-button
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
        <jhi-item-count :page="page" :total="queryCount" :itemsPerPage="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./respuesta.component.ts"></script>
