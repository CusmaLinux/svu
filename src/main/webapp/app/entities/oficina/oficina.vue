<template>
  <b-container>
    <h2 id="page-heading" data-cy="OficinaHeading">
      <span v-text="t$('ventanillaUnicaApp.oficina.home.title')" id="oficina-heading"></span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info mr-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.oficina.home.refreshListLabel')"></span>
        </button>
        <router-link :to="{ name: 'OficinaCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-oficina"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span v-text="t$('ventanillaUnicaApp.oficina.home.createLabel')"></span>
          </button>
        </router-link>
      </div>
    </h2>
    <!-- Filter begin -->
    <b-card no-body class="shadow-sm my-4">
      <b-card-header class="bg-light py-3">
        <h3 class="mb-0 h5 font-weight-bold">
          <font-awesome-icon icon="filter" class="mr-2" />
          Filtrar Oficinas
        </h3>
      </b-card-header>

      <b-card-body>
        <b-form @submit.prevent>
          <b-form-group label="Buscar" label-for="pqrs-search-input" label-class="font-weight-bold">
            <b-input-group>
              <b-input-group-prepend is-text>
                <font-awesome-icon icon="search" class="text-secondary" />
              </b-input-group-prepend>

              <b-form-input
                id="pqrs-search-input"
                v-model="searchQuery"
                type="text"
                placeholder="Busca por oficina superior o nombre..."
                data-cy="pqrsSearchInput"
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
    <div class="alert alert-warning" v-if="!isFetching && oficinas && oficinas.length === 0">
      <span v-text="t$('ventanillaUnicaApp.oficina.home.notFound')"></span>
    </div>
    <div class="table-responsive" v-if="oficinas && oficinas.length > 0">
      <table class="table table-striped" aria-describedby="oficinas">
        <thead>
          <tr>
            <th scope="row"><span v-text="t$('ventanillaUnicaApp.oficina.nombre')"></span></th>
            <th scope="row"><span>Responsable</span></th>
            <th scope="row"><span v-text="t$('ventanillaUnicaApp.oficina.descripcion')"></span></th>
            <th scope="row"><span v-text="t$('ventanillaUnicaApp.oficina.nivel')"></span></th>
            <th scope="row"><span v-text="t$('ventanillaUnicaApp.oficina.oficinaSuperior')"></span></th>
            <th scope="row"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="oficina in oficinas" :key="oficina.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'OficinaView', params: { oficinaId: oficina.id } }">{{ oficina.nombre }}</router-link>
            </td>
            <td>{{ oficina.responsableDTO?.login }}</td>
            <td>{{ oficina.descripcion }}</td>
            <td>{{ oficina.nivel }}</td>
            <td>{{ oficina.oficinaSuperior }}</td>
            <td class="text-right">
              <div class="btn-group">
                <router-link :to="{ name: 'OficinaView', params: { oficinaId: oficina.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.view')"></span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'OficinaEdit', params: { oficinaId: oficina.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.edit')"></span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(oficina)"
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
          id="ventanillaUnicaApp.oficina.delete.question"
          data-cy="oficinaDeleteDialogHeading"
          v-text="t$('entity.delete.title')"
        ></span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-oficina-heading" v-text="t$('ventanillaUnicaApp.oficina.delete.question', { id: removeId })"></p>
      </div>
      <template #modal-footer>
        <div>
          <button type="button" class="btn btn-secondary" v-text="t$('entity.action.cancel')" @click="closeDialog()"></button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-oficina"
            data-cy="entityConfirmDeleteButton"
            v-text="t$('entity.action.delete')"
            @click="removeOficina()"
          ></button>
        </div>
      </template>
    </b-modal>
    <div v-show="oficinas && oficinas.length > 0">
      <div class="row justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </b-container>
</template>

<script lang="ts" src="./oficina.component.ts"></script>
