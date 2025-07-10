<template>
  <b-container>
    <h2 id="page-heading" data-cy="PqrsHeading">
      <span v-text="t$('ventanillaUnicaApp.pqrs.home.title')" id="pqrs-heading"></span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info mr-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.pqrs.home.refreshListLabel')"></span>
        </button>
        <router-link :to="{ name: 'PqrsCreate' }" custom v-slot="{ navigate }">
          <button
            v-can="['create', 'pqrs']"
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-pqrs"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span v-text="t$('ventanillaUnicaApp.pqrs.home.createLabel')"></span>
          </button>
        </router-link>
      </div>
    </h2>
    <!-- Filter begin -->
    <b-card class="shadow mb-4">
      <h3 class="font-weight-bold mb-4">Filtrar PQRS</h3>
      <b-form name="consultForm" no-validate>
        <b-form-group>
          <template #label> Número de Radicado <span class="text-danger">*</span> </template>

          <b-input-group class="d-flex flex-row">
            <b-input-group-prepend is-text>
              <font-awesome-icon icon="file-lines" class="text-secondary" />
            </b-input-group-prepend>
            <b-form-input
              id="numero_radicado"
              name="numero_radicado"
              type="text"
              data-cy="pqrsSearchInput"
              v-model="searchQuery"
              placeholder="Busca por Titulo o Nro de Radicado"
              required
            >
            </b-form-input>
            <b-input-group-prepend>
              <button class="btn btn-danger" type="button" v-if="searchQuery" @click="clearSearch()">
                <font-awesome-icon icon="trash-can"></font-awesome-icon>
              </button>
            </b-input-group-prepend>
          </b-input-group>
        </b-form-group>
      </b-form>
    </b-card>
    <!-- Filter end -->
    <br />

    <div class="alert alert-warning" v-if="!isFetching && pqrs && pqrs.length === 0">
      <span v-text="t$('ventanillaUnicaApp.pqrs.home.notFound')"></span>
    </div>

    <!-- Begin of the table -->
    <div v-if="pqrs && pqrs.length > 0">
      <b-table-simple striped hover responsive aria-describedby="pqrs">
        <b-thead>
          <b-tr>
            <b-th @click="changeOrder('fileNumber')">
              <span v-text="t$('ventanillaUnicaApp.pqrs.fileNumber')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'fileNumber'"></jhi-sort-indicator>
            </b-th>
            <b-th @click="changeOrder('titulo')">
              <span v-text="t$('ventanillaUnicaApp.pqrs.titulo')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'titulo'"></jhi-sort-indicator>
            </b-th>
            <b-th @click="changeOrder('estado')">
              <span>Estado</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'estado'"></jhi-sort-indicator>
            </b-th>
            <b-th @click="changeOrder('oficinaResponder.id')">
              <span>Oficina</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'oficinaResponder.id'"></jhi-sort-indicator>
            </b-th>
            <b-th></b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <b-tr v-for="pqr in pqrs" :key="pqr.id" data-cy="entityTable">
            <b-td>
              <router-link :to="{ name: 'PqrsView', params: { pqrsId: pqr.id } }">{{ pqr.fileNumber }}</router-link>
            </b-td>
            <b-td>{{ pqr.titulo }}</b-td>
            <b-td>{{ pqr.estado }}</b-td>
            <b-td>
              <div v-if="pqr.oficinaResponder">
                <router-link :to="{ name: 'OficinaView', params: { oficinaId: pqr.oficinaResponder.id } }">{{
                  pqr.oficinaResponder.nombre
                }}</router-link>
              </div>
            </b-td>
            <b-td class="text-right">
              <div class="btn-group">
                <router-link :to="{ name: 'PqrsView', params: { pqrsId: pqr.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.view')"></span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'PqrsEdit', params: { pqrsId: pqr.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.edit')"></span>
                  </button>
                </router-link>
                <b-button
                  v-can="['delete', 'pqrs']"
                  @click="prepareRemove(pqr)"
                  variant="danger"
                  class="btn btn-sm"
                  data-cy="entityDeleteButton"
                  v-b-modal.removeEntity
                >
                  <font-awesome-icon icon="times"></font-awesome-icon>
                  <span class="d-none d-md-inline" v-text="t$('entity.action.delete')"></span>
                </b-button>
              </div>
            </b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
    </div>
    <!--End of the table-->

    <b-modal ref="removeEntity" id="removeEntity">
      <template #modal-title>
        <span id="ventanillaUnicaApp.pqrs.delete.question" data-cy="pqrsDeleteDialogHeading" v-text="t$('entity.delete.title')"></span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-pqrs-heading" v-text="t$('ventanillaUnicaApp.pqrs.delete.question', { id: removeId })"></p>
      </div>
      <template #modal-footer>
        <div>
          <button type="button" class="btn btn-secondary" v-text="t$('entity.action.cancel')" @click="closeDialog()"></button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-pqrs"
            data-cy="entityConfirmDeleteButton"
            v-text="t$('entity.action.delete')"
            @click="removePqrs()"
          ></button>
        </div>
      </template>
    </b-modal>
    <div v-show="pqrs && pqrs.length > 0">
      <div class="row justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :itemsPerPage="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </b-container>
</template>

<script lang="ts" src="./pqrs.component.ts"></script>
