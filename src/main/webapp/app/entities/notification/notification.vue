<template>
  <b-container>
    <div class="d-flex flex-column flex-md-row justify-content-md-between align-items-md-center mb-4">
      <h2 id="page-heading" data-cy="NotificationHeading" class="mb-3 mb-md-0">
        <span v-text="t$('ventanillaUnicaApp.notification.home.title')" id="notification-heading"></span>
      </h2>

      <div class="d-flex justify-content-end">
        <b-button variant="outline-secondary" @click="handleSyncList" :disabled="isFetching" v-b-tooltip.hover title="Refrescar lista">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon>
          <span class="d-none d-lg-inline ml-1" v-text="t$('ventanillaUnicaApp.notification.home.refreshListLabel')"></span>
        </b-button>

        <b-dropdown variant="primary" right class="ml-2" :disabled="!isAnySelected || isSaving || isDeleting">
          <template #button-content>
            <font-awesome-icon icon="list-ul"></font-awesome-icon>
            <span class="d-none d-lg-inline ml-1">Acciones</span>
          </template>

          <b-dropdown-item @click="saveChanges" :disabled="!isAnySelected || isSaving">
            <font-awesome-icon icon="check-double" class="mr-2 text-success" />
            <span>Marcar como revisadas</span>
          </b-dropdown-item>

          <b-dropdown-item @click="prepareRemoveSelected" :disabled="!isAnySelected || isDeleting">
            <font-awesome-icon icon="trash-alt" class="mr-2 text-danger" />
            <span>Eliminar seleccionados</span>
          </b-dropdown-item>
        </b-dropdown>
      </div>
    </div>

    <br />
    <div class="alert alert-warning" v-if="!isFetching && notifications && notifications.length === 0">
      <span v-text="t$('ventanillaUnicaApp.notification.home.notFound')"></span>
    </div>
    <div class="table-responsive" v-if="notifications && notifications.length > 0">
      <table class="table table-striped" aria-describedby="notifications">
        <thead class="thead-light">
          <tr>
            <th scope="row" style="width: 50px">
              <input type="checkbox" v-model="isAllSelected" title="Seleccionar todos en esta página" />
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
            <th scope="row" class="text-center" @click="changeOrder('leido')" style="width: 70px">
              <span><font-awesome-icon icon="check-double"></font-awesome-icon></span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'leido'"></jhi-sort-indicator>
            </th>
            <th scope="row" class="text-center"><span> Acciones </span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="notification in notifications" :key="notification.id" data-cy="entityTable">
            <td>
              <input type="checkbox" :value="notification.id" v-model="selectedNotificationIds" />
            </td>
            <td>{{ t$(getEnumValueByKey(NotificationType, notification.tipo, t$('sse-notification.non-type'))) }}</td>
            <td>{{ formatDateShort(notification.fecha) || '' }}</td>
            <td>{{ notification.mensaje }}</td>
            <td class="text-center align-middle">
              <span v-if="notification.leido">
                <font-awesome-icon icon="check-circle" class="text-success" style="font-size: 1.2rem" />
              </span>
              <span v-else>
                <font-awesome-icon icon="circle-notch" class="text-muted" style="font-size: 1.2rem" />
              </span>
            </td>
            <td class="text-center">
              <div class="btn-group">
                <button @click="handleViewClick(notification)" class="btn btn-primary btn-sm" data-cy="entityDetailsButton">
                  <font-awesome-icon icon="eye"></font-awesome-icon>
                  <span class="d-none d-md-inline"> Vista</span>
                </button>
                <button @click="prepareRemove(notification)" class="btn btn-danger btn-sm" data-cy="entityDeleteButton">
                  <font-awesome-icon icon="trash-alt"></font-awesome-icon>
                  <!-- Changed from 'times' to 'trash-alt' for consistency -->
                  <span class="d-none d-md-inline" v-text="t$('entity.action.delete')"></span>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Delete Confirmation Modal -->
    <b-modal ref="removeNotificationModalRef" id="removeNotificationModal">
      <template #modal-title>
        <span
          id="ventanillaUnicaApp.notification.delete.question"
          data-cy="notificationDeleteDialogHeading"
          v-text="t$('entity.delete.title')"
        ></span>
      </template>
      <div class="modal-body">
        <p v-if="bulkDelete" id="jhi-delete-notification-heading">
          ¿Está seguro de que desea eliminar las {{ selectedNotificationIds.length }} notificaciones seleccionadas?
        </p>
        <p v-else id="jhi-delete-notification-heading">¿Está seguro de que desea eliminar esta notificación?</p>
      </div>
      <template #modal-footer>
        <div>
          <button type="button" class="btn btn-secondary" v-text="t$('entity.action.cancel')" @click="closeDialog()"></button>
          <button
            type="button"
            class="btn btn-danger"
            id="jhi-confirm-delete-notification"
            data-cy="entityConfirmDeleteButton"
            :disabled="isDeleting"
            @click="deleteNotifications()"
          >
            Eliminar
            <font-awesome-icon icon="trash-alt" v-if="!isDeleting" />
            <b-spinner small v-if="isDeleting"></b-spinner>
          </button>
        </div>
      </template>
    </b-modal>

    <div v-show="notifications && notifications.length > 0">
      <div class="row justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </b-container>
</template>

<script lang="ts" src="./notification.component.ts"></script>

<style scoped>
.thead-light th {
  background-color: #f8f9fa;
  border-color: #dee2e6;
}
.align-middle {
  vertical-align: middle !important;
}
</style>
