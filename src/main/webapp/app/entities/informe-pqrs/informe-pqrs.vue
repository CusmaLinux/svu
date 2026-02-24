<template>
  <b-container>
    <h2 id="page-heading" data-cy="InformePqrsHeading">
      <span v-text="t$('ventanillaUnicaApp.informePqrs.home.title')" id="informe-pqrs-heading"></span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info mr-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.informePqrs.home.refreshListLabel')"></span>
        </button>
        <router-link :to="{ name: 'InformePqrsCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-informe-pqrs"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span v-text="t$('ventanillaUnicaApp.informePqrs.home.createLabel')"></span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && informePqrs && informePqrs.length === 0">
      <span v-text="t$('ventanillaUnicaApp.informePqrs.home.notFound')"></span>
    </div>
    <div class="table-responsive" v-if="informePqrs && informePqrs.length > 0">
      <table class="table table-striped" aria-describedby="informePqrs">
        <thead>
          <tr>
            <th scope="row" @click="changeOrder('id')">
              <span v-text="t$('global.field.id')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('fechaInicio')">
              <span v-text="t$('ventanillaUnicaApp.informePqrs.fechaInicio')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'fechaInicio'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('fechaFin')">
              <span v-text="t$('ventanillaUnicaApp.informePqrs.fechaFin')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'fechaFin'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('oficina.id')">
              <span v-text="t$('ventanillaUnicaApp.informePqrs.oficina')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'oficina.id'"></jhi-sort-indicator>
            </th>
            <th scope="row"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="informePqrs in informePqrs" :key="informePqrs.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'InformePqrsView', params: { informePqrsId: informePqrs.id } }">{{ informePqrs.id }}</router-link>
            </td>
            <td>{{ formatDateShort(informePqrs.fechaInicio) || '' }}</td>
            <td>{{ formatDateShort(informePqrs.fechaFin) || '' }}</td>
            <td>
              <div v-if="informePqrs.oficina">
                <router-link :to="{ name: 'OficinaView', params: { oficinaId: informePqrs.oficina.id } }">{{
                  informePqrs.oficina.nombre
                }}</router-link>
              </div>
            </td>
            <td class="text-right">
              <div class="btn-group">
                <router-link :to="{ name: 'InformePqrsView', params: { informePqrsId: informePqrs.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.view')"></span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'InformePqrsEdit', params: { informePqrsId: informePqrs.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.edit')"></span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(informePqrs)"
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
          id="ventanillaUnicaApp.informePqrs.delete.question"
          data-cy="informePqrsDeleteDialogHeading"
          v-text="t$('entity.delete.title')"
        ></span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-informePqrs-heading" v-text="t$('ventanillaUnicaApp.informePqrs.delete.question', { id: removeId })"></p>
      </div>
      <template #modal-footer>
        <div>
          <button type="button" class="btn btn-secondary" v-text="t$('entity.action.cancel')" @click="closeDialog()"></button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-informePqrs"
            data-cy="entityConfirmDeleteButton"
            v-text="t$('entity.action.delete')"
            @click="removeInformePqrs()"
          ></button>
        </div>
      </template>
    </b-modal>
    <div v-show="informePqrs && informePqrs.length > 0">
      <div class="row justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </b-container>
</template>

<script lang="ts" src="./informe-pqrs.component.ts"></script>
