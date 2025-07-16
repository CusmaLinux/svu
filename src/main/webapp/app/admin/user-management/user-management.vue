<template>
  <b-container>
    <h2>
      <span id="user-management-page-heading" v-text="t$('userManagement.home.title')" data-cy="userManagementPageHeading"></span>

      <div class="d-flex justify-content-end">
        <button class="btn btn-info mr-2" @click="handleSyncList" :disabled="isLoading">
          <font-awesome-icon icon="sync" :spin="isLoading"></font-awesome-icon>
          <span v-text="t$('userManagement.home.refreshListLabel')"></span>
        </button>
        <router-link custom v-slot="{ navigate }" :to="{ name: 'JhiUserCreate' }">
          <button @click="navigate" class="btn btn-primary jh-create-entity">
            <font-awesome-icon icon="plus"></font-awesome-icon> <span v-text="t$('userManagement.home.createLabel')"></span>
          </button>
        </router-link>
      </div>
    </h2>

    <!-- Filter begin -->
    <b-card no-body class="shadow-sm my-4">
      <b-card-header class="bg-light py-3">
        <h3 class="mb-0 h5 font-weight-bold">
          <font-awesome-icon icon="filter" class="mr-2" />
          Filtrar Usuarios
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
    <!-- Filter end -->

    <div class="table-responsive" v-if="users">
      <table class="table table-striped" aria-describedby="Users">
        <thead>
          <tr>
            <th scope="col" @click="changeOrder('login')">
              <span v-text="t$('userManagement.login')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'login'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('email')">
              <span v-text="t$('userManagement.email')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'email'"></jhi-sort-indicator>
            </th>
            <th scope="col">Estado</th>
            <th scope="col"><span v-text="t$('userManagement.profiles')"></span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody v-if="users">
          <tr v-for="user in users" :key="user.id" :id="user.login">
            <td>
              <router-link :to="{ name: 'JhiUserView', params: { userId: user.login } }">{{ user.login }}</router-link>
            </td>
            <td class="jhi-user-email">{{ user.email }}</td>
            <td>
              <button
                class="btn btn-danger btn-sm deactivated"
                @click="setActive(user, true)"
                v-if="!user.activated"
                v-text="t$('userManagement.deactivated')"
              ></button>
              <button
                class="btn btn-success btn-sm"
                @click="setActive(user, false)"
                v-if="user.activated"
                :disabled="username === user.login"
                v-text="t$('userManagement.activated')"
              ></button>
            </td>
            <td>
              <div v-for="authority of user.authorities" :key="authority">
                <span class="badge badge-info">{{ authority }}</span>
              </div>
            </td>
            <td class="text-right">
              <div class="btn-group">
                <router-link :to="{ name: 'JhiUserView', params: { userId: user.login } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.view')"></span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'JhiUserEdit', params: { userId: user.login } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.edit')"></span>
                  </button>
                </router-link>
                <b-button @click="prepareRemove(user)" variant="danger" class="btn btn-sm delete" :disabled="username === user.login">
                  <font-awesome-icon icon="times"></font-awesome-icon>
                  <span class="d-none d-md-inline" v-text="t$('entity.action.delete')"></span>
                </b-button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <b-modal ref="removeUser" id="removeUser" :title="t$('entity.delete.title')" @ok="deleteUser()">
        <div class="modal-body">
          <p id="jhi-delete-user-heading" v-text="t$('userManagement.delete.question', { login: removeId })"></p>
        </div>
        <template #modal-footer>
          <div>
            <button type="button" class="btn btn-secondary" v-text="t$('entity.action.cancel')" @click="closeDialog()"></button>
            <button
              type="button"
              class="btn btn-primary"
              id="confirm-delete-user"
              v-text="t$('entity.action.delete')"
              @click="deleteUser()"
            ></button>
          </div>
        </template>
      </b-modal>
    </div>
    <div v-show="users && users.length > 0">
      <div class="row justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :itemsPerPage="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage" :change="loadPage(page)"></b-pagination>
      </div>
    </div>
  </b-container>
</template>

<script lang="ts" src="./user-management.component.ts"></script>
