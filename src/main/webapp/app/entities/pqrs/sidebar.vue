<template>
  <div class="sidebar card shadow-sm">
    <div class="card-body">
      <h5 class="card-title font-weight-bold text-dark mb-4" v-text="t$('ventanillaUnicaApp.pqrs.action.title')"></h5>

      <div class="d-flex flex-column gap-3">
        <b-button
          v-if="pqrs && pqrs.id && (isFunctionary || isFrontdesk) && pqrs.estado !== PqrsStatus.Closed"
          @click="$emit('toggle-status')"
          :class="['w-100 font-weight-bold br-lg', pqrs.estado === PqrsStatus.Resolved ? 'btn-warning' : 'btn-success']"
          data-cy="sidebarToggleStatusButton"
        >
          <font-awesome-icon :icon="pqrs.estado === PqrsStatus.Resolved ? 'undo-alt' : 'check-circle'"></font-awesome-icon>
          <span v-if="pqrs.estado === PqrsStatus.Resolved" v-text="t$('ventanillaUnicaApp.pqrs.action.inProgres')"></span>
          <span v-else v-text="t$('ventanillaUnicaApp.pqrs.action.resolve')"></span>
        </b-button>

        <b-button
          v-if="pqrs && pqrs.id && pqrs.estado !== PqrsStatus.Closed && isAdmin"
          @click="$emit('open-close-modal')"
          class="font-weight-bold w-100 br-lg"
          variant="danger"
          data-cy="sidebarClosePqrsButton"
        >
          <font-awesome-icon icon="times-circle"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.pqrs.action.closePqrs')"></span>
        </b-button>

        <b-button
          v-if="pqrs && pqrs.id && (isFunctionary || isAdmin || isFrontdesk) && pqrs.estado !== PqrsStatus.Closed"
          @click="navigateToCreateResponse"
          variant="info"
          class="font-weight-bold w-100 br-lg"
          data-cy="sidebarCreateResponseButton"
        >
          <font-awesome-icon icon="plus-circle"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.pqrs.action.createResponse')"></span>
        </b-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./sidebar.ts"></script>
<style scoped>
.card {
  border-radius: 1rem;
}
</style>
