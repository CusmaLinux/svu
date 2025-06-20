<template>
  <div class="sidebar card shadow-sm">
    <div class="card-body">
      <h5 class="card-title mb-3" v-text="t$('ventanillaUnicaApp.pqrs.action.title')"></h5>

      <div class="d-grid gap-1">
        <button
          v-if="pqrs && pqrs.id && isFunctionary && pqrs.estado !== PqrsStatus.Closed"
          @click="$emit('toggle-status')"
          :class="['btn', pqrs.estado === PqrsStatus.Resolved ? 'btn-warning' : 'btn-success']"
          data-cy="sidebarToggleStatusButton"
        >
          <font-awesome-icon :icon="pqrs.estado === PqrsStatus.Resolved ? 'undo-alt' : 'check-circle'"></font-awesome-icon>
          <span v-if="pqrs.estado === PqrsStatus.Resolved" v-text="t$('ventanillaUnicaApp.pqrs.action.inProgres')"></span>
          <span v-else v-text="t$('ventanillaUnicaApp.pqrs.action.resolve')"></span>
        </button>

        <button
          v-if="pqrs && pqrs.id && pqrs.estado !== PqrsStatus.Closed && isAdmin"
          @click="$emit('open-close-modal')"
          class="btn btn-danger"
          data-cy="sidebarClosePqrsButton"
        >
          <font-awesome-icon icon="times-circle"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.pqrs.action.closePqrs')"></span>
        </button>

        <button
          v-if="pqrs && pqrs.id && (isFunctionary || isAdmin || isFrontdesk) && pqrs.estado !== PqrsStatus.Closed"
          @click="navigateToCreateResponse"
          class="btn btn-info"
          data-cy="sidebarCreateResponseButton"
        >
          <font-awesome-icon icon="plus-circle"></font-awesome-icon>
          <span v-text="t$('ventanillaUnicaApp.pqrs.action.createResponse')"></span>
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./sidebar.ts"></script>
