<template>
  <div>
    <h2 id="page-heading" data-cy="NotificationHeading">
      <span v-text="t$('ventanillaUnicaApp.notification.home.title')" id="notification-heading"></span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info mr-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.notification.home.refreshListLabel')"></span>
        </button>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && notifications && notifications.length === 0">
      <span v-text="t$('ventanillaUnicaApp.notification.home.notFound')"></span>
    </div>
    <div class="table-responsive" v-if="notifications && notifications.length > 0">
      <table class="table table-striped" aria-describedby="notifications">
        <thead>
          <tr>
            <th scope="row" @click="changeOrder('id')">
              <span v-text="t$('global.field.id')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('tipo')">
              <span v-text="t$('ventanillaUnicaApp.notification.tipo')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'tipo'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('fecha')">
              <span v-text="t$('ventanillaUnicaApp.notification.fecha')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'fecha'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('mensaje')">
              <span v-text="t$('ventanillaUnicaApp.notification.mensaje')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'mensaje'"></jhi-sort-indicator>
            </th>
            <th scope="row" @click="changeOrder('leido')">
              <span v-text="t$('ventanillaUnicaApp.notification.leido')"></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'leido'"></jhi-sort-indicator>
            </th>
            <th scope="row"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="notification in notifications" :key="notification.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'NotificacionView', params: { notificacionId: notification.id } }">{{
                notification.id
              }}</router-link>
            </td>
            <td>{{ notification.tipo }}</td>
            <td>{{ formatDateShort(notification.fecha) || '' }}</td>
            <td>{{ notification.mensaje }}</td>
            <td>{{ notification.leido }}</td>
            <td class="text-right">
              <div class="btn-group">
                <router-link :to="{ name: 'NotificacionView', params: { notificacionId: notification.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline" v-text="t$('entity.action.view')"></span>
                  </button>
                </router-link>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-show="notifications && notifications.length > 0">
      <div class="row justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./notification.component.ts"></script>
